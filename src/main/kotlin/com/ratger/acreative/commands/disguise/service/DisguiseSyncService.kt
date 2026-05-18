package com.ratger.acreative.commands.disguise.service

import com.github.retrooper.packetevents.util.Vector3d
import com.ratger.acreative.commands.disguise.DisguiseAttackStatePresentation
import com.ratger.acreative.commands.disguise.DisguiseCapabilities
import com.ratger.acreative.commands.disguise.DisguiseEntityFactory
import com.ratger.acreative.commands.disguise.DisguiseLocationOffset
import com.ratger.acreative.commands.disguise.DisguiseMotionStabilization
import com.ratger.acreative.commands.disguise.DisguisePacketDispatcher
import com.ratger.acreative.commands.disguise.DisguiseRenderProfile
import com.ratger.acreative.commands.disguise.model.DisguiseData
import com.ratger.acreative.commands.disguise.model.DisguiseRuntimeState
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.utils.PacketItemConversionSupport
import com.ratger.acreative.utils.PlayerDisplayNameResolver
import com.ratger.acreative.utils.ViewerTeamPacketSupport
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import me.tofaa.entitylib.meta.other.FireworkRocketMeta
import me.tofaa.entitylib.meta.other.FallingBlockMeta
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.meta.projectile.ItemEntityMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.math.abs
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.player.Equipment as PacketEquipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot as PacketEquipmentSlot

