package com.ratger.acreative.commands.lay

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.MessageChannel
import com.ratger.acreative.core.MessageKey
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.ratger.acreative.core.FunctionHooker
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.tofaa.entitylib.meta.types.PlayerMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.Player
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import java.util.*
import kotlin.collections.iterator
import kotlin.math.abs

class LayManager(private val hooker: FunctionHooker) {

    companion object {
        private const val ARMORSTAND_REATTACH_DELAY_TICKS = 5L
        private const val CHECK_TASK_PERIOD_TICKS = 10L
        private const val INTERACT_DELAY_MS = 500L
        private const val MAX_INTERACT_DISTANCE = 3.0
    }

    private data class LayPose(
        val snappedYaw: Int,
        val npcYaw: Float,
        val offsetX: Double,
        val offsetZ: Double
    )

    val layingMap: MutableMap<Player, LayData> = mutableMapOf()
    private val lastInteract: MutableMap<UUID, Long> = mutableMapOf()

    data class LayData(
        val npc: WrapperEntity,
        val headRotationTaskId: Int,
        val armorStandId: UUID,
        val bedLocation: Location?,
        val equipment: List<Equipment>,
        val baseYaw: Float,
        val teamName: String
    )

    fun canLay(player: Player): Boolean {
        val blockBelow = player.location.clone().add(0.0, -1.0, 0.0).block
        return player.gameMode != GameMode.SPECTATOR &&
                !player.isFlying &&
                blockBelow.type.isSolid
    }

    fun layPlayer(player: Player) {
        if (hooker.utils.isPissing(player)) {
            hooker.pissManager.stopPiss(player)
        }
        if (!canLay(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_LAY_IN_AIR)
            return
        }
        val location = player.location.clone()
        val yaw = player.location.yaw
        hooker.playerStateManager.activateState(player, PlayerStateType.LAYING)
        layPlayerAt(player, location, yaw, null)
    }

    fun findBedHeadBlock(block: Block): Block? {
        val directions = listOf(
            Vector(1.0, 0.0, 0.0), Vector(-1.0, 0.0, 0.0),
            Vector(0.0, 0.0, 1.0), Vector(0.0, 0.0, -1.0)
        )

        if (block.blockData is Bed && (block.blockData as Bed).part == Bed.Part.HEAD) {
            return block
        }

        for (dir in directions) {
            val nearbyBlock = block.location.clone().add(dir).block
            if (nearbyBlock.type.name.contains("BED")) {
                val bedData = nearbyBlock.blockData as? Bed
                if (bedData?.part == Bed.Part.HEAD) {
                    return nearbyBlock
                }
            }
        }
        return null
    }

    fun handleRightClickBlock(player: Player, block: Block): Boolean {
        if (layingMap.containsKey(player)) return true
        if (!isValidLayInteraction(player, block)) return false

        lastInteract[player.uniqueId] = System.currentTimeMillis()
        if (!canLay(player)) return false

        val headBlock = findBedHeadBlock(block) ?: return false
        val location = headBlock.location.clone().add(0.5, 0.5625, 0.5)
        val yaw = resolveBedYaw(headBlock, player)

        hooker.playerStateManager.activateState(player, PlayerStateType.LAYING)
        layPlayerAt(player, location, yaw, headBlock.location)
        return true
    }

    fun layPlayerAt(player: Player, location: Location, yaw: Float, bedLocation: Location?) {
        val pose = resolveLayPose(yaw)

        val npcLocation = location.clone().add(pose.offsetX, 0.12, pose.offsetZ).apply {
            this.yaw = pose.npcYaw
            pitch = 0f
        }

        if (isNpcLocationOccupied(player, npcLocation)) return

        val (entity, equipment, teamName) = hooker.entityManager.createPlayerNPC(player, npcLocation, pose.npcYaw, hooker.utils.isGlowing(player))

        val armorStandLocation = location.clone().add(0.0, 0.3, 0.0)
        val armorStand = hooker.entityManager.createArmorStand(armorStandLocation, pose.npcYaw).apply {
            customName(getDisplayName(player))
            isCustomNameVisible = true
        }

        if (!hooker.utils.isSitting(player)) {
            armorStand.addPassenger(player)
        }
        hooker.tickScheduler.runLater(ARMORSTAND_REATTACH_DELAY_TICKS) {
            if (!armorStand.passengers.contains(player) && hooker.utils.isSitting(player)) {
                armorStand.addPassenger(player)
            }
        }

        val taskId = startHeadRotationTask(player, entity.entityId, entity::isSpawned, pose)

        layingMap[player] = LayData(entity, taskId, armorStand.uniqueId, bedLocation, equipment, pose.snappedYaw.toFloat(), teamName)

        applyLayingPlayerState(player)
    }

