package com.ratger.acreative.commands.admin.npc

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.utils.PacketItemConversionSupport
import com.ratger.acreative.utils.ViewerTeamPacketSupport
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta
import me.tofaa.entitylib.meta.display.TextDisplayMeta
import me.tofaa.entitylib.meta.types.PlayerMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import me.tofaa.entitylib.wrapper.WrapperPlayer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

class NpcRuntimeManager(
    private val hooker: FunctionHooker,
    private val parser: MiniMessageParser,
    private val visibilityRadius: Double,
    private val trackingRadius: Double,
    private val nickDisplaySettings: NpcNickDisplaySettings,
    private val viewerSyncTicks: Long,
    private val lookUpdateTicks: Long,
    private val onInteract: (Player, NpcProfile, NpcInteractionType) -> Unit
) {
    private data class ViewerNpcInstance(
        val profileUuid: UUID,
        val playerEntity: WrapperPlayer,
        val nickEntity: WrapperEntity,
        val teamName: String,
        var isNickSpawned: Boolean,
        var lastYaw: Float,
        var lastPitch: Float
    )

    private data class ViewRotation(
        val yaw: Float,
        val pitch: Float
    )

    private val profilesByName = linkedMapOf<String, NpcProfile>()
    private val instancesByProfile = mutableMapOf<String, MutableMap<UUID, ViewerNpcInstance>>()
    private val interactionProfilesByViewer = mutableMapOf<UUID, MutableMap<Int, String>>()
    private var viewerSyncTaskId: Int? = null
    private var lookTaskId: Int? = null

    fun install(initialProfiles: Collection<NpcProfile>) {
        instancesByProfile.keys.toList().forEach(::removeAllInstancesForProfile)
        profilesByName.clear()
        initialProfiles.forEach { profile ->
            profilesByName[profile.name] = profile.copyDeep()
        }

        if (viewerSyncTaskId == null) {
            viewerSyncTaskId = hooker.tickScheduler.runRepeating(1L, viewerSyncTicks.coerceAtLeast(1L)) {
                syncAllProfiles()
            }
        }
        if (lookTaskId == null) {
            lookTaskId = hooker.tickScheduler.runRepeating(1L, lookUpdateTicks.coerceAtLeast(1L)) {
                updateTrackedRotations()
            }
        }

        syncAllProfiles()
    }

    fun shutdown() {
        viewerSyncTaskId?.let(hooker.tickScheduler::cancel)
        lookTaskId?.let(hooker.tickScheduler::cancel)
        viewerSyncTaskId = null
        lookTaskId = null
        instancesByProfile.keys.toList().forEach(::removeAllInstancesForProfile)
        profilesByName.clear()
        interactionProfilesByViewer.clear()
    }

    fun upsertProfile(profile: NpcProfile) {
        profilesByName[profile.name] = profile.copyDeep()
        rebuildProfile(profile.name)
    }

    fun removeProfile(profileName: String) {
        profilesByName.remove(profileName)
        removeAllInstancesForProfile(profileName)
    }

    fun onViewerJoin(viewer: Player) {
        syncViewer(viewer)
    }

    fun onViewerDisconnect(viewerId: UUID) {
        instancesByProfile.keys.toList().forEach { profileName ->
            removeViewerInstance(profileName, viewerId)
        }
        interactionProfilesByViewer.remove(viewerId)
    }

    fun onViewerWorldOrRespawn(viewer: Player) {
        onViewerDisconnect(viewer.uniqueId)
        syncViewer(viewer)
    }

    fun handleUseUnknownEntity(event: PlayerUseUnknownEntityEvent) {
        if (event.hand != BukkitEquipmentSlot.HAND) {
            return
        }

        val profileName = interactionProfilesByViewer[event.player.uniqueId]?.get(event.entityId) ?: return
        val profile = profilesByName[profileName]?.copyDeep() ?: return
        val interactionType = if (event.isAttack) NpcInteractionType.LEFT_CLICK else NpcInteractionType.RIGHT_CLICK
        onInteract(event.player, profile, interactionType)
    }

    private fun syncAllProfiles() {
        profilesByName.values.toList().forEach(::syncProfile)
    }

    private fun rebuildProfile(profileName: String) {
        removeAllInstancesForProfile(profileName)
        profilesByName[profileName]?.let(::syncProfile)
    }

    private fun syncViewer(viewer: Player) {
        profilesByName.values.forEach { profile ->
            syncProfileForViewer(profile, viewer)
        }
    }

    private fun syncProfile(profile: NpcProfile) {
        Bukkit.getOnlinePlayers().forEach { viewer ->
            syncProfileForViewer(profile, viewer)
        }
    }

    private fun syncProfileForViewer(profile: NpcProfile, viewer: Player) {
        val spawnLocation = resolveSpawnLocation(profile)
        val activeInstances = instancesByProfile.getOrPut(profile.name) { linkedMapOf() }
        if (spawnLocation == null || !shouldViewerSeeNpc(viewer, spawnLocation)) {
            removeViewerInstance(profile.name, viewer.uniqueId)
            return
        }

        val existing = activeInstances[viewer.uniqueId]
        if (existing == null) {
            activeInstances[viewer.uniqueId] = spawnViewerInstance(profile, viewer, spawnLocation)
        } else {
            syncViewerInstancePosition(viewer, spawnLocation, existing)
        }
    }

    private fun spawnViewerInstance(profile: NpcProfile, viewer: Player, spawnLocation: Location): ViewerNpcInstance {
        val fakeUuid = UUID.nameUUIDFromBytes(
            "acreative:npc:${profile.name}:${viewer.uniqueId}".toByteArray(StandardCharsets.UTF_8)
        )
        val teamEntry = "NPC_${fakeUuid.toString().replace("-", "").take(12)}"
        val teamName = "npc_${fakeUuid.toString().replace("-", "").take(12)}"
        val userProfile = UserProfile(fakeUuid, teamEntry, textureProperties(profile.skin))
        val entityId = EntityLib.getPlatform().entityIdProvider.provide(fakeUuid, EntityTypes.PLAYER)
        val desiredRotation = resolveViewRotation(spawnLocation, viewer)
        val packetLocation = packetLocation(spawnLocation, desiredRotation.yaw, desiredRotation.pitch)

        val playerEntity = WrapperPlayer(userProfile, entityId).apply {
            isInTablist = false
            gameMode = GameMode.CREATIVE
            displayName = Component.empty()
        }
        applyPlayerMetaDefaults(playerEntity)

        ViewerTeamPacketSupport.sendCreate(viewer, ViewerTeamPacketSupport.Definition.hidden(teamName, teamEntry))
        sendPlayerInfoAdd(viewer, userProfile)
        playerEntity.addViewer(viewer.uniqueId)
        playerEntity.spawn(packetLocation)
        sendEquipment(viewer, playerEntity.entityId, profile.equipment)
        hooker.tickScheduler.runLater(1L) {
            val activeInstance = instancesByProfile[profile.name]?.get(viewer.uniqueId) ?: return@runLater
            if (activeInstance.playerEntity.entityId == playerEntity.entityId && viewer.isOnline) {
                sendEquipment(viewer, playerEntity.entityId, profile.equipment)
            }
        }
        hooker.tickScheduler.runLater(20L) {
            val activeInstance = instancesByProfile[profile.name]?.get(viewer.uniqueId) ?: return@runLater
            if (activeInstance.playerEntity.entityId == playerEntity.entityId && viewer.isOnline) {
                sendPlayerInfoRemove(viewer, fakeUuid)
            }
        }

        val nickEntity = WrapperEntity(EntityTypes.TEXT_DISPLAY)
        applyNickMeta(nickEntity, profile)
        nickEntity.addViewer(viewer.uniqueId)
        val isNickSpawned = if (shouldViewerSeeNick(viewer, spawnLocation)) {
            nickEntity.spawn(packetLocation(nickDisplayLocation(spawnLocation), 0f))
            true
        } else {
            false
        }

        interactionProfilesByViewer
            .getOrPut(viewer.uniqueId) { linkedMapOf() }[playerEntity.entityId] = profile.name

        return ViewerNpcInstance(
            profileUuid = fakeUuid,
            playerEntity = playerEntity,
            nickEntity = nickEntity,
            teamName = teamName,
            isNickSpawned = isNickSpawned,
            lastYaw = desiredRotation.yaw,
            lastPitch = desiredRotation.pitch
        )
    }

    private fun syncViewerInstancePosition(viewer: Player, spawnLocation: Location, instance: ViewerNpcInstance) {
        val desiredRotation = resolveViewRotation(spawnLocation, viewer)
        val currentLocation = instance.playerEntity.location
        val positionChanged = hasPositionChanged(currentLocation.x, currentLocation.y, currentLocation.z, spawnLocation)
        if (positionChanged) {
            instance.playerEntity.teleport(packetLocation(spawnLocation, desiredRotation.yaw, desiredRotation.pitch))
            if (instance.isNickSpawned) {
                instance.nickEntity.teleport(packetLocation(nickDisplayLocation(spawnLocation), 0f))
            }
            instance.lastYaw = desiredRotation.yaw
            instance.lastPitch = desiredRotation.pitch
        }
        syncNickVisibility(viewer, spawnLocation, instance)
        updateViewerRotation(viewer, spawnLocation, instance)
    }

    private fun updateTrackedRotations() {
        instancesByProfile.entries.toList().forEach { (profileName, viewerInstances) ->
            val profile = profilesByName[profileName]
            if (profile == null) {
                viewerInstances.keys.toList().forEach { viewerId -> removeViewerInstance(profileName, viewerId) }
                return@forEach
            }

            val spawnLocation = resolveSpawnLocation(profile)
            if (spawnLocation == null) {
                viewerInstances.keys.toList().forEach { viewerId -> removeViewerInstance(profileName, viewerId) }
                return@forEach
            }

            viewerInstances.keys.toList().forEach { viewerId ->
                val instance = viewerInstances[viewerId] ?: return@forEach
                val viewer = Bukkit.getPlayer(viewerId)
                if (viewer == null || !shouldViewerSeeNpc(viewer, spawnLocation)) {
                    removeViewerInstance(profileName, viewerId)
                    return@forEach
                }
                syncNickVisibility(viewer, spawnLocation, instance)
                updateViewerRotation(viewer, spawnLocation, instance)
            }
        }
    }

    private fun removeAllInstancesForProfile(profileName: String) {
        instancesByProfile[profileName]?.keys?.toList()?.forEach { viewerId ->
            removeViewerInstance(profileName, viewerId)
        }
        instancesByProfile.remove(profileName)
    }

    private fun removeViewerInstance(profileName: String, viewerId: UUID) {
        val instances = instancesByProfile[profileName] ?: return
        val instance = instances.remove(viewerId) ?: return
        Bukkit.getPlayer(viewerId)?.let { viewer ->
            ViewerTeamPacketSupport.sendRemove(viewer, instance.teamName)
            sendPlayerInfoRemove(viewer, instance.profileUuid)
        }
        interactionProfilesByViewer[viewerId]?.let { byEntityId ->
            byEntityId.remove(instance.playerEntity.entityId)
            if (byEntityId.isEmpty()) {
                interactionProfilesByViewer.remove(viewerId)
            }
        }
        instance.nickEntity.remove()
        instance.playerEntity.remove()
        if (instances.isEmpty()) {
            instancesByProfile.remove(profileName)
        }
    }

    private fun updateViewerRotation(
        viewer: Player,
        spawnLocation: Location,
        instance: ViewerNpcInstance,
        force: Boolean = false
    ) {
        val desiredRotation = resolveViewRotation(spawnLocation, viewer)
        val yawChanged = force || normalizedAngleDiff(instance.lastYaw, desiredRotation.yaw) > YAW_EPSILON
        val pitchChanged = force || abs(instance.lastPitch - desiredRotation.pitch) > PITCH_EPSILON
        if (!yawChanged && !pitchChanged) {
            return
        }

        val bodyRotationPacket = WrapperPlayServerEntityRotation(
            instance.playerEntity.entityId,
            desiredRotation.yaw,
            desiredRotation.pitch,
            true
        )
        val headLookPacket = WrapperPlayServerEntityHeadLook(instance.playerEntity.entityId, desiredRotation.yaw)
        PacketEvents.getAPI().playerManager.sendPacket(viewer, bodyRotationPacket)
        PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
        instance.playerEntity.setLocation(packetLocation(spawnLocation, desiredRotation.yaw, desiredRotation.pitch))
        instance.lastYaw = desiredRotation.yaw
        instance.lastPitch = desiredRotation.pitch
    }

    private fun applyNickMeta(entity: WrapperEntity, profile: NpcProfile) {
        val meta = entity.entityMeta as? TextDisplayMeta ?: return
        meta.text = parser.parse(profile.effectiveVisualNick())
        meta.isShadow = true
        meta.isUseDefaultBackground = false
        meta.backgroundColor = 0
        meta.isSeeThrough = nickDisplaySettings.isSeeThrough
        meta.viewRange = nickDisplaySettings.viewRange
        meta.scale = Vector3f(nickDisplaySettings.scale, nickDisplaySettings.scale, nickDisplaySettings.scale)
        meta.billboardConstraints = AbstractDisplayMeta.BillboardConstraints.VERTICAL
    }

    private fun applyPlayerMetaDefaults(entity: WrapperPlayer) {
        val meta = entity.entityMeta as? PlayerMeta ?: return
        meta.isCapeEnabled = true
        meta.isJacketEnabled = true
        meta.isLeftSleeveEnabled = true
        meta.isRightSleeveEnabled = true
        meta.isLeftLegEnabled = true
        meta.isRightLegEnabled = true
        meta.isHatEnabled = true
        meta.isRightHandMain = true
    }

    private fun textureProperties(skin: NpcSkin?): List<TextureProperty> {
        return skin?.let { listOf(TextureProperty("textures", it.textureValue, it.textureSignature ?: "")) }.orEmpty()
    }

    private fun sendPlayerInfoAdd(viewer: Player, userProfile: UserProfile) {
        val packet = WrapperPlayServerPlayerInfoUpdate(
            WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile)
        )
        PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
    }

    private fun sendPlayerInfoRemove(viewer: Player, profileUuid: UUID) {
        PacketEvents.getAPI().playerManager.sendPacket(
            viewer,
            WrapperPlayServerPlayerInfoRemove(listOf(profileUuid))
        )
    }

    private fun sendEquipment(viewer: Player, entityId: Int, equipment: NpcEquipment) {
        val packet = WrapperPlayServerEntityEquipment(
            entityId,
            listOf(
                packetEquipment(EquipmentSlot.HELMET, equipment.helmet),
                packetEquipment(EquipmentSlot.CHEST_PLATE, equipment.chestplate),
                packetEquipment(EquipmentSlot.LEGGINGS, equipment.leggings),
                packetEquipment(EquipmentSlot.BOOTS, equipment.boots),
                packetEquipment(EquipmentSlot.MAIN_HAND, equipment.mainHand),
                packetEquipment(EquipmentSlot.OFF_HAND, equipment.offHand)
            )
        )
        PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
    }

    private fun packetEquipment(slot: EquipmentSlot, item: ItemStack?): Equipment {
        return Equipment(slot, PacketItemConversionSupport.toPacket(normalizeEquipmentItemOrAir(item)))
    }

    private fun normalizeEquipmentItemOrAir(item: ItemStack?): ItemStack {
        val clone = item?.clone()
        if (clone == null || clone.type.isAir || clone.amount <= 0) {
            return ItemStack(Material.AIR)
        }

        clone.amount = 1
        return clone
    }

    private fun resolveSpawnLocation(profile: NpcProfile): Location? {
        val world = Bukkit.getWorld(profile.location.worldName) ?: return null
        return Location(
            world,
            profile.location.x,
            profile.location.y,
            profile.location.z,
            profile.location.yaw,
            profile.location.pitch
        )
    }

    private fun shouldViewerSeeNpc(viewer: Player, spawnLocation: Location): Boolean {
        if (!viewer.isOnline || viewer.world != spawnLocation.world) {
            return false
        }
        return viewer.location.distanceSquared(spawnLocation) <= visibilityRadius * visibilityRadius
    }

    private fun shouldViewerTrackNpc(viewer: Player, spawnLocation: Location): Boolean {
        if (!viewer.isOnline || viewer.world != spawnLocation.world) {
            return false
        }
        return viewer.location.distanceSquared(spawnLocation) <= trackingRadius * trackingRadius
    }

    private fun shouldViewerSeeNick(viewer: Player, spawnLocation: Location): Boolean {
        if (!viewer.isOnline || viewer.world != spawnLocation.world) {
            return false
        }
        return viewer.location.distanceSquared(spawnLocation) <= nickDisplaySettings.visibilityRadius * nickDisplaySettings.visibilityRadius
    }

    private fun syncNickVisibility(viewer: Player, spawnLocation: Location, instance: ViewerNpcInstance) {
        val shouldSpawnNick = shouldViewerSeeNick(viewer, spawnLocation)
        if (shouldSpawnNick == instance.isNickSpawned) {
            return
        }

        if (shouldSpawnNick) {
            instance.nickEntity.spawn(packetLocation(nickDisplayLocation(spawnLocation), 0f))
        } else {
            instance.nickEntity.despawn()
        }
        instance.isNickSpawned = shouldSpawnNick
    }

    private fun resolveViewRotation(spawnLocation: Location, viewer: Player): ViewRotation {
        if (!shouldViewerTrackNpc(viewer, spawnLocation)) {
            return ViewRotation(
                yaw = spawnLocation.yaw,
                pitch = spawnLocation.pitch
            )
        }

        val viewerEye = viewer.eyeLocation
        val dx = viewerEye.x - spawnLocation.x
        val dy = viewerEye.y - (spawnLocation.y + NPC_EYE_HEIGHT)
        val dz = viewerEye.z - spawnLocation.z
        val fullDistance = sqrt(dx * dx + dy * dy + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = if (fullDistance <= POSITION_EPSILON) {
            0f
        } else {
            Math.toDegrees(-asin((dy / fullDistance).coerceIn(-1.0, 1.0))).toFloat()
        }
        return ViewRotation(yaw = yaw, pitch = pitch)
    }

    private fun packetLocation(location: Location, yaw: Float, pitch: Float = 0f) = com.github.retrooper.packetevents.protocol.world.Location(
        location.x,
        location.y,
        location.z,
        yaw,
        pitch
    )

    private fun nickDisplayLocation(spawnLocation: Location): Location {
        return spawnLocation.clone().add(0.0, nickDisplaySettings.verticalOffset + nickDisplaySettings.additionalYOffset, 0.0)
    }

    private fun hasPositionChanged(currentX: Double, currentY: Double, currentZ: Double, desiredLocation: Location): Boolean {
        return abs(currentX - desiredLocation.x) > POSITION_EPSILON ||
            abs(currentY - desiredLocation.y) > POSITION_EPSILON ||
            abs(currentZ - desiredLocation.z) > POSITION_EPSILON
    }

    private fun normalizedAngleDiff(left: Float, right: Float): Float {
        var diff = (left - right) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return abs(diff)
    }

    private companion object {
        const val POSITION_EPSILON = 0.001
        const val YAW_EPSILON = 0.5f
        const val PITCH_EPSILON = 0.5f
        const val NPC_EYE_HEIGHT = 1.62
    }
}