class DisguiseSyncService(
    private val hooker: FunctionHooker,
    private val state: DisguiseRuntimeState,
    private val entityFactory: DisguiseEntityFactory,
    private val packetDispatcher: DisguisePacketDispatcher
) {
    companion object {
        private const val PENDING_INIT_DELAY_TICKS = 10L
        private const val MIN_PROJECTILE_SPAWN_VELOCITY = 0.001
        val ZERO_VELOCITY = Vector3d(0.0, 0.0, 0.0)
    }

    fun activePendingViewerCount(): Int {
        pruneExpiredViewerPending()
        return state.viewerPendingUntilTick.size
    }

    fun onViewerJoin(viewer: Player) {
        cleanupViewerFromAllDisguises(viewer.uniqueId)
        markViewerPending(viewer.uniqueId)
    }

    fun onViewerDisconnect(viewerId: UUID) {
        state.viewerPendingUntilTick.remove(viewerId)
        cleanupViewerFromAllDisguises(viewerId)
    }

    fun onViewerWorldOrRespawn(viewer: Player) {
        cleanupViewerFromAllDisguises(viewer.uniqueId)
        markViewerPending(viewer.uniqueId)
    }

    fun beginDisguise(player: Player, data: DisguiseData) {
        state.activeViewers[player] = mutableSetOf()
        state.queuedInitViewers[player] = mutableSetOf()
        getNearbyPlayers(player, player.location, data.showSelf).forEach { queueViewerInit(player, it.uniqueId) }
    }

    fun stopTracking(player: Player, data: DisguiseData) {
        state.activeViewers[player].orEmpty().forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                removeViewerTeam(viewer, data)
            }
        }

        state.tasks.remove(player)?.let(hooker.tickScheduler::cancel)
        state.activeViewers.remove(player)
        state.queuedInitViewers.remove(player)
        state.lastCustomName.remove(player)
        state.lastSharedFlags.remove(player)
        state.lastVelocityState.remove(player)
        state.lastLocationState.remove(player)
        state.lastPrimaryItemState.remove(player)
        state.lastMirroredBlockStateId.remove(player)
    }

    fun seedStateCaches(
        player: Player,
        created: DisguiseEntityFactory.CreatedDisguise,
        showNick: Boolean,
        preferredMainHand: ItemStack?
    ) {
        val customName = resolveVisibleName(player, created.nameMode, showNick)
        if (customName == null) {
            state.lastCustomName.remove(player)
        } else {
            state.lastCustomName[player] = customName
        }

        state.lastSharedFlags[player] = currentSharedFlags(player, created.capabilities)
        state.lastLocationState[player] = snapshotFor(player, created.renderProfile, player.location)

        if (created.capabilities.mirrorsMainHandIntoItemMetadata) {
            state.lastPrimaryItemState[player] = entityFactory.resolveMirroredItem(created.type, preferredMainHand)
        } else {
            state.lastPrimaryItemState.remove(player)
        }

        if (created.renderProfile.mirrorsMainHandIntoBlockState) {
            state.lastMirroredBlockStateId[player] = entityFactory.resolveBlockDisplayStateId(preferredMainHand)
        } else {
            state.lastMirroredBlockStateId.remove(player)
        }
    }

    fun seedVelocityState(player: Player, created: DisguiseEntityFactory.CreatedDisguise) {
        if (!created.capabilities.requiresSpawnVelocity && !created.capabilities.tracksVelocityContinuously) {
            state.lastVelocityState.remove(player)
            return
        }

        val velocity = if (created.capabilities.tracksVelocityContinuously) {
            player.velocity.toPacketVector()
        } else {
            minimalProjectileSpawnVelocity(player)
        }
        packetDispatcher.applyVelocity(created.entity, velocity)

        if (created.capabilities.tracksVelocityContinuously) {
            state.lastVelocityState[player] = velocity
        } else {
            state.lastVelocityState.remove(player)
        }
    }

    fun updateDisguiseForPlayer(disguisedPlayer: Player, viewer: Player) {
        val data = state.disguisedPlayers[disguisedPlayer] ?: return
        if (!shouldViewerSeeDisguise(disguisedPlayer, viewer, data.showSelf)) {
            cleanupViewerForDisguise(disguisedPlayer, data, viewer.uniqueId)
            return
        }
        queueViewerInit(disguisedPlayer, viewer.uniqueId)
    }

    fun updateEntityGlowing(player: Player, data: DisguiseData, isGlowing: Boolean) {
        val desiredFlags = currentSharedFlags(player, data.capabilities, glowingOverride = isGlowing)
        if (state.lastSharedFlags[player] == desiredFlags) return

        state.lastSharedFlags[player] = desiredFlags
        packetDispatcher.sendSharedFlags(resolveActiveViewers(player), data.entity.entityId, desiredFlags)
    }

    fun recreateDisguise(player: Player, data: DisguiseData, to: Location?) {
        val targetLoc = to ?: player.location

        refreshViewerRelations(player, data, targetLoc)
        state.lastLocationState.remove(player)
        syncMovement(player, data, targetLoc, force = true)
        initializeQueuedViewers(player, data)
        if (data.capabilities.supportsEquipment) {
            packetDispatcher.sendEquipment(resolveActiveViewers(player), data.entity.entityId, data.equipment)
        }
    }

    fun updateMainHandEquipment(player: Player, data: DisguiseData) {
        syncEquipment(player, data)
        syncDerivedMetadata(player, data)
    }

    fun performUpdateTick(
        player: Player,
        data: DisguiseData,
        location: Location,
        refreshLoopingTnt: Boolean,
        refreshViewers: Boolean
    ) {
        syncName(player, data)
        syncSharedFlags(player, data)
        syncVelocity(player, data)
        syncEquipment(player, data)
        syncDerivedMetadata(player, data)
        syncMovement(player, data, location)

        if (refreshLoopingTnt) {
            maintainLoopingTnt(player, data)
        }
        if (refreshViewers) {
            refreshViewerRelations(player, data, location)
        }
        initializeQueuedViewers(player, data)
    }

    fun resolveActiveViewers(owner: Player): List<Player> {
        return state.activeViewers[owner]
            .orEmpty()
            .mapNotNull(Bukkit::getPlayer)
    }

    fun currentLocationSnapshot(
        owner: Player,
        data: DisguiseData
    ): DisguisePacketDispatcher.LocationSnapshot {
        return state.lastLocationState[owner] ?: snapshotFor(owner, data.renderProfile, owner.location)
    }

    fun respawnEntityForViewers(
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

    fun syncViewerPresentation(
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

    fun hiddenMainHandStack(player: Player): ItemStack? {
        return hooker.playerStateManager.getCurrentSavedMainHandItem(player)
            ?.takeUnless { it.type.isAir }
    }

    fun buildHiddenEquipment(player: Player, capabilities: DisguiseCapabilities): List<PacketEquipment> {
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

    fun clearVisibleInventory(player: Player) {
        player.inventory.setItemInMainHand(null)
        player.inventory.setItemInOffHand(null)
        player.inventory.armorContents = arrayOfNulls(4)
    }

    fun resolveVisibleName(
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

    private fun getCurrentTick(): Long = Bukkit.getCurrentTick().toLong()

    private fun getNearbyPlayers(player: Player, location: Location, showSelf: Boolean): List<Player> {
        return location.getNearbyPlayers(100.0)
            .filter { !hooker.utils.isHiddenFromPlayer(it, player) && (showSelf || it != player) }
    }

    private fun isViewerPending(viewerId: UUID): Boolean {
        pruneExpiredViewerPending()
        val untilTick = state.viewerPendingUntilTick[viewerId] ?: return false
        return untilTick > getCurrentTick()
    }

    private fun markViewerPending(viewerId: UUID, ticks: Long = PENDING_INIT_DELAY_TICKS) {
        state.viewerPendingUntilTick[viewerId] = getCurrentTick() + ticks
    }

    private fun pruneExpiredViewerPending(currentTick: Long = getCurrentTick()) {
        state.viewerPendingUntilTick.entries.removeIf { (_, untilTick) -> untilTick <= currentTick }
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
        val wasActive = state.activeViewers[owner]?.remove(viewerId) == true
        state.queuedInitViewers[owner]?.remove(viewerId)
        if (wasActive) {
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                removeViewerTeam(viewer, data)
            }
        }
        data.entity.removeViewer(viewerId)
    }

    private fun cleanupViewerFromAllDisguises(viewerId: UUID) {
        state.disguisedPlayers.forEach { (owner, data) ->
            cleanupViewerForDisguise(owner, data, viewerId)
        }
    }

    private fun queueViewerInit(owner: Player, viewerId: UUID) {
        state.queuedInitViewers.computeIfAbsent(owner) { mutableSetOf() }.add(viewerId)
    }

    private fun initializeViewer(owner: Player, data: DisguiseData, viewer: Player): Boolean {
        if (!shouldViewerSeeDisguise(owner, viewer, data.showSelf)) return false
        if (isViewerPending(viewer.uniqueId)) return false

        hooker.actionLogger.info(
            "Initializing disguise viewer owner=${hooker.actionLogger.playerRef(owner)} viewer=${hooker.actionLogger.playerRef(viewer)} equipment=${data.equipment.size}"
        )

        sendViewerTeam(viewer, data)
        data.entity.addViewer(viewer.uniqueId)
        state.activeViewers.computeIfAbsent(owner) { mutableSetOf() }.add(viewer.uniqueId)
        state.queuedInitViewers[owner]?.remove(viewer.uniqueId)
        syncViewerPresentation(owner, data, listOf(viewer), scheduleMotionStabilization = true)
        return true
    }

    private fun refreshViewerRelations(player: Player, data: DisguiseData, location: Location) {
        val desiredViewers = getNearbyPlayers(player, location, data.showSelf)
        val desiredIds = desiredViewers.map { it.uniqueId }.toSet()
        val active = state.activeViewers.computeIfAbsent(player) { mutableSetOf() }
        val queued = state.queuedInitViewers.computeIfAbsent(player) { mutableSetOf() }

        val toRemove = (active + queued).filter { it !in desiredIds }
        toRemove.forEach { cleanupViewerForDisguise(player, data, it) }

        desiredViewers.forEach { viewer ->
            if (viewer.uniqueId !in active && viewer.uniqueId !in queued) {
                queueViewerInit(player, viewer.uniqueId)
            }
        }
    }

    private fun initializeQueuedViewers(player: Player, data: DisguiseData) {
        val queued = state.queuedInitViewers.computeIfAbsent(player) { mutableSetOf() }
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
        if (state.lastCustomName[player] == desiredName) return

        if (desiredName == null) {
            state.lastCustomName.remove(player)
        } else {
            state.lastCustomName[player] = desiredName
        }
        packetDispatcher.sendCustomName(resolveActiveViewers(player), data.entity.entityId, desiredName)
    }

    private fun syncSharedFlags(player: Player, data: DisguiseData) {
        val desiredFlags = currentSharedFlags(player, data.capabilities)
        if (state.lastSharedFlags[player] == desiredFlags) return

        state.lastSharedFlags[player] = desiredFlags
        packetDispatcher.sendSharedFlags(resolveActiveViewers(player), data.entity.entityId, desiredFlags)
    }

    private fun syncVelocity(player: Player, data: DisguiseData) {
        if (!data.capabilities.tracksVelocityContinuously) return

        val desiredVelocity = player.velocity.toPacketVector()
        if (state.lastVelocityState[player]?.roughlyEquals(desiredVelocity) == true) return

        packetDispatcher.applyVelocity(data.entity, desiredVelocity)
        state.lastVelocityState[player] = desiredVelocity
    }

    private fun syncMovement(player: Player, data: DisguiseData, location: Location, force: Boolean = false) {
        val current = snapshotFor(player, data.renderProfile, location)
        val previous = if (force) null else state.lastLocationState[player]
        val viewers = resolveActiveViewers(player)
        val positionChanged = force || previous == null || previous.hasPositionChangedFrom(current)

        packetDispatcher.sendMovement(viewers, data.entity, previous, current)
        if (data.capabilities.supportsHeadRotation &&
            (force || previous == null || previous.hasHeadYawChangedFrom(current))) {
            packetDispatcher.sendHeadRotation(viewers, data.entity.entityId, current.yaw)
        }

        state.lastLocationState[player] = current

        val attackState = data.capabilities.attackState
        if (positionChanged &&
            attackState.reapplyAfterPositionSyncWhenActive &&
            attackState.isActive(data.entity)) {
            val presentation = attackState.presentation(data.entity, active = true)
            if (presentation != null) {
                sendAttackStatePresentation(viewers, data.entity, presentation)
            }
        }
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
            val currentData = state.disguisedPlayers[currentOwner] ?: return@runLater
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
            if (state.lastPrimaryItemState[player] != desiredItem) {
                if (applyPrimaryItemMetadata(data.entity, desiredItem)) {
                    requiresRefresh = true
                }
                state.lastPrimaryItemState[player] = desiredItem
            }
        }

        if (data.renderProfile.mirrorsMainHandIntoBlockState) {
            val desiredStateId = entityFactory.resolveBlockDisplayStateId(hiddenMainHandStack(player))
            if (state.lastMirroredBlockStateId[player] != desiredStateId) {
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
                state.lastMirroredBlockStateId[player] = desiredStateId
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
        val meta = entity.entityMeta as? FallingBlockMeta ?: return false
        if (meta.blockStateId == blockStateId) return false

        meta.blockStateId = blockStateId
        return true
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

    private fun hiddenOffHandStack(player: Player): ItemStack? {
        return hooker.playerStateManager.getSavedOffHandItem(player)
            ?.takeUnless { it.type.isAir }
    }

    private fun packetFromNullable(item: ItemStack?): PacketItemStack {
        return PacketItemConversionSupport.toPacket(item ?: ItemStack(Material.AIR))
    }

    private fun sendViewerTeam(viewer: Player, data: DisguiseData) {
        data.viewerTeam?.let { ViewerTeamPacketSupport.sendCreate(viewer, it) }
    }

    private fun removeViewerTeam(viewer: Player, data: DisguiseData) {
        data.viewerTeam?.let { ViewerTeamPacketSupport.sendRemove(viewer, it.teamName) }
    }

    private fun sendAttackStatePresentation(
        viewers: Collection<Player>,
        entity: WrapperEntity,
        presentation: DisguiseAttackStatePresentation
    ) {
        when (presentation) {
            is DisguiseAttackStatePresentation.EntityStatus -> {
                packetDispatcher.sendEntityStatus(viewers, entity.entityId, presentation.status)
            }

            is DisguiseAttackStatePresentation.Metadata -> {
                packetDispatcher.sendMetadata(viewers, entity.entityId, presentation.entries)
            }
        }
    }

    private fun org.bukkit.util.Vector.toPacketVector(): Vector3d {
        return Vector3d(x, y, z)
    }

    private fun Vector3d.roughlyEquals(other: Vector3d): Boolean {
        return abs(x - other.x) < 0.0001 && abs(y - other.y) < 0.0001 && abs(z - other.z) < 0.0001
    }
}
