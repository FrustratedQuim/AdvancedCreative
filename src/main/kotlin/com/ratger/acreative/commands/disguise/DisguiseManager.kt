package com.ratger.acreative.commands.disguise

import com.github.retrooper.packetevents.util.Vector3d
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PacketItemConversionSupport
import com.ratger.acreative.utils.PlayerDisplayNameResolver
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import com.ratger.acreative.utils.ViewerTeamPacketSupport
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import me.tofaa.entitylib.meta.other.FireworkRocketMeta
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.meta.projectile.ItemEntityMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permissible
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.player.Equipment as PacketEquipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot as PacketEquipmentSlot

data class DisguiseData(
    val entity: WrapperEntity,
    val type: EntityType,
    val identityKey: String,
    val showSelf: Boolean,
    val showNick: Boolean,
    val capabilities: DisguiseCapabilities,
    val nameMode: DisguiseEntityFactory.NameMode,
    val viewerTeam: ViewerTeamPacketSupport.Definition? = null,
    val renderProfile: DisguiseRenderProfile = DisguiseRenderProfile(),
    var equipment: List<PacketEquipment> = emptyList()
)

class DisguiseManager(private val hooker: FunctionHooker) {
    private companion object {
        const val BLOCKED_DISGUISES_PATH = "blocked-disguises"
        const val UPDATE_PERIOD_TICKS = 2L
        const val ATTACK_ACTION_COOLDOWN_TICKS = 2L
        const val VIEWER_REFRESH_PERIOD_TICKS = 20L
        const val LOOPING_TNT_RESPAWN_PERIOD_TICKS = 60L
        const val SLIME_JUMP_LAND_TICKS = 2L
        const val MIN_PROJECTILE_SPAWN_VELOCITY = 0.001
        val ZERO_VELOCITY = Vector3d(0.0, 0.0, 0.0)
    }

    data class CacheSnapshot(
        val disguisedPlayers: Int,
        val viewerRelations: Int,
        val queuedViewerRelations: Int,
        val pendingViewers: Int,
        val rememberedNames: Int,
        val rememberedGlowStates: Int
    )

    val disguisedPlayers = ConcurrentHashMap<Player, DisguiseData>()
    private val tasks = ConcurrentHashMap<Player, Int>()
    private val activeViewers = ConcurrentHashMap<Player, MutableSet<UUID>>()
    private val queuedInitViewers = ConcurrentHashMap<Player, MutableSet<UUID>>()
    private val viewerPendingUntilTick = ConcurrentHashMap<UUID, Long>()
    private val lastCustomName = ConcurrentHashMap<Player, Component>()
    private val lastSharedFlags = ConcurrentHashMap<Player, DisguisePacketDispatcher.SharedFlagsState>()
    private val lastVelocityState = ConcurrentHashMap<Player, Vector3d>()
    private val lastLocationState = ConcurrentHashMap<Player, DisguisePacketDispatcher.LocationSnapshot>()
    private val lastPrimaryItemState = ConcurrentHashMap<Player, PacketItemStack>()
    private val lastMirroredBlockStateId = ConcurrentHashMap<Player, Int>()
    private val lastAttackAnimationTick = ConcurrentHashMap<Player, Long>()
    private val attackStateResetTasks = ConcurrentHashMap<Player, Int>()
    private val entityFactory = DisguiseEntityFactory()
    private val packetDispatcher = DisguisePacketDispatcher()

    private val pendingInitDelayTicks = 10L

    val donationRestrictedEntities = setOf(
        EntityType.WITHER,
        EntityType.ENDER_DRAGON,
        EntityType.GIANT,
        EntityType.WARDEN
    )

    val blockedDisguiseEntities: Set<EntityType>
        get() = hooker.configManager.config.getStringList(BLOCKED_DISGUISES_PATH)
            .mapNotNull(::getEntityType)
            .toSet()

    private fun getNearbyPlayers(player: Player, location: Location, showSelf: Boolean): List<Player> {
        return location.getNearbyPlayers(100.0)
            .filter { !hooker.utils.isHiddenFromPlayer(it, player) && (showSelf || it != player) }
    }

    private fun getEntityType(type: String): EntityType? {
        return runCatching { EntityType.valueOf(type.uppercase()) }.getOrNull()
    }

    fun isBlockedDisguiseType(type: EntityType): Boolean {
        return type in blockedDisguiseEntities
    }

    fun canUseTextDisguise(permissible: Permissible): Boolean {
        return permissible.hasPermission(DisguisePermissions.TEXT)
    }

    private fun getCurrentTick(): Long = Bukkit.getCurrentTick().toLong()

    private fun isViewerPending(viewerId: UUID): Boolean {
        pruneExpiredViewerPending()
        val untilTick = viewerPendingUntilTick[viewerId] ?: return false
        return untilTick > getCurrentTick()
    }

    private fun markViewerPending(viewerId: UUID, ticks: Long = pendingInitDelayTicks) {
        viewerPendingUntilTick[viewerId] = getCurrentTick() + ticks
    }

    fun onViewerJoin(viewer: Player) {
        cleanupViewerFromAllDisguises(viewer.uniqueId)
        markViewerPending(viewer.uniqueId)
    }

    fun onViewerDisconnect(viewerId: UUID) {
        viewerPendingUntilTick.remove(viewerId)
        cleanupViewerFromAllDisguises(viewerId)
    }

