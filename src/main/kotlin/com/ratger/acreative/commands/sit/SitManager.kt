package com.ratger.acreative.commands.sit

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.atan2

class SitManager(private val hooker: FunctionHooker) {

    companion object {
        private const val INTERACT_DELAY_MS = 500L
        private const val ARMORSTAND_REATTACH_DELAY_TICKS = 5L
        private const val CHECK_TASK_PERIOD_TICKS = 10L
        private const val MAX_INTERACT_DISTANCE = 3.0
        private const val EPSILON = 0.01
    }

    val sittingMap: MutableMap<Player, SitData> = mutableMapOf()
    private val lastInteract: MutableMap<UUID, Long> = mutableMapOf()

    data class SitData(val armorStandId: UUID, val block: Block?, val style: String)

    fun canSit(player: Player): Boolean {
        val blockBelow = player.location.clone().add(0.0, -1.0, 0.0).block
        return player.gameMode != GameMode.SPECTATOR &&
                !player.isFlying &&
                blockBelow.type.isSolid
    }

    fun sitPlayer(player: Player) {
        if (hooker.utils.isDisguised(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-cannot-disguised")
            return
        }
        if (!hooker.utils.checkAndRemovePose(player)) {
            return
        }
        if (!canSit(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-sit-in-air")
            return
        }
        val location = player.location.clone()
        val yaw = player.location.yaw
        sitPlayerAt(player, location, yaw, "basic")
    }

    fun sitPlayerAt(player: Player, location: Location, yaw: Float, style: String = "basic", block: Block? = null) {
        if (hooker.utils.isDisguised(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-cannot-disguised")
            return
        }
        val targetLocation = location.clone().apply { this.yaw = yaw }
        val armorStand = hooker.entityManager.createArmorStand(targetLocation, yaw)
        hooker.messageManager.sendMiniMessage(player, "ACTION", "action-pose-unset", repeatable = true)
        sittingMap[player] = SitData(armorStand.uniqueId, block, style)
        armorStand.addPassenger(player)
        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            if (!armorStand.passengers.contains(player) && hooker.utils.isSitting(player)) {
                armorStand.addPassenger(player)
            }
        }, ARMORSTAND_REATTACH_DELAY_TICKS)
    }

    fun unsitPlayer(player: Player) {
        val sitData = sittingMap[player] ?: return
        val block = sitData.block
        var targetY = player.location.y
        if (block != null) {
            targetY = determineStyleAndHeight(block, block.location.y).second
        } else if (sitData.style == "basic") {
            val checkY = player.location.y + 0.2
            val checkBlock = player.world.getBlockAt(
                player.location.blockX, checkY.toInt(), player.location.blockZ
            )
            targetY = determineStyleAndHeight(checkBlock, checkY.toInt().toDouble()).second
        }
        player.world.getEntity(sitData.armorStandId)?.remove()
        sittingMap.remove(player)
        player.teleport(player.location.clone().apply { y = targetY })
        hooker.messageManager.sendMiniMessage(player, "ACTION_STOP")
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    private fun determineStyleAndHeight(block: Block?, baseY: Double): Pair<String, Double> {
        if (block == null) return "basic" to (baseY + 1.0)
        val blockType = block.type.name
        val blockData = block.blockData.toString()
        return when {
            "SLAB" in blockType -> {
                when {
                    "type=top" in blockData -> "slab" to (block.location.y + 1.0)
                    "type=double" in blockData -> "slab" to (block.location.y + 1.0)
                    else -> "slab" to (block.location.y + 0.5)
                }
            }
            "CARPET" in blockType -> "carpet" to (block.location.y + 0.0625)
            "TRAPDOOR" in blockType && "half=bottom" in blockData -> "trapdoor_bottom" to (block.location.y + 0.1875)
            "CAKE" in blockType -> "cake" to (block.location.y + 0.5)
            "FENCE" in blockType -> "fence" to (block.location.y + 0.5)
            "LANTERN" in blockType -> "lantern" to (block.location.y + 0.5625)
            "STONECUTTER" in blockType -> "stonecutter" to (block.location.y + 0.5625)
            "HEAD" in blockType || "SKULL" in blockType -> "head_skull" to (block.location.y + 0.5)
            "FLOWER_POT" in blockType -> "flower_pot" to (block.location.y + 0.375)
            "CANDLE" in blockType -> "candle" to (block.location.y + 0.375)
            "BED" in blockType -> "bed" to (block.location.y + 0.5625)
            else -> "basic" to (block.location.y + 1.0)
        }
    }

    private fun isLocationOccupied(location: Location, excluding: Player): Boolean {
        for ((otherPlayer, sitData) in sittingMap) {
            if (otherPlayer == excluding) continue
            val stand = excluding.world.getEntity(sitData.armorStandId) ?: continue
            if (stand.location.distanceSquared(location) < EPSILON) return true
        }
        return false
    }

    fun handleRightClickBlock(player: Player, block: Block): Boolean {
        val sitData = sittingMap[player]
        if (sitData != null && sitData.block == block) {
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

        hooker.utils.checkGlideUnglide(player)

        lastInteract[player.uniqueId] = currentTime
        if (block.type.name.contains("STAIRS")) {
            if ("half=top" in block.blockData.toString()) return false
            sitOnStairs(player, block)
            return true
        } else if (block.type.name.contains("SLAB")) {
            if ("type=top" in block.blockData.toString() || "type=double" in block.blockData.toString()) return false
            sitOnSlab(player, block)
            return true
        }
        return false
    }

    fun handleBlockBreak(block: Block) {
        sittingMap.filterValues { it.block == block }
            .keys
            .toList()
            .forEach { unsitPlayer(it) }
    }

    private fun sitOnStairs(player: Player, block: Block) {
        val (yaw, offsetX, offsetZ) = when {
            "facing=north" in block.blockData.toString() -> Triple(0f, 0.0, 0.13)
            "facing=south" in block.blockData.toString() -> Triple(180f, 0.0, -0.13)
            "facing=west" in block.blockData.toString() -> Triple(-90f, 0.13, 0.0)
            "facing=east" in block.blockData.toString() -> Triple(90f, -0.13, 0.0)
            else -> Triple(player.location.yaw, 0.0, 0.0)
        }
        val targetLocation = block.location.clone().add(0.5 + offsetX, 0.55, 0.5 + offsetZ)
        if (isLocationOccupied(targetLocation, player)) return
        sitPlayerAt(player, targetLocation, yaw, "stairs", block)
    }

    fun sitOnSlab(player: Player, block: Block) {
        val baseLocation = block.location.clone().add(0.5, 0.55, 0.5)
        val angleByIndex = listOf(145f, 45f, -45f, -145f)
        val slabOffsets = listOf(
            Vector(-0.3, 0.0, -0.3),
            Vector(-0.3, 0.0, 0.3),
            Vector(0.3, 0.0, 0.3),
            Vector(0.3, 0.0, -0.3)
        )
        val centerLocation = baseLocation.clone()
        val clickOffset = Vector(
            player.location.x - (block.location.x + 0.5),
            0.0,
            player.location.z - (block.location.z + 0.5)
        )
        var closestIndex = 0
        var minDistance = Double.MAX_VALUE
        for ((i, offset) in slabOffsets.withIndex()) {
            val dist = offset.distanceSquared(clickOffset)
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }
        val sittingHere = sittingMap.filterValues {
            val stand = player.world.getEntity(it.armorStandId)
            stand?.location?.block?.location == block.location
        }
        val occupiedIndices = sittingHere.values.mapNotNull { sitData ->
            val stand = player.world.getEntity(sitData.armorStandId) ?: return@mapNotNull null
            val standOffset = stand.location.clone().subtract(baseLocation)
            slabOffsets.indexOfFirst { it.distanceSquared(standOffset.toVector()) < EPSILON }
        }.toMutableSet()
        val isCenterOccupied = sittingHere.values.any {
            val stand = player.world.getEntity(it.armorStandId) ?: return@any false
            stand.location.distanceSquared(centerLocation) < EPSILON
        }
        sittingMap[player]?.let { sitData ->
            player.world.getEntity(sitData.armorStandId)?.remove()
            sittingMap.remove(player)
        }
        if (sittingHere.isEmpty()) {
            val yaw = Math.toDegrees(atan2(-(player.location.direction.x), player.location.direction.z)).toFloat()
            sitPlayerAt(player, centerLocation, yaw, "slab", block)
            return
        }
        var targetIndexB = closestIndex
        var attempts = 0
        while (targetIndexB in occupiedIndices && attempts < 4) {
            targetIndexB = (targetIndexB + 1) % 4
            attempts++
        }
        if (attempts >= 4) return
        val targetLocationB = baseLocation.clone().add(slabOffsets[targetIndexB])
        sitPlayerAt(player, targetLocationB, angleByIndex[targetIndexB], "slab", block)
        occupiedIndices.add(targetIndexB)
        if (isCenterOccupied) {
            val centerPlayerEntry = sittingHere.entries.firstOrNull { entry ->
                val stand = player.world.getEntity(entry.value.armorStandId)
                stand?.location?.distanceSquared(centerLocation)!! < EPSILON
            }
            if (centerPlayerEntry != null) {
                val freeIndex = (0 until 4).firstOrNull { it !in occupiedIndices }
                if (freeIndex != null) {
                    val centerPlayer = centerPlayerEntry.key
                    unsitPlayer(centerPlayer)
                    val newLocation = baseLocation.clone().add(slabOffsets[freeIndex])
                    sitPlayerAt(centerPlayer, newLocation, angleByIndex[freeIndex], "slab", block)
                    occupiedIndices.add(freeIndex)
                }
            }
        }
    }

    fun startArmorStandChecker() {
        Bukkit.getScheduler().runTaskTimer(hooker.plugin, Runnable {
            val playersToUnsit = mutableListOf<Player>()
            for ((player, data) in sittingMap) {
                val entity = player.world.getEntity(data.armorStandId)
                if (entity == null || !entity.isValid) {
                    playersToUnsit.add(player)
                }
            }
            playersToUnsit.forEach { unsitPlayer(it) }
        }, CHECK_TASK_PERIOD_TICKS, CHECK_TASK_PERIOD_TICKS)
    }
}