package com.ratger.acreative.commands.lay

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove
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
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
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
        if (hooker.utils.isDisguised(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-cannot-disguised")
            return
        }
        if (hooker.utils.isPissing(player)) {
            hooker.pissManager.stopPiss(player)
        }
        if (!hooker.utils.checkAndRemovePose(player)) {
            return
        }
        if (!canLay(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-lay-in-air")
            return
        }
        val location = player.location.clone()
        val yaw = player.location.yaw
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
        if (layingMap.containsKey(player)) {
            return true
        }
        if (player.isSneaking ||
            player.location.distance(block.location) > MAX_INTERACT_DISTANCE) {
            return false
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - (lastInteract[player.uniqueId] ?: 0) < INTERACT_DELAY_MS) {
            return false
        }
        if (!block.type.name.contains("BED") || !player.world.isDayTime) {
            return false
        }

        hooker.utils.checkGlideUnglide(player)
        lastInteract[player.uniqueId] = currentTime

        if (canLay(player)) {
            hooker.utils.checkAndRemovePose(player)
            val headBlock = findBedHeadBlock(block)
            if (headBlock != null) {
                val location = headBlock.location.clone().add(0.5, 0.5625, 0.5)
                val bedData = headBlock.blockData as? Bed
                val yaw = when (bedData?.facing?.name) {
                    "NORTH" -> -1f
                    "SOUTH" -> 180f
                    "WEST" -> -90f
                    "EAST" -> 90f
                    else -> player.location.yaw
                }
                layPlayerAt(player, location, yaw, headBlock.location)
                return true
            }
        }
        return false
    }

    fun layPlayerAt(player: Player, location: Location, yaw: Float, bedLocation: Location?) {
        val yFloat = ((yaw + 180) % 360)
        var y = yFloat.toInt() - 180
        y = when {
            y in -45..45 -> 0
            y in 46..135 -> 90
            y in -135..-46 -> -90
            y > 135 -> 180
            else -> -180
        }
        val (npcYaw, offsetX, offsetZ) = when (y) {
            0 -> Triple(-90f, 0.0, 1.45)
            90 -> Triple(180f, -1.45, 0.0)
            180, -180 -> Triple(90f, 0.0, -1.45)
            -90 -> Triple(0f, 1.45, 0.0)
            else -> Triple(-90f, 0.0, 1.45)
        }

        val npcLocation = location.clone().add(offsetX, 0.12, offsetZ).apply {
            this.yaw = npcYaw
            pitch = 0f
        }

        for ((otherPlayer, data) in layingMap) {
            if (otherPlayer == player) continue
            val otherNpcLocation = data.npc.location
            if (otherPlayer.world == player.world &&
                abs(otherNpcLocation.x - npcLocation.x) <= 0.5 &&
                abs(otherNpcLocation.y - npcLocation.y) <= 0.5 &&
                abs(otherNpcLocation.z - npcLocation.z) <= 0.5) {
                return
            }
        }

        val (entity, equipment, teamName) = hooker.entityManager.createPlayerNPC(player, npcLocation, npcYaw, hooker.utils.isGlowing(player))

        val armorStandLocation = location.clone().add(0.0, 0.3, 0.0)
        val armorStand = hooker.entityManager.createArmorStand(armorStandLocation, npcYaw).apply {
            customName(getDisplayName(player))
            isCustomNameVisible = true
        }

        if (!hooker.utils.isSitting(player)) {
            armorStand.addPassenger(player)
        }
        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            if (!armorStand.passengers.contains(player) && hooker.utils.isSitting(player)) {
                armorStand.addPassenger(player)
            }
        }, ARMORSTAND_REATTACH_DELAY_TICKS)

        val headRotationTask = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || !entity.isSpawned) {
                    cancel()
                    return
                }

                fun normalizeYawDiff(diff: Int): Int {
                    var d = (diff + 540) % 360 - 180
                    if (d > 90) d = 90
                    if (d < -90) d = -90
                    return d
                }

                val finalYaw = when (y) {
                    -90, 180, -180, 90, 0 -> npcYaw + normalizeYawDiff(y - player.location.yaw.toInt()) * -1
                    else -> 0f
                }

                val headLookPacket = WrapperPlayServerEntityHeadLook(entity.entityId, finalYaw)
                Bukkit.getOnlinePlayers().forEach { viewer ->
                    if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                        PacketEvents.getAPI().playerManager.sendPacket(viewer, headLookPacket)
                    }
                }
            }
        }
        val taskId = headRotationTask.runTaskTimer(hooker.plugin, 0L, 2L).taskId

        layingMap[player] = LayData(entity, taskId, armorStand.uniqueId, bedLocation, equipment, y.toFloat(), teamName)

        hooker.playerStateManager.savePlayerInventory(player)
        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.setItemInOffHand(null)
        player.inventory.setItem(player.inventory.heldItemSlot, null)

        player.isInvisible = true
        player.isInvulnerable = true
        player.isCollidable = false
        player.isSilent = true

        hooker.messageManager.sendMiniMessage(player, "ACTION", "action-pose-unset", repeatable = true)
    }

    fun unlayPlayer(player: Player, shouldUnsit: Boolean = true) {
        if (shouldUnsit && hooker.utils.isSitting(player)) {
            hooker.sitManager.unsitPlayer(player)
        }

        layingMap.remove(player)?.let { data ->
            val scoreboard = Scoreboard()
            val team = PlayerTeam(scoreboard, data.teamName)
            val removePacket = ClientboundSetPlayerTeamPacket.createRemovePacket(team)
            Bukkit.getOnlinePlayers().forEach { viewer ->
                if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                    (viewer as CraftPlayer).handle.connection.send(removePacket)
                }
            }

            Bukkit.getScheduler().cancelTask(data.headRotationTaskId)

            val playerInfoRemove = WrapperPlayServerPlayerInfoRemove(listOf(data.npc.uuid))
            Bukkit.getOnlinePlayers().forEach { viewer ->
                if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                    PacketEvents.getAPI().playerManager.sendPacket(viewer, playerInfoRemove)
                }
            }
            data.npc.remove()

            player.world.getEntity(data.armorStandId)?.remove()

            if (data.bedLocation != null) {
                val targetLocation = data.bedLocation.clone().apply {
                    y += 0.5625
                }
                player.teleport(targetLocation)
            }

            hooker.playerStateManager.restorePlayerInventory(player)
            player.isInvisible = false
            player.isInvulnerable = false
            player.isCollidable = true
            player.isSilent = false
            hooker.messageManager.sendMiniMessage(player, "ACTION_STOP")
            hooker.playerStateManager.refreshPlayerPose(player)
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
        Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            val playersToUnlay = mutableListOf<Player>()
            for ((player, data) in layingMap) {
                val entity = player.world.getEntity(data.armorStandId)
                if (entity == null || !entity.isValid) {
                    playersToUnlay.add(player)
                }
            }
            playersToUnlay.forEach { unlayPlayer(it) }
        }, CHECK_TASK_PERIOD_TICKS, CHECK_TASK_PERIOD_TICKS)
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