    fun unlayPlayer(player: Player, shouldUnsit: Boolean = true) {
        if (!layingMap.containsKey(player)) {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.LAYING)
            return
        }

        if (shouldUnsit && hooker.utils.isSitting(player)) {
            hooker.sitManager.unsitPlayer(player)
        }

        layingMap.remove(player)?.let { data ->
            val scoreboard = Scoreboard()
            val team = PlayerTeam(scoreboard, data.teamName)
            val removePacket = ClientboundSetPlayerTeamPacket.createRemovePacket(team)
            forEachVisibleViewer(player) { viewer ->
                (viewer as CraftPlayer).handle.connection.send(removePacket)
            }

            hooker.tickScheduler.cancel(data.headRotationTaskId)

            val playerInfoRemove = WrapperPlayServerPlayerInfoRemove(listOf(data.npc.uuid))
            forEachVisibleViewer(player) { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, playerInfoRemove)
            }
            data.npc.remove()

            player.world.getEntity(data.armorStandId)?.remove()

            if (data.bedLocation != null) {
                val targetLocation = data.bedLocation.clone().apply {
                    y += 0.5625
                }
                player.teleport(targetLocation)
            }

            restoreLayingPlayerState(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.LAYING)
            hooker.playerStateManager.refreshPlayerPose(player)
        }
    }

    private fun isValidLayInteraction(player: Player, block: Block): Boolean {
        if (player.isSneaking) return false
        if (player.location.distance(block.location) > MAX_INTERACT_DISTANCE) return false
        if (!block.type.name.contains("BED") || !player.world.isDayTime) return false
        val currentTime = System.currentTimeMillis()
        return currentTime - (lastInteract[player.uniqueId] ?: 0) >= INTERACT_DELAY_MS
    }

    private fun resolveBedYaw(headBlock: Block, player: Player): Float {
        val bedData = headBlock.blockData as? Bed
        return when (bedData?.facing?.name) {
            "NORTH" -> -1f
            "SOUTH" -> 180f
            "WEST" -> -90f
            "EAST" -> 90f
            else -> player.location.yaw
        }
    }

    private fun resolveLayPose(yaw: Float): LayPose {
        val yFloat = ((yaw + 180) % 360)
        val snappedYaw = when (val angle = yFloat.toInt() - 180) {
            in -45..45 -> 0
            in 46..135 -> 90
            in -135..-46 -> -90
            else -> if (angle > 135) 180 else -180
        }

        return when (snappedYaw) {
            0 -> LayPose(0, -90f, 0.0, 1.45)
            90 -> LayPose(90, 180f, -1.45, 0.0)
            180, -180 -> LayPose(snappedYaw, 90f, 0.0, -1.45)
            -90 -> LayPose(-90, 0f, 1.45, 0.0)
            else -> LayPose(0, -90f, 0.0, 1.45)
        }
    }

    private fun isNpcLocationOccupied(player: Player, npcLocation: Location): Boolean {
        for ((otherPlayer, data) in layingMap) {
            if (otherPlayer == player) continue
            val otherNpcLocation = data.npc.location
            if (otherPlayer.world == player.world &&
                abs(otherNpcLocation.x - npcLocation.x) <= 0.5 &&
                abs(otherNpcLocation.y - npcLocation.y) <= 0.5 &&
                abs(otherNpcLocation.z - npcLocation.z) <= 0.5) {
                return true
            }
        }
        return false
    }

    private fun startHeadRotationTask(player: Player, entityId: Int, isSpawned: () -> Boolean, pose: LayPose): Int {
        var taskId = 0
        taskId = hooker.tickScheduler.runRepeating(0L, 2L) {
            if (!player.isOnline || !isSpawned()) {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }

            val finalYaw = pose.npcYaw + normalizeYawDiff(pose.snappedYaw - player.location.yaw.toInt()) * -1
            val headLookPacket = WrapperPlayServerEntityHeadLook(entityId, finalYaw)
            forEachVisibleViewer(player) { viewer ->
                PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
            }
        }
        return taskId
    }

    private fun normalizeYawDiff(diff: Int): Int {
        var normalized = (diff + 540) % 360 - 180
        if (normalized > 90) normalized = 90
        if (normalized < -90) normalized = -90
        return normalized
    }

    private fun applyLayingPlayerState(player: Player) {
        hooker.playerStateManager.savePlayerInventory(player)
        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.setItemInOffHand(null)
        player.inventory.setItem(player.inventory.heldItemSlot, null)

        player.isInvisible = true
        player.isInvulnerable = true
        player.isCollidable = false
        player.isSilent = true

        hooker.messageManager.startRepeatingActionBar(player, MessageKey.ACTION_POSE_UNSET)
    }

    private fun restoreLayingPlayerState(player: Player) {
        hooker.playerStateManager.restorePlayerInventory(player)
        player.isInvisible = false
        player.isInvulnerable = false
        player.isCollidable = true
        player.isSilent = false
        hooker.messageManager.stopRepeating(player, MessageChannel.ACTION_BAR)
    }

    private inline fun forEachVisibleViewer(source: Player, action: (Player) -> Unit) {
        Bukkit.getOnlinePlayers().forEach { viewer ->
            if (!hooker.utils.isHiddenFromPlayer(viewer, source)) {
                action(viewer)
            }
        }
    }

    private fun getDisplayName(player: Player): Component {
        val team = player.scoreboard.getEntryTeam(player.name)
        return if (team != null) {
            val prefix = team.prefix()
            val suffix = team.suffix()
            val name = Component.text(player.name)
            Component.join(JoinConfiguration.noSeparators(), prefix, name, suffix)
        } else {
            Component.text(player.name)
        }
    }

    fun startArmorStandChecker() {
        hooker.tickScheduler.runRepeating(CHECK_TASK_PERIOD_TICKS, CHECK_TASK_PERIOD_TICKS) {
            val playersToUnlay = mutableListOf<Player>()
            for ((player, data) in layingMap) {
                val entity = player.world.getEntity(data.armorStandId)
                if (entity == null || !entity.isValid) {
                    playersToUnlay.add(player)
                }
            }
            playersToUnlay.forEach { unlayPlayer(it) }
        }
    }


    fun onViewerJoin(viewer: Player) {
        layingMap.forEach { (owner, data) ->
            if (!owner.isOnline || owner.world != viewer.world) return@forEach
            if (hooker.utils.isHiddenFromPlayer(viewer, owner)) return@forEach

            val profileName = "NPC_${data.npc.uuid.toString().substring(0, 8)}"
            val textures = collectOwnerTextures(owner)
            val userProfile = UserProfile(data.npc.uuid, profileName, textures)

            val scoreboard = Scoreboard()
            val team = PlayerTeam(scoreboard, data.teamName)
            team.playerPrefix = net.minecraft.network.chat.Component.empty()
            team.playerSuffix = net.minecraft.network.chat.Component.empty()
            team.nameTagVisibility = net.minecraft.world.scores.Team.Visibility.NEVER
            team.collisionRule = net.minecraft.world.scores.Team.CollisionRule.NEVER
            team.players.add(profileName)

            val createTeamPacket = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true)
            (viewer as CraftPlayer).handle.connection.send(createTeamPacket)

            val playerInfoUpdate = WrapperPlayServerPlayerInfoUpdate(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile)
            )
            PacketEvents.getAPI().playerManager.sendPacket(viewer, playerInfoUpdate)

            data.npc.addViewer(viewer.uniqueId)
            val equipPacket = WrapperPlayServerEntityEquipment(data.npc.entityId, data.equipment)
            PacketEvents.getAPI().playerManager.sendPacket(viewer, equipPacket)
        }
    }

    private fun collectOwnerTextures(owner: Player): List<TextureProperty> {
        val craftPlayer = owner as CraftPlayer
        val gameProfile = craftPlayer.profile
        return gameProfile.properties.get("textures").map {
            TextureProperty("textures", it.value, it.signature ?: "")
        }
    }

    fun updateNpcGlowing(player: Player, isGlowing: Boolean = true) {
        val data = layingMap[player] ?: return
        val playerMeta = data.npc.entityMeta as? PlayerMeta ?: return
        if (playerMeta.isGlowing != isGlowing) {
            playerMeta.isGlowing = isGlowing
        }
    }

    fun updateMainHandEquipment(player: Player) {
        layingMap[player]?.let { data ->
            val state = hooker.playerStateManager.savedItems[player.uniqueId] ?: return
            val currentItem = state.hotbarItems[state.currentHotbarSlot]?.clone() ?: ItemStack(Material.AIR)
            val packetItem = SpigotConversionUtil.fromBukkitItemStack(currentItem)
            val newEquipment = data.equipment.filter { it.slot != EquipmentSlot.MAIN_HAND }.toMutableList()
            newEquipment.add(Equipment(EquipmentSlot.MAIN_HAND, packetItem))
            hooker.entityManager.updatePlayerNPCEquipment(data.npc, newEquipment, player)
            layingMap[player] = data.copy(equipment = newEquipment)
        }
    }
}
