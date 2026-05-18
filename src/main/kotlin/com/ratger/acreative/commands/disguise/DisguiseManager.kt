package com.ratger.acreative.commands.disguise

import com.ratger.acreative.commands.disguise.model.DisguiseData
import com.ratger.acreative.commands.disguise.model.DisguiseFlags
import com.ratger.acreative.commands.disguise.model.DisguiseRequest
import com.ratger.acreative.commands.disguise.model.DisguiseRuntimeState
import com.ratger.acreative.commands.disguise.service.DisguiseAccessPolicy
import com.ratger.acreative.commands.disguise.service.DisguiseAttackService
import com.ratger.acreative.commands.disguise.service.DisguiseSyncService
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permissible

class DisguiseManager(private val hooker: FunctionHooker) {
    private companion object {
        const val UPDATE_PERIOD_TICKS = 2L
        const val VIEWER_REFRESH_PERIOD_TICKS = 20L
        const val LOOPING_TNT_RESPAWN_PERIOD_TICKS = 60L
    }

    data class CacheSnapshot(
        val disguisedPlayers: Int,
        val viewerRelations: Int,
        val queuedViewerRelations: Int,
        val pendingViewers: Int,
        val rememberedNames: Int,
        val rememberedGlowStates: Int
    )

    private val state = DisguiseRuntimeState()
    private val entityFactory = DisguiseEntityFactory()
    private val packetDispatcher = DisguisePacketDispatcher()
    private val accessPolicy = DisguiseAccessPolicy(hooker)
    private val syncService = DisguiseSyncService(hooker, state, entityFactory, packetDispatcher)
    private val attackService = DisguiseAttackService(hooker, state, packetDispatcher, syncService)

    val disguisedPlayers
        get() = state.disguisedPlayers

    val donationRestrictedEntities
        get() = accessPolicy.donationRestrictedEntities

    fun isBlockedDisguiseType(type: EntityType): Boolean = accessPolicy.isBlockedDisguiseType(type)

    fun canUseTextDisguise(permissible: Permissible): Boolean = accessPolicy.canUseTextDisguise(permissible)

    fun onViewerJoin(viewer: Player) = syncService.onViewerJoin(viewer)

    fun onViewerDisconnect(viewerId: java.util.UUID) = syncService.onViewerDisconnect(viewerId)

    fun onViewerWorldOrRespawn(viewer: Player) = syncService.onViewerWorldOrRespawn(viewer)

    fun disguisePlayer(
        player: Player,
        type: String?,
        playerName: String?,
        flags: List<String>,
        textDisplayRaw: String? = null,
        slimeSize: Int? = null
    ) {
        disguisePlayer(
            player,
            DisguiseRequest(
                type = type,
                playerName = playerName,
                flags = flags,
                textDisplayText = textDisplayRaw,
                slimeSize = slimeSize
            )
        )
    }

    fun disguisePlayer(player: Player, request: DisguiseRequest) {
        if (request.type == null) {
            if (state.disguisedPlayers.containsKey(player)) {
                undisguisePlayer(player)
                return
            }
            hooker.messageManager.sendChat(player, MessageKey.USAGE_DISGUISE)
            return
        }

        if (request.type.equals("off", ignoreCase = true)) {
            undisguisePlayer(player)
            return
        }

        val parsedFlags = DisguiseFlags.parse(request.flags)
        if (parsedFlags.requiresNickPermission && !player.hasPermission(DisguisePermissions.NICK)) {
            hooker.permissionManager.sendPermissionDenied(player, "disguise.nick")
            return
        }

        if (request.type.equals("player", ignoreCase = true)) {
            handlePlayerDisguise(player, request, parsedFlags)
            return
        }

        handleEntityDisguise(player, request, parsedFlags)
    }

    fun cacheSnapshot(): CacheSnapshot = CacheSnapshot(
        disguisedPlayers = state.disguisedPlayers.size,
        viewerRelations = state.activeViewers.values.sumOf { it.size },
        queuedViewerRelations = state.queuedInitViewers.values.sumOf { it.size },
        pendingViewers = syncService.activePendingViewerCount(),
        rememberedNames = state.lastCustomName.size,
        rememberedGlowStates = state.lastSharedFlags.size
    )

