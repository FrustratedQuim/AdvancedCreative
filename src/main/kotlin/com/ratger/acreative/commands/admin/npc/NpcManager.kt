package com.ratger.acreative.commands.admin.npc

import com.ratger.acreative.commands.admin.npc.NpcSkinService.ResolutionResult
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.UUID

class NpcManager(
    private val hooker: FunctionHooker
) {
    sealed interface CreateResult {
        data class Success(val profile: NpcProfile) : CreateResult
        data object InvalidName : CreateResult
        data object AlreadyExists : CreateResult
    }

    sealed interface ProfileResult {
        data class Success(val profile: NpcProfile) : ProfileResult
        data object ProfileNotFound : ProfileResult
    }

    sealed interface TeleportResult {
        data class Success(val profile: NpcProfile) : TeleportResult
        data object ProfileNotFound : TeleportResult
        data object LocationUnavailable : TeleportResult
    }

    sealed interface SkinApplyResult {
        data class Success(val profile: NpcProfile) : SkinApplyResult
        data object ProfileNotFound : SkinApplyResult
        data object UnknownPlayer : SkinApplyResult
    }

    private val config = hooker.configManager.config
    private val equipmentCodec = NpcEquipmentCodec()
    private val interactionHandlers = mutableListOf<NpcInteractionHandler>()
    private val licensedProfileLookupService = LicensedProfileLookupService()
    private val flushLock = Any()
    private val storage = NpcJsonStorage(
        folder = File(hooker.plugin.dataFolder, config.getString("npc.storage.directory", "npcs")!!),
        equipmentCodec = equipmentCodec,
        logger = hooker.plugin.logger
    )
    private val registry = NpcProfileRegistry(storage)
    private val skinService = NpcSkinService(
        licensedProfileLookupService = licensedProfileLookupService
    )
    private val runtime = NpcRuntimeManager(
        hooker = hooker,
        parser = MiniMessageParser(),
        visibilityRadius = config.getDouble("npc.visibility-radius", 100.0),
        trackingRadius = config.getDouble("npc.look.track-radius", 20.0),
        nickDisplaySettings = NpcNickDisplaySettings(
            verticalOffset = config.getDouble("npc.nick.vertical-offset", 2.0),
            additionalYOffset = config.getDouble("npc.nick.additional-y-offset", -0.1),
            visibilityRadius = config.getDouble(
                "npc.nick.visibility-radius",
                config.getDouble("npc.nick.view-range", 25.0)
            ).coerceAtLeast(0.0),
            viewRange = config.getDouble("npc.nick.client-view-range", 1.0).coerceAtLeast(0.0).toFloat(),
            isSeeThrough = config.getBoolean("npc.nick.see-through", false),
            scale = config.getDouble("npc.nick.scale", 0.75).toFloat().coerceAtLeast(0f)
        ),
        viewerSyncTicks = config.getLong("npc.viewer-sync-ticks", 10L),
        lookUpdateTicks = config.getLong("npc.look.update-ticks", config.getLong("npc.look-update-ticks", 2L)),
        onInteract = ::handleNpcInteraction
    )
    private val commandService = NpcCommandService(hooker, this)
    private var flushTask: BukkitTask? = null
    private var delayedFlushTask: BukkitTask? = null

    fun init() {
        registry.load()
        registerInteractionHandler(NpcBuiltInInteractionHandler(hooker))
        runtime.install(registry.snapshot())
        val flushIntervalTicks = 6L * 60L * 60L * 20L
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            hooker.plugin,
            Runnable { registry.flushIfDirty() },
            flushIntervalTicks,
            flushIntervalTicks
        )
    }

    fun shutdown() {
        flushTask?.cancel()
        synchronized(flushLock) {
            delayedFlushTask?.cancel()
            delayedFlushTask = null
        }
        registry.flushAll()
        runtime.shutdown()
    }

    fun commandService(): NpcCommandService = commandService

    fun registerInteractionHandler(handler: NpcInteractionHandler) {
        interactionHandlers += handler
    }

    fun profileSuggestions(prefix: String): List<String> =
        registry.names().filter { it.startsWith(prefix, ignoreCase = true) }

    fun onlinePlayerSuggestions(prefix: String): List<String> =
        Bukkit.getOnlinePlayers().map(Player::getName).filter { it.startsWith(prefix, ignoreCase = true) }

    fun createProfile(player: Player, name: String): CreateResult {
        if (!PROFILE_NAME_PATTERN.matches(name)) {
            return CreateResult.InvalidName
        }
        if (registry.contains(name)) {
            return CreateResult.AlreadyExists
        }

        val profile = NpcProfile(
            name = name,
            location = player.location.toNpcLocation(),
            visualNick = name,
            skin = null,
            equipment = NpcEquipment.EMPTY
        )
        registry.create(profile)
        runtime.upsertProfile(profile)
        scheduleFlush()
        return CreateResult.Success(profile)
    }

    fun applySkinAsync(profileName: String, input: String): java.util.concurrent.CompletableFuture<SkinApplyResult> {
        val currentProfile = registry.find(profileName)
            ?: return java.util.concurrent.CompletableFuture.completedFuture(SkinApplyResult.ProfileNotFound)

        return skinService.resolveAsync(input).thenApply { result ->
            when (result) {
                is ResolutionResult.Success -> {
                    val updatedProfile = registry.update(currentProfile.name) { profile ->
                        profile.copy(skin = result.skin)
                    } ?: return@thenApply SkinApplyResult.ProfileNotFound
                    scheduleFlush()
                    runSync { runtime.upsertProfile(updatedProfile) }
                    SkinApplyResult.Success(updatedProfile)
                }
                ResolutionResult.UnknownPlayer -> SkinApplyResult.UnknownPlayer
            }
        }
    }

    fun updateNick(profileName: String, visualNick: String): ProfileResult {
        val updatedProfile = registry.update(profileName) { profile ->
            profile.copy(visualNick = visualNick)
        } ?: return ProfileResult.ProfileNotFound
        runtime.upsertProfile(updatedProfile)
        scheduleFlush()
        return ProfileResult.Success(updatedProfile)
    }

    fun updateEquipment(player: Player, profileName: String): ProfileResult {
        val updatedProfile = registry.update(profileName) { profile ->
            profile.copy(equipment = equipmentCodec.captureFrom(player))
        } ?: return ProfileResult.ProfileNotFound
        runtime.upsertProfile(updatedProfile)
        scheduleFlush()
        return ProfileResult.Success(updatedProfile)
    }

    fun updatePositionFromPlayer(player: Player, profileName: String): ProfileResult {
        val updatedProfile = registry.update(profileName) { profile ->
            profile.copy(location = player.location.toNpcLocation())
        } ?: return ProfileResult.ProfileNotFound
        runtime.upsertProfile(updatedProfile)
        scheduleFlush()
        return ProfileResult.Success(updatedProfile)
    }

    fun removeProfile(profileName: String): ProfileResult {
        val removedProfile = registry.remove(profileName) ?: return ProfileResult.ProfileNotFound
        runtime.removeProfile(removedProfile.name)
        scheduleFlush()
        return ProfileResult.Success(removedProfile)
    }

    fun teleportPlayerToProfile(player: Player, profileName: String): TeleportResult {
        val profile = registry.find(profileName) ?: return TeleportResult.ProfileNotFound
        val targetLocation = profile.toBukkitLocation() ?: return TeleportResult.LocationUnavailable
        player.teleport(targetLocation)
        return TeleportResult.Success(profile)
    }

    fun onViewerJoin(viewer: Player) = runtime.onViewerJoin(viewer)

    fun onViewerDisconnect(viewerId: UUID) = runtime.onViewerDisconnect(viewerId)

    fun onViewerWorldOrRespawn(viewer: Player) = runtime.onViewerWorldOrRespawn(viewer)

    fun handleUseUnknownEntity(event: com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent) {
        runtime.handleUseUnknownEntity(event)
    }

    private fun handleNpcInteraction(player: Player, profile: NpcProfile, interactionType: NpcInteractionType) {
        if (interactionHandlers.isEmpty()) {
            hooker.actionLogger.info {
                "NPC interaction handled profile=${profile.name} type=$interactionType player=${hooker.actionLogger.playerRef(player)}"
            }
            return
        }

        interactionHandlers.forEach { handler ->
            runCatching {
                handler.handle(
                    NpcInteractionContext(
                        player = player,
                        profile = profile.copyDeep(),
                        interactionType = interactionType
                    )
                )
            }
                .onFailure { error ->
                    hooker.plugin.logger.warning("NPC interaction handler failed for ${profile.name}: ${error.message}")
                }
        }
    }

    private fun runSync(action: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            action()
        } else {
            Bukkit.getScheduler().runTask(hooker.plugin, Runnable { action() })
        }
    }

    private fun scheduleFlush() {
        synchronized(flushLock) {
            delayedFlushTask?.cancel()
            var scheduledTask: BukkitTask? = null
            scheduledTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                hooker.plugin,
                Runnable {
                    registry.flushIfDirty()
                    synchronized(flushLock) {
                        if (delayedFlushTask?.taskId == scheduledTask?.taskId) {
                            delayedFlushTask = null
                        }
                    }
                },
                FLUSH_DEBOUNCE_TICKS
            )
            delayedFlushTask = scheduledTask
        }
    }

    private fun Location.toNpcLocation(): NpcLocation = NpcLocation(
        worldName = world.name,
        x = x,
        y = y,
        z = z,
        yaw = yaw,
        pitch = pitch
    )

    private fun NpcProfile.toBukkitLocation(): Location? {
        val world = Bukkit.getWorld(location.worldName) ?: return null
        return Location(world, location.x, location.y, location.z, location.yaw, location.pitch)
    }

    private companion object {
        val PROFILE_NAME_PATTERN = Regex("^[A-Za-z0-9_-]+$")
        const val FLUSH_DEBOUNCE_TICKS = 40L
    }
}
