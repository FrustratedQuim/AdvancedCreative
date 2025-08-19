package com.ratger.acreative.commands.lay

import com.ratger.acreative.core.FunctionHooker
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.roundToLong
import org.bukkit.block.Block
import org.bukkit.block.data.type.Bed
import org.bukkit.util.Vector

class LayManager(private val hooker: FunctionHooker) {

    companion object {
        private const val ARMORSTAND_REATTACH_DELAY_TICKS = 5L
        private const val CHECK_TASK_PERIOD_TICKS = 10L
        private const val INTERACT_DELAY_MS = 500L
        private const val MAX_INTERACT_DISTANCE = 3.0
    }

    val layingMap: MutableMap<Player, LayData> = mutableMapOf()
    private val lastInteract: MutableMap<UUID, Long> = mutableMapOf()

    data class LayData(val npc: Any, val armorStandId: UUID, val bedY: Double?)

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
        layPlayerAt(player, location, yaw)
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
                val yaw = when {
                    "facing=north" in headBlock.blockData.toString() -> 0f
                    "facing=south" in headBlock.blockData.toString() -> 180f
                    "facing=west" in headBlock.blockData.toString() -> -90f
                    "facing=east" in headBlock.blockData.toString() -> 90f
                    else -> player.location.yaw
                }
                layPlayerAt(player, location, yaw, headBlock.location.y)
            } else {
                val location = block.location.clone().add(0.5, 0.525, 0.5)
                val yaw = player.location.yaw
                hooker.sitManager.sitPlayerAt(player, location, yaw, "bed", block)
            }
            return true
        } else {
            hooker.messageManager.sendMiniMessage(player, key = "error-lay-in-air")
            return false
        }
    }

    fun layPlayerAt(player: Player, baseLocation: Location, yaw: Float, bedY: Double? = null) {
        if (hooker.utils.isDisguised(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-cannot-disguised")
            return
        }
        if (layingMap.containsKey(player)) return

        val world = player.location.world
        val baseX = baseLocation.x
        val baseY = baseLocation.y
        val baseZ = baseLocation.z

        for ((otherPlayer, _) in layingMap) {
            if (otherPlayer == player || otherPlayer.location.world != world) continue
            val loc = otherPlayer.location
            if (kotlin.math.abs(baseX - loc.x) < 0.8 &&
                kotlin.math.abs(baseY - loc.y) < 0.8 &&
                kotlin.math.abs(baseZ - loc.z) < 0.8
            ) {
                return
            }
        }

        val location = baseLocation.clone().apply {
            x = blockX + 0.5
            z = blockZ + 0.5
            if (hooker.utils.isSitting(player)) y += 0.55
        }

        if (!canLay(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-lay-in-air")
            return
        }

        val roundedYaw = ((yaw / 90.0).roundToLong() * 90) % 360
        val (npcYaw, offsetX, offsetZ) = when (roundedYaw.toInt()) {
            0 -> Triple(-90f, 0.0, 1.45)
            90 -> Triple(180f, -1.45, 0.0)
            180, -180 -> Triple(90f, 0.0, -1.45)
            -90 -> Triple(0f, 1.45, 0.0)
            else -> Triple(-90f, 0.0, 1.45)
        }

        val npcLocation = location.clone().add(offsetX, 0.12, offsetZ).apply {
            pitch = 0f
        }
        val npc = hooker.entityManager.createNpc(player, npcLocation, npcYaw)
        if (hooker.utils.isGlowing(player)) {
            player.isGlowing = false
            npc.data.isGlowing = true
            npc.updateForAll()
        }

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

        layingMap[player] = LayData(npc, armorStand.uniqueId, bedY)

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

        layingMap.remove(player)?.let { (npc, armorStandId, bedY) ->

            (npc as? Npc)?.let {
                FancyNpcsPlugin.get().npcManager.removeNpc(it)
                it.removeForAll()
            }

            player.world.getEntity(armorStandId)?.remove()
            if (bedY != null) {
                val targetLocation = player.location.clone().apply {
                    y = bedY + 0.5625
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

        if (hooker.utils.isGlowing(player)) player.isGlowing = true
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
        val npc = data.npc as? Npc ?: return
        if (npc.data.isGlowing != isGlowing) {
            npc.data.setGlowing(isGlowing)
        }
        npc.updateForAll()
    }
}