    fun undisguisePlayer(player: Player, silent: Boolean = false) {
        state.disguisedPlayers[player]?.let { data ->
            hooker.actionLogger.info(
                "Disguise stop for ${hooker.actionLogger.playerRef(player)} viewers=${state.activeViewers[player]?.size ?: 0}"
            )

            data.entity.remove()
            player.isInvisible = false
            if (hooker.utils.isGlowing(player)) player.isGlowing = true
            hooker.playerStateManager.restorePlayerInventory(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.DISGUISED)
            state.disguisedPlayers.remove(player)
            syncService.stopTracking(player, data)
            attackService.stopTracking(player)
            if (!silent) {
                hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE_REMOVED)
            }
        }
    }

    fun updateDisguiseForPlayer(disguisedPlayer: Player, viewer: Player) {
        syncService.updateDisguiseForPlayer(disguisedPlayer, viewer)
    }

    fun updateEntityGlowing(player: Player, isGlowing: Boolean) {
        val data = state.disguisedPlayers[player] ?: return
        syncService.updateEntityGlowing(player, data, isGlowing)
    }

    fun sendAttackAnimation(player: Player) {
        attackService.sendAttackAnimation(player)
    }

    fun recreateDisguise(player: Player, to: org.bukkit.Location?) {
        val data = state.disguisedPlayers[player] ?: return
        syncService.recreateDisguise(player, data, to)
    }

    fun updateMainHandEquipment(player: Player) {
        val data = state.disguisedPlayers[player] ?: return
        syncService.updateMainHandEquipment(player, data)
    }

    private fun handlePlayerDisguise(
        player: Player,
        request: DisguiseRequest,
        flags: DisguiseFlags
    ) {
        if (!player.hasPermission(DisguisePermissions.PLAYER)) {
            hooker.permissionManager.sendPermissionDenied(player, "disguise.player")
            return
        }

        val targetName = request.playerName?.takeIf { it.isNotBlank() } ?: run {
            hooker.messageManager.sendChat(player, MessageKey.USAGE_DISGUISE)
            return
        }
        val target = accessPolicy.findOnlinePlayer(targetName) ?: run {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        val identityKey = "player:${target.uniqueId}"
        if (switchOrToggleCurrentDisguise(player, EntityType.PLAYER, identityKey, flags)) {
            return
        }

        startDisguise(
            player = player,
            identityKey = identityKey,
            showSelf = flags.showSelf,
            showNick = flags.showNick,
            buildDisguise = { preferredMainHand -> entityFactory.createPlayerDisguise(player, target, preferredMainHand) }
        )
    }

    private fun handleEntityDisguise(
        player: Player,
        request: DisguiseRequest,
        flags: DisguiseFlags
    ) {
        val entityType = accessPolicy.parseEntityType(request.type)
        if (entityType == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        if (accessPolicy.isBlockedDisguiseType(entityType)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_BLOCKED)
            return
        }

        if (entityType == EntityType.TEXT_DISPLAY && !accessPolicy.canUseTextDisguise(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        if (!player.hasPermission(DisguisePermissions.EXTENDED) && entityType in donationRestrictedEntities) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_TYPE)
            return
        }

        val slimeSize = if (accessPolicy.isSlimeType(entityType)) {
            accessPolicy.normalizeSlimeSize(request.slimeSize)
        } else {
            null
        }
        val identityKey = accessPolicy.resolveIdentityKey(entityType, request.textDisplayText, slimeSize)
        if (switchOrToggleCurrentDisguise(player, entityType, identityKey, flags)) {
            return
        }

        startDisguise(
            player = player,
            identityKey = identityKey,
            showSelf = flags.showSelf,
            showNick = flags.showNick,
            buildDisguise = { preferredMainHand ->
                val textDisplayComponent = if (entityType == EntityType.TEXT_DISPLAY) {
                    hooker.messageManager.renderMiniMessage(request.textDisplayText.orEmpty())
                } else {
                    null
                }
                entityFactory.createMobDisguise(
                    owner = player,
                    type = entityType,
                    preferredMainHand = preferredMainHand,
                    textDisplayText = textDisplayComponent,
                    slimeSize = slimeSize
                )
            }
        )
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

        val preferredMainHand = syncService.hiddenMainHandStack(player)
        val created = buildDisguise(preferredMainHand) ?: run {
            hooker.playerStateManager.restorePlayerInventory(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.DISGUISED)
            hooker.messageManager.sendChat(player, MessageKey.ERROR_DISGUISE_BLOCKED)
            return
        }

        created.entity.entityMeta.setNotifyAboutChanges(false)

        val equipment = syncService.buildHiddenEquipment(player, created.capabilities)
        syncService.clearVisibleInventory(player)
        syncService.seedStateCaches(player, created, showNick, preferredMainHand)
        syncService.seedVelocityState(player, created)

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
        state.disguisedPlayers[player] = data

        hooker.actionLogger.info(
            "Disguise start for ${hooker.actionLogger.playerRef(player)} type=${created.type.name} showSelf=$showSelf showNick=$showNick equipment=${equipment.size}"
        )

        created.entity.spawn(syncService.currentLocationSnapshot(player, data).toPacketLocation())

        player.isInvisible = true
        player.isGlowing = false
        syncService.beginDisguise(player, data)

        scheduleUpdateTask(player)
        hooker.messageManager.sendChat(player, MessageKey.SUCCESS_DISGUISE)
    }

    private fun switchOrToggleCurrentDisguise(
        player: Player,
        type: EntityType,
        identityKey: String,
        flags: DisguiseFlags
    ): Boolean {
        val current = state.disguisedPlayers[player] ?: return false
        if (current.type == type &&
            current.identityKey == identityKey &&
            current.showSelf == flags.showSelf &&
            current.showNick == flags.showNick) {
            undisguisePlayer(player)
            return true
        }

        undisguisePlayer(player, true)
        return false
    }

    private fun scheduleUpdateTask(player: Player) {
        var runCounter = 0
        val viewerRefreshEvery = (VIEWER_REFRESH_PERIOD_TICKS / UPDATE_PERIOD_TICKS).toInt().coerceAtLeast(1)
        val loopingTntRefreshEvery = (LOOPING_TNT_RESPAWN_PERIOD_TICKS / UPDATE_PERIOD_TICKS).toInt().coerceAtLeast(1)

        val taskId = hooker.tickScheduler.runRepeating(0L, UPDATE_PERIOD_TICKS) {
            if (!player.isOnline || !state.disguisedPlayers.containsKey(player)) {
                state.tasks.remove(player)?.let(hooker.tickScheduler::cancel)
                return@runRepeating
            }

            val currentData = state.disguisedPlayers[player] ?: return@runRepeating
            val playerLoc = player.location

            syncService.performUpdateTick(
                player = player,
                data = currentData,
                location = playerLoc,
                refreshLoopingTnt = runCounter % loopingTntRefreshEvery == 0,
                refreshViewers = runCounter % viewerRefreshEvery == 0
            )
            attackService.syncContinuousAttackState(player, currentData)

            runCounter += 1
        }
        state.tasks[player] = taskId
    }
}