    fun onViewerWorldOrRespawn(viewer: Player) {
        cleanupViewerFromAllDisguises(viewer.uniqueId)
        markViewerPending(viewer.uniqueId)
    }

    private fun shouldViewerSeeDisguise(owner: Player, viewer: Player, showSelf: Boolean): Boolean {
        if (!viewer.isOnline || !owner.isOnline) return false
        if (owner.world != viewer.world) return false
        if (!showSelf && viewer.uniqueId == owner.uniqueId) return false
        if (hooker.utils.isHiddenFromPlayer(viewer, owner)) return false
        if (owner.location.distanceSquared(viewer.location) > 100.0 * 100.0) return false
        return true
    }

    private fun cleanupViewerForDisguise(owner: Player, data: DisguiseData, viewerId: UUID) {
        val wasActive = activeViewers[owner]?.remove(viewerId) == true
        queuedInitViewers[owner]?.remove(viewerId)
        if (wasActive) {
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                removeViewerTeam(viewer, data)
            }
        }
        data.entity.removeViewer(viewerId)
    }

    private fun cleanupViewerFromAllDisguises(viewerId: UUID) {
        disguisedPlayers.forEach { (owner, data) ->
            cleanupViewerForDisguise(owner, data, viewerId)
        }
    }

    private fun queueViewerInit(owner: Player, viewerId: UUID) {
        queuedInitViewers.computeIfAbsent(owner) { mutableSetOf() }.add(viewerId)
    }

    private fun initializeViewer(owner: Player, data: DisguiseData, viewer: Player): Boolean {
        if (!shouldViewerSeeDisguise(owner, viewer, data.showSelf)) return false
        if (isViewerPending(viewer.uniqueId)) return false

        hooker.actionLogger.info(
            "Initializing disguise viewer owner=${hooker.actionLogger.playerRef(owner)} viewer=${hooker.actionLogger.playerRef(viewer)} equipment=${data.equipment.size}"
        )

        sendViewerTeam(viewer, data)
        data.entity.addViewer(viewer.uniqueId)
        activeViewers.computeIfAbsent(owner) { mutableSetOf() }.add(viewer.uniqueId)
        queuedInitViewers[owner]?.remove(viewer.uniqueId)
        syncViewerPresentation(owner, data, listOf(viewer), scheduleMotionStabilization = true)
        return true
    }

    fun disguisePlayer(
        player: Player,
        type: String?,
        playerName: String?,
        flags: List<String>,
        textDisplayRaw: String? = null
    ) {
        if (type == null) {
            if (disguisedPlayers.containsKey(player)) {
                undisguisePlayer(player)
                return
            }
            hooker.messageManager.sendChat(player, MessageKey.USAGE_DISGUISE)
            return
        }

        if (type.equals("off", ignoreCase = true)) {
            undisguisePlayer(player)
            return
        }

        val parsedFlags = DisguiseFlags.parse(flags)
        if (parsedFlags.requiresNickPermission && !player.hasPermission(DisguisePermissions.NICK)) {
            hooker.permissionManager.sendPermissionDenied(player, "disguise.nick")
            return
        }

        if (type.equals("player", ignoreCase = true)) {
            if (!player.hasPermission(DisguisePermissions.PLAYER)) {
                hooker.permissionManager.sendPermissionDenied(player, "disguise.player")
                return
            }

            val targetName = playerName?.takeIf { it.isNotBlank() } ?: run {
                hooker.messageManager.sendChat(player, MessageKey.USAGE_DISGUISE)
                return
            }
            val target = findOnlinePlayer(targetName) ?: run {
                hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
                return
            }

            if (switchOrToggleCurrentDisguise(player, EntityType.PLAYER, "player:${target.uniqueId}", parsedFlags)) {
                return
            }

            startDisguise(
                player = player,
                identityKey = "player:${target.uniqueId}",
                showSelf = parsedFlags.showSelf,
                showNick = parsedFlags.showNick,
                buildDisguise = { preferredMainHand -> entityFactory.createPlayerDisguise(player, target, preferredMainHand) }
            )
            return
        }

        val entityType = getEntityType(type)
        if (entityType == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        if (isBlockedDisguiseType(entityType)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_BLOCKED)
            return
        }

        if (entityType == EntityType.TEXT_DISPLAY && !canUseTextDisguise(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        if (!player.hasPermission(DisguisePermissions.EXTENDED) && entityType in donationRestrictedEntities) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        val identityKey = resolveMobIdentityKey(entityType, textDisplayRaw)
        if (switchOrToggleCurrentDisguise(player, entityType, identityKey, parsedFlags)) {
            return
        }

        startDisguise(
            player = player,
            identityKey = identityKey,
            showSelf = parsedFlags.showSelf,
            showNick = parsedFlags.showNick,
            buildDisguise = { preferredMainHand ->
                val textDisplayComponent = if (entityType == EntityType.TEXT_DISPLAY) {
                    hooker.messageManager.renderMiniMessage(textDisplayRaw.orEmpty())
                } else {
                    null
                }
                entityFactory.createMobDisguise(
                    owner = player,
                    type = entityType,
                    preferredMainHand = preferredMainHand,
                    textDisplayText = textDisplayComponent
                )
            }
        )
    }

    fun cacheSnapshot(): CacheSnapshot = CacheSnapshot(
        disguisedPlayers = disguisedPlayers.size,
        viewerRelations = activeViewers.values.sumOf { it.size },
        queuedViewerRelations = queuedInitViewers.values.sumOf { it.size },
        pendingViewers = activePendingViewerCount(),
        rememberedNames = lastCustomName.size,
        rememberedGlowStates = lastSharedFlags.size
    )

    private fun activePendingViewerCount(): Int {
        pruneExpiredViewerPending()
        return viewerPendingUntilTick.size
    }

    private fun pruneExpiredViewerPending(currentTick: Long = getCurrentTick()) {
        viewerPendingUntilTick.entries.removeIf { (_, untilTick) -> untilTick <= currentTick }
    }

    fun undisguisePlayer(player: Player, silent: Boolean = false) {
        disguisedPlayers[player]?.let { data ->
            hooker.actionLogger.info(
                "Disguise stop for ${hooker.actionLogger.playerRef(player)} viewers=${activeViewers[player]?.size ?: 0}"
            )

            activeViewers[player].orEmpty().forEach { viewerId ->
                Bukkit.getPlayer(viewerId)?.let { viewer ->
                    removeViewerTeam(viewer, data)
                }
            }

            data.entity.remove()
            player.isInvisible = false
            if (hooker.utils.isGlowing(player)) player.isGlowing = true
            hooker.playerStateManager.restorePlayerInventory(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.DISGUISED)
            disguisedPlayers.remove(player)
            tasks.remove(player)?.let { hooker.tickScheduler.cancel(it) }
            activeViewers.remove(player)
            queuedInitViewers.remove(player)
            lastCustomName.remove(player)
            lastSharedFlags.remove(player)
            lastVelocityState.remove(player)
            lastLocationState.remove(player)
            lastPrimaryItemState.remove(player)
            lastMirroredBlockStateId.remove(player)
            lastAttackAnimationTick.remove(player)
            attackStateResetTasks.remove(player)?.let(hooker.tickScheduler::cancel)
            if (!silent) {
                hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE_REMOVED)
            }
        }
    }

    fun updateDisguiseForPlayer(disguisedPlayer: Player, viewer: Player) {
        val data = disguisedPlayers[disguisedPlayer] ?: return
        if (!shouldViewerSeeDisguise(disguisedPlayer, viewer, data.showSelf)) {
            cleanupViewerForDisguise(disguisedPlayer, data, viewer.uniqueId)
            return
        }
        queueViewerInit(disguisedPlayer, viewer.uniqueId)
    }

    fun updateEntityGlowing(player: Player, isGlowing: Boolean) {
        val data = disguisedPlayers[player] ?: return
        val desiredFlags = currentSharedFlags(player, data.capabilities, glowingOverride = isGlowing)
        if (lastSharedFlags[player] == desiredFlags) return

        lastSharedFlags[player] = desiredFlags
        packetDispatcher.sendSharedFlags(resolveActiveViewers(player), data.entity.entityId, desiredFlags)
    }

    fun sendAttackAnimation(player: Player) {
        val data = disguisedPlayers[player] ?: return
        val currentTick = getCurrentTick()
        val lastTick = lastAttackAnimationTick[player]
        if (lastTick != null && currentTick - lastTick < ATTACK_ACTION_COOLDOWN_TICKS) return
        lastAttackAnimationTick[player] = currentTick

        if (data.type == EntityType.EVOKER_FANGS) {
            respawnEntityForViewers(player, data, resolveActiveViewers(player), scheduleMotionStabilization = false)
        }

        if (data.type == EntityType.SLIME || data.type == EntityType.MAGMA_CUBE) {
            triggerSlimeJumpAnimation(player, data)
            return
        }

        packetDispatcher.playAttackAnimation(data.entity, data.capabilities.attackAnimation)
        applyTemporaryAttackState(player, data)
    }

    fun recreateDisguise(player: Player, to: Location?) {
        val data = disguisedPlayers[player] ?: return
        val targetLoc = to ?: player.location

        refreshViewerRelations(player, data, targetLoc)
        lastLocationState.remove(player)
        syncMovement(player, data, targetLoc, force = true)
        initializeQueuedViewers(player, data)
        if (data.capabilities.supportsEquipment) {
            packetDispatcher.sendEquipment(resolveActiveViewers(player), data.entity.entityId, data.equipment)
        }
    }

    fun updateMainHandEquipment(player: Player) {
        val data = disguisedPlayers[player] ?: return
        syncEquipment(player, data)
        syncDerivedMetadata(player, data)
    }

    private fun startDisguise(
        player: Player,
        identityKey: String,
        showSelf: Boolean,
        showNick: Boolean,
        buildDisguise: (preferredMainHand: ItemStack?) -> DisguiseEntityFactory.CreatedDisguise?
    ) {
        hooker.playerStateManager.activateState(player, PlayerStateType.DISGUISED)
        hooker.utils.unsetAllStates(player)
        hooker.playerStateManager.savePlayerInventory(player)

        val preferredMainHand = hiddenMainHandStack(player)
        val created = buildDisguise(preferredMainHand) ?: run {
            hooker.playerStateManager.restorePlayerInventory(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.DISGUISED)
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_BLOCKED)
            return
        }

        created.entity.entityMeta.setNotifyAboutChanges(false)

        val equipment = buildHiddenEquipment(player, created.capabilities)
        clearVisibleInventory(player)
        seedStateCaches(player, created, showNick, preferredMainHand)
        seedVelocityState(player, created)

        val data = DisguiseData(
            entity = created.entity,
            type = created.type,
            identityKey = identityKey,
            showSelf = showSelf,
            showNick = showNick,
            capabilities = created.capabilities,
            nameMode = created.nameMode,
            viewerTeam = created.viewerTeam,
            renderProfile = created.renderProfile,
            equipment = equipment
        )
        disguisedPlayers[player] = data

        hooker.actionLogger.info(
            "Disguise start for ${hooker.actionLogger.playerRef(player)} type=${created.type.name} showSelf=$showSelf showNick=$showNick equipment=${equipment.size}"
        )

        created.entity.spawn(lastLocationState[player]!!.toPacketLocation())

        player.isInvisible = true
        player.isGlowing = false
        activeViewers[player] = mutableSetOf()
        queuedInitViewers[player] = mutableSetOf()
        getNearbyPlayers(player, player.location, data.showSelf).forEach { queueViewerInit(player, it.uniqueId) }

        scheduleUpdateTask(player)
        hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE)
    }

    private fun switchOrToggleCurrentDisguise(
        player: Player,
        type: EntityType,
        identityKey: String,
        flags: DisguiseFlags
    ): Boolean {
        val current = disguisedPlayers[player] ?: return false
        if (current.type == type && current.identityKey == identityKey &&
            current.showSelf == flags.showSelf && current.showNick == flags.showNick) {
            undisguisePlayer(player)
            return true
        }

        undisguisePlayer(player, true)
        return false
    }

    private fun resolveMobIdentityKey(entityType: EntityType, textDisplayRaw: String?): String {
        return if (entityType == EntityType.TEXT_DISPLAY) {
            "${entityType.name}:${textDisplayRaw.orEmpty()}"
        } else {
            entityType.name
        }
    }

    private fun seedStateCaches(
        player: Player,
        created: DisguiseEntityFactory.CreatedDisguise,
        showNick: Boolean,
        preferredMainHand: ItemStack?
    ) {
        val customName = resolveVisibleName(player, created.nameMode, showNick)
        if (customName == null) {
            lastCustomName.remove(player)
        } else {
            lastCustomName[player] = customName
        }

        lastSharedFlags[player] = currentSharedFlags(player, created.capabilities)
        lastLocationState[player] = snapshotFor(player, created.renderProfile, player.location)

        if (created.capabilities.mirrorsMainHandIntoItemMetadata) {
            lastPrimaryItemState[player] = entityFactory.resolveMirroredItem(created.type, preferredMainHand)
        } else {
            lastPrimaryItemState.remove(player)
        }

        if (created.renderProfile.mirrorsMainHandIntoBlockState) {
            lastMirroredBlockStateId[player] = entityFactory.resolveBlockDisplayStateId(preferredMainHand)
        } else {
            lastMirroredBlockStateId.remove(player)
        }
    }

    private fun seedVelocityState(player: Player, created: DisguiseEntityFactory.CreatedDisguise) {
        if (!created.capabilities.requiresSpawnVelocity && !created.capabilities.tracksVelocityContinuously) {
            lastVelocityState.remove(player)
            return
        }

        val velocity = if (created.capabilities.tracksVelocityContinuously) {
            player.velocity.toPacketVector()
        } else {
            minimalProjectileSpawnVelocity(player)
        }
        packetDispatcher.applyVelocity(created.entity, velocity)

        if (created.capabilities.tracksVelocityContinuously) {
            lastVelocityState[player] = velocity
        } else {
            lastVelocityState.remove(player)
        }
    }

    private fun scheduleUpdateTask(player: Player) {
        var runCounter = 0
        val viewerRefreshEvery = (VIEWER_REFRESH_PERIOD_TICKS / UPDATE_PERIOD_TICKS).toInt().coerceAtLeast(1)
        val loopingTntRefreshEvery = (LOOPING_TNT_RESPAWN_PERIOD_TICKS / UPDATE_PERIOD_TICKS).toInt().coerceAtLeast(1)

        val taskId = hooker.tickScheduler.runRepeating(0L, UPDATE_PERIOD_TICKS) {
            if (!player.isOnline || !disguisedPlayers.containsKey(player)) {
                tasks.remove(player)?.let { hooker.tickScheduler.cancel(it) }
                return@runRepeating
            }

            val currentData = disguisedPlayers[player] ?: return@runRepeating
            val playerLoc = player.location

            syncName(player, currentData)
            syncSharedFlags(player, currentData)
            syncVelocity(player, currentData)
            syncEquipment(player, currentData)
            syncDerivedMetadata(player, currentData)
            syncMovement(player, currentData, playerLoc)
            syncContinuousAttackState(player, currentData)
            if (runCounter % loopingTntRefreshEvery == 0) {
                maintainLoopingTnt(player, currentData)
            }

            if (runCounter % viewerRefreshEvery == 0) {
                refreshViewerRelations(player, currentData, playerLoc)
            }
            initializeQueuedViewers(player, currentData)

            runCounter += 1
        }
        tasks[player] = taskId
    }

    private fun refreshViewerRelations(player: Player, data: DisguiseData, location: Location) {
        val desiredViewers = getNearbyPlayers(player, location, data.showSelf)
        val desiredIds = desiredViewers.map { it.uniqueId }.toSet()
        val active = activeViewers.computeIfAbsent(player) { mutableSetOf() }
        val queued = queuedInitViewers.computeIfAbsent(player) { mutableSetOf() }

        val toRemove = (active + queued).filter { it !in desiredIds }
        toRemove.forEach { cleanupViewerForDisguise(player, data, it) }

        desiredViewers.forEach { viewer ->
            if (viewer.uniqueId !in active && viewer.uniqueId !in queued) {
                queueViewerInit(player, viewer.uniqueId)
            }
        }
    }

    private fun initializeQueuedViewers(player: Player, data: DisguiseData) {
        val queued = queuedInitViewers.computeIfAbsent(player) { mutableSetOf() }
        queued.toSet().forEach { viewerId ->
            val viewer = Bukkit.getPlayer(viewerId) ?: run {
                cleanupViewerForDisguise(player, data, viewerId)
                return@forEach
            }
            if (!shouldViewerSeeDisguise(player, viewer, data.showSelf)) {
                cleanupViewerForDisguise(player, data, viewerId)
                return@forEach
            }
            initializeViewer(player, data, viewer)
        }
    }

    private fun syncName(player: Player, data: DisguiseData) {
        val desiredName = resolveVisibleName(player, data.nameMode, data.showNick)
        if (lastCustomName[player] == desiredName) return

        if (desiredName == null) {
            lastCustomName.remove(player)
        } else {
            lastCustomName[player] = desiredName
        }
        packetDispatcher.sendCustomName(resolveActiveViewers(player), data.entity.entityId, desiredName)
    }

    private fun syncSharedFlags(player: Player, data: DisguiseData) {
        val desiredFlags = currentSharedFlags(player, data.capabilities)
        if (lastSharedFlags[player] == desiredFlags) return

        lastSharedFlags[player] = desiredFlags
        packetDispatcher.sendSharedFlags(resolveActiveViewers(player), data.entity.entityId, desiredFlags)
    }

    private fun syncVelocity(player: Player, data: DisguiseData) {
        if (!data.capabilities.tracksVelocityContinuously) return

        val desiredVelocity = player.velocity.toPacketVector()
        if (lastVelocityState[player]?.roughlyEquals(desiredVelocity) == true) return

        packetDispatcher.applyVelocity(data.entity, desiredVelocity)
        lastVelocityState[player] = desiredVelocity
    }

    private fun syncMovement(player: Player, data: DisguiseData, location: Location, force: Boolean = false) {
        val current = snapshotFor(player, data.renderProfile, location)
        val previous = if (force) null else lastLocationState[player]
        val viewers = resolveActiveViewers(player)
        val positionChanged = force || previous == null || previous.hasPositionChangedFrom(current)

        packetDispatcher.sendMovement(viewers, data.entity, previous, current)
        if (data.capabilities.supportsHeadRotation &&
            (force || previous == null || previous.hasHeadYawChangedFrom(current))) {
            packetDispatcher.sendHeadRotation(viewers, data.entity.entityId, current.yaw)
        }

        lastLocationState[player] = current

        val attackState = data.capabilities.attackState
        if (positionChanged &&
            attackState.reapplyAfterPositionSyncWhenActive &&
            attackState.isActive(data.entity)) {
            sendAttackStatePresentation(viewers, data.entity, attackState.presentation(data.entity, active = true))
        }
    }

    private fun syncViewerPresentation(
        owner: Player,
        data: DisguiseData,
        viewers: Collection<Player>,
        scheduleMotionStabilization: Boolean
    ) {
        if (viewers.isEmpty()) return

        val name = resolveVisibleName(owner, data.nameMode, data.showNick)
        packetDispatcher.sendCustomName(viewers, data.entity.entityId, name)
        packetDispatcher.sendSharedFlags(viewers, data.entity.entityId, currentSharedFlags(owner, data.capabilities))

        if (data.capabilities.supportsEquipment) {
            packetDispatcher.sendEquipment(viewers, data.entity.entityId, data.equipment)
        }
        if (data.capabilities.supportsHeadRotation) {
            packetDispatcher.sendHeadRotation(viewers, data.entity.entityId, currentLocationSnapshot(owner, data).yaw)
        }

        stabilizeViewerMotion(owner, data, viewers, scheduleFollowUp = scheduleMotionStabilization)
    }

    private fun stabilizeViewerMotion(
        owner: Player,
        data: DisguiseData,
        viewers: Collection<Player>,
        scheduleFollowUp: Boolean
    ) {
        val stabilization = data.renderProfile.motionStabilization
        if (!stabilization.requiresCorrection || viewers.isEmpty()) return

        val currentLocation = currentLocationSnapshot(owner, data)
        sendMotionCorrection(viewers, data.entity.entityId, currentLocation, stabilization)

        if (!scheduleFollowUp || stabilization.followUpTicks <= 0L) return

        val ownerId = owner.uniqueId
        val entityId = data.entity.entityId
        val viewerIds = viewers.map(Player::getUniqueId)

        hooker.tickScheduler.runLater(stabilization.followUpTicks) {
            val currentOwner = Bukkit.getPlayer(ownerId) ?: return@runLater
            val currentData = disguisedPlayers[currentOwner] ?: return@runLater
            if (currentData.entity.entityId != entityId) return@runLater

            val currentViewers = viewerIds
                .mapNotNull(Bukkit::getPlayer)
                .filter { currentData.entity.hasViewer(it.uniqueId) }
            if (currentViewers.isEmpty()) return@runLater

            sendMotionCorrection(
                currentViewers,
                entityId,
                currentLocationSnapshot(currentOwner, currentData),
                currentData.renderProfile.motionStabilization
            )
        }
    }

    private fun sendMotionCorrection(
        viewers: Collection<Player>,
        entityId: Int,
        location: DisguisePacketDispatcher.LocationSnapshot,
        stabilization: DisguiseMotionStabilization
    ) {
        if (stabilization.sendsZeroVelocity) {
            packetDispatcher.sendVelocity(viewers, entityId, ZERO_VELOCITY)
        }
        if (stabilization.sendsZeroProjectilePower) {
            packetDispatcher.sendProjectilePower(viewers, entityId, 0.0)
        }
        packetDispatcher.sendTeleport(viewers, entityId, location)
    }

    private fun maintainLoopingTnt(player: Player, data: DisguiseData) {
        if (!data.renderProfile.keepsPrimedTntLooping) return

        val viewers = resolveActiveViewers(player)
        if (viewers.isEmpty()) return

        respawnEntityForViewers(player, data, viewers, scheduleMotionStabilization = false)
    }

    private fun syncContinuousAttackState(player: Player, data: DisguiseData) {
        val attackState = data.capabilities.attackState
        if (!attackState.reappliesContinuouslyWhenActive) return
        if (!attackState.isActive(data.entity)) return

        sendAttackStatePresentation(
            resolveActiveViewers(player),
            data.entity,
            attackState.presentation(data.entity, active = true)
        )
    }

    private fun applyTemporaryAttackState(player: Player, data: DisguiseData) {
        val attackState = data.capabilities.attackState
        if (attackState == DisguiseAttackState.None) return

        attackStateResetTasks.remove(player)?.let(hooker.tickScheduler::cancel)

        val context = DisguiseAttackContext(targetEntityId = null)
        if (attackState.isToggle) {
            val activate = !attackState.isActive(data.entity)
            val changed = if (activate) {
                attackState.apply(data.entity, context)
            } else {
                attackState.clear(data.entity)
            }
            if (!changed) return

            sendAttackStatePresentation(
                resolveActiveViewers(player),
                data.entity,
                attackState.presentation(data.entity, active = activate)
            )
            return
        }

        val wasAlreadyActive = attackState.isActive(data.entity)
        val changed = attackState.apply(data.entity, context)
        if (!changed && !wasAlreadyActive) {
            return
        }

        sendAttackStatePresentation(
            resolveActiveViewers(player),
            data.entity,
            attackState.presentation(data.entity, active = true)
        )

        val ownerId = player.uniqueId
        val entityId = data.entity.entityId
        val resetTaskId = hooker.tickScheduler.runLater(attackState.durationTicks) {
            val currentOwner = Bukkit.getPlayer(ownerId) ?: return@runLater
            val currentData = disguisedPlayers[currentOwner] ?: return@runLater
            if (currentData.entity.entityId != entityId) return@runLater

            attackStateResetTasks.remove(currentOwner)
            if (attackState.clear(currentData.entity)) {
                sendAttackStatePresentation(
                    resolveActiveViewers(currentOwner),
                    currentData.entity,
                    attackState.presentation(currentData.entity, active = false)
                )
            }
        }
        attackStateResetTasks[player] = resetTaskId
    }

    private fun sendAttackStatePresentation(
        viewers: Collection<Player>,
        entity: WrapperEntity,
        presentation: DisguiseAttackStatePresentation?
    ) {
        when (presentation) {
            null -> return
            is DisguiseAttackStatePresentation.EntityStatus -> {
                packetDispatcher.sendEntityStatus(viewers, entity.entityId, presentation.status)
            }
            is DisguiseAttackStatePresentation.Metadata -> {
                packetDispatcher.sendMetadata(viewers, entity.entityId, presentation.entries)
            }
        }
    }

    private fun currentLocationSnapshot(
        owner: Player,
        data: DisguiseData
    ): DisguisePacketDispatcher.LocationSnapshot {
        return lastLocationState[owner] ?: snapshotFor(owner, data.renderProfile, owner.location)
    }

    private fun syncEquipment(player: Player, data: DisguiseData) {
        if (!data.capabilities.supportsEquipment) return

        val newEquipment = buildHiddenEquipment(player, data.capabilities)
        if (data.equipment == newEquipment) return

        data.equipment = newEquipment
        hooker.actionLogger.info(
            "Updating disguise equipment for ${hooker.actionLogger.playerRef(player)} equipment=${newEquipment.size}"
        )
        packetDispatcher.sendEquipment(resolveActiveViewers(player), data.entity.entityId, newEquipment)
    }

    private fun syncDerivedMetadata(player: Player, data: DisguiseData) {
        var requiresRefresh = false

        if (data.capabilities.mirrorsMainHandIntoItemMetadata) {
            val desiredItem = entityFactory.resolveMirroredItem(data.type, hiddenMainHandStack(player))
            if (lastPrimaryItemState[player] != desiredItem) {
                if (applyPrimaryItemMetadata(data.entity, desiredItem)) {
                    requiresRefresh = true
                }
                lastPrimaryItemState[player] = desiredItem
            }
        }

        if (data.renderProfile.mirrorsMainHandIntoBlockState) {
            val desiredStateId = entityFactory.resolveBlockDisplayStateId(hiddenMainHandStack(player))
            if (lastMirroredBlockStateId[player] != desiredStateId) {
                val changed = when {
                    data.renderProfile.respawnsOnMirroredBlockStateChange ->
                        applyFallingBlockMetadata(data.entity, desiredStateId)
                    else -> applyBlockDisplayMetadata(data.entity, desiredStateId)
                }
                if (changed) {
                    if (data.renderProfile.respawnsOnMirroredBlockStateChange) {
                        respawnEntityForViewers(
                            player,
                            data,
                            resolveActiveViewers(player),
                            scheduleMotionStabilization = true
                        )
                    } else {
                        requiresRefresh = true
                    }
                }
                lastMirroredBlockStateId[player] = desiredStateId
            }
        }

        if (requiresRefresh) {
            data.entity.refresh()
        }
    }

    private fun applyPrimaryItemMetadata(entity: WrapperEntity, item: PacketItemStack): Boolean {
        val meta = entity.entityMeta
        return when (meta) {
            is ItemDisplayMeta -> {
                if (meta.item == item) {
                    false
                } else {
                    meta.item = item
                    true
                }
            }

            is ItemFrameMeta -> {
                if (meta.item == item) {
                    false
                } else {
                    meta.item = item
                    true
                }
            }

            is ItemEntityMeta -> {
                if (meta.item == item) {
                    false
                } else {
                    meta.item = item
                    true
                }
            }

            is FireworkRocketMeta -> {
                if (meta.fireworkItem == item) {
                    false
                } else {
                    meta.fireworkItem = item
                    true
                }
            }

            else -> false
        }
    }

    private fun applyBlockDisplayMetadata(entity: WrapperEntity, blockStateId: Int): Boolean {
        val meta = entity.entityMeta as? BlockDisplayMeta ?: return false
        if (meta.blockId == blockStateId) return false

        meta.blockId = blockStateId
        return true
    }

    private fun applyFallingBlockMetadata(entity: WrapperEntity, blockStateId: Int): Boolean {
        val meta = entity.entityMeta as? me.tofaa.entitylib.meta.other.FallingBlockMeta ?: return false
        if (meta.blockStateId == blockStateId) return false

        meta.blockStateId = blockStateId
        return true
    }

    private fun respawnEntityForViewers(
        owner: Player,
        data: DisguiseData,
        viewers: Collection<Player>,
        scheduleMotionStabilization: Boolean
    ) {
        if (viewers.isEmpty()) return

        viewers.forEach { viewer ->
            data.entity.removeViewer(viewer.uniqueId)
            data.entity.addViewer(viewer.uniqueId)
        }
        syncViewerPresentation(owner, data, viewers, scheduleMotionStabilization)
    }

    private fun triggerSlimeJumpAnimation(player: Player, data: DisguiseData) {
        val viewers = resolveActiveViewers(player)
        if (viewers.isEmpty()) return

        val airborneSnapshot = currentLocationSnapshot(player, data).copy(onGround = false)
        packetDispatcher.sendTeleport(viewers, data.entity.entityId, airborneSnapshot)

        attackStateResetTasks.remove(player)?.let(hooker.tickScheduler::cancel)
        val ownerId = player.uniqueId
        val entityId = data.entity.entityId
        val resetTaskId = hooker.tickScheduler.runLater(SLIME_JUMP_LAND_TICKS) {
            val currentOwner = Bukkit.getPlayer(ownerId) ?: return@runLater
            val currentData = disguisedPlayers[currentOwner] ?: return@runLater
            if (currentData.entity.entityId != entityId) return@runLater

            attackStateResetTasks.remove(currentOwner)
            val currentViewers = resolveActiveViewers(currentOwner)
            if (currentViewers.isEmpty()) return@runLater

            packetDispatcher.sendTeleport(
                currentViewers,
                currentData.entity.entityId,
                currentLocationSnapshot(currentOwner, currentData)
            )
        }
        attackStateResetTasks[player] = resetTaskId
    }

    private fun currentSharedFlags(
        player: Player,
        capabilities: DisguiseCapabilities,
        glowingOverride: Boolean? = null
    ): DisguisePacketDispatcher.SharedFlagsState {
        return DisguisePacketDispatcher.SharedFlagsState(
            glowing = glowingOverride ?: hooker.utils.isGlowing(player),
            sneaking = capabilities.supportsSneakState && player.isSneaking
        )
    }

    private fun snapshotFor(
        player: Player,
        renderProfile: DisguiseRenderProfile,
        location: Location
    ): DisguisePacketDispatcher.LocationSnapshot {
        val adjustedLocation = applyLocationOffset(location, renderProfile.locationOffset)
        val rotatedLocation = applyYawOffset(adjustedLocation, renderProfile.yawOffset)
        return DisguisePacketDispatcher.LocationSnapshot.from(rotatedLocation, player.isOnGround)
    }

    private fun applyLocationOffset(location: Location, offset: DisguiseLocationOffset): Location {
        if (offset == DisguiseLocationOffset.NONE) return location
        return location.clone().add(offset.x, offset.y, offset.z)
    }

    private fun applyYawOffset(location: Location, yawOffset: Float): Location {
        if (yawOffset == 0f) return location
        return location.clone().apply {
            yaw = normalizeYaw(yaw + yawOffset)
        }
    }

    private fun normalizeYaw(yaw: Float): Float {
        var normalized = yaw % 360f
        if (normalized <= -180f) normalized += 360f
        if (normalized > 180f) normalized -= 360f
        return normalized
    }

    private fun minimalProjectileSpawnVelocity(player: Player): Vector3d {
        val direction = player.location.direction.normalize().multiply(MIN_PROJECTILE_SPAWN_VELOCITY)
        return direction.toPacketVector()
    }

    private fun buildHiddenEquipment(player: Player, capabilities: DisguiseCapabilities): List<PacketEquipment> {
        if (!capabilities.supportsEquipment) return emptyList()

        val equipment = mutableListOf<PacketEquipment>()
        if (capabilities.supportsMainHandEquipment) {
            equipment += PacketEquipment(PacketEquipmentSlot.MAIN_HAND, packetFromNullable(hiddenMainHandStack(player)))
        }
        if (capabilities.supportsOffHandEquipment) {
            equipment += PacketEquipment(PacketEquipmentSlot.OFF_HAND, packetFromNullable(hiddenOffHandStack(player)))
        }
        if (capabilities.supportsArmorEquipment) {
            hooker.playerStateManager.getSavedArmorContents(player).forEachIndexed { index, item ->
                val slot = when (index) {
                    0 -> PacketEquipmentSlot.BOOTS
                    1 -> PacketEquipmentSlot.LEGGINGS
                    2 -> PacketEquipmentSlot.CHEST_PLATE
                    3 -> PacketEquipmentSlot.HELMET
                    else -> return@forEachIndexed
                }
                equipment += PacketEquipment(slot, packetFromNullable(item))
            }
        }
        return equipment
    }

    private fun hiddenMainHandStack(player: Player): ItemStack? {
        return hooker.playerStateManager.getCurrentSavedMainHandItem(player)
            ?.takeUnless { it.type.isAir }
    }

    private fun hiddenOffHandStack(player: Player): ItemStack? {
        return hooker.playerStateManager.getSavedOffHandItem(player)
            ?.takeUnless { it.type.isAir }
    }

    private fun packetFromNullable(item: ItemStack?): PacketItemStack {
        return PacketItemConversionSupport.toPacket(item ?: ItemStack(Material.AIR))
    }

    private fun clearVisibleInventory(player: Player) {
        player.inventory.setItemInMainHand(null)
        player.inventory.setItemInOffHand(null)
        player.inventory.armorContents = arrayOfNulls(4)
    }

    private fun resolveVisibleName(
        owner: Player,
        nameMode: DisguiseEntityFactory.NameMode,
        visible: Boolean
    ): Component? {
        if (!visible) return null
        return when (nameMode) {
            is DisguiseEntityFactory.NameMode.Fixed -> nameMode.component
            DisguiseEntityFactory.NameMode.Owner -> PlayerDisplayNameResolver.resolve(owner)
        }
    }

    private fun sendViewerTeam(viewer: Player, data: DisguiseData) {
        data.viewerTeam?.let { ViewerTeamPacketSupport.sendCreate(viewer, it) }
    }

    private fun removeViewerTeam(viewer: Player, data: DisguiseData) {
        data.viewerTeam?.let { ViewerTeamPacketSupport.sendRemove(viewer, it.teamName) }
    }

    private fun resolveActiveViewers(owner: Player): List<Player> {
        return activeViewers[owner]
            .orEmpty()
            .mapNotNull(Bukkit::getPlayer)
    }

    private fun findOnlinePlayer(name: String): Player? {
        return Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    private fun org.bukkit.util.Vector.toPacketVector(): Vector3d {
        return Vector3d(x, y, z)
    }

    private fun Vector3d.roughlyEquals(other: Vector3d): Boolean {
        return abs(x - other.x) < 0.0001 && abs(y - other.y) < 0.0001 && abs(z - other.z) < 0.0001
    }

    private data class DisguiseFlags(
        val showSelf: Boolean,
        val showNick: Boolean,
        val requiresNickPermission: Boolean
    ) {
        companion object {
            fun parse(flags: List<String>): DisguiseFlags {
                var showSelf: Boolean? = null
                var showNick: Boolean? = null
                var requiresNickPermission = false

                flags.forEach { rawFlag ->
                    when (rawFlag.lowercase()) {
                        "-self" -> if (showSelf == null) showSelf = true
                        "-noself" -> if (showSelf == null) showSelf = false
                        "-withnick" -> {
                            if (showNick == null) showNick = true
                            requiresNickPermission = true
                        }
                        "-nonick" -> {
                            if (showNick == null) showNick = false
                            requiresNickPermission = true
                        }
                    }
                }

                return DisguiseFlags(
                    showSelf = showSelf ?: true,
                    showNick = showNick ?: true,
                    requiresNickPermission = requiresNickPermission
                )
            }
        }
    }
}
