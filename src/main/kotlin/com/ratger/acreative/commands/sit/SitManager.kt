package com.ratger.acreative.commands.sit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.MessageChannel
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.util.Vector
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.Stairs
import org.bukkit.block.data.Bisected
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
                !player.isInsideVehicle &&
                blockBelow.type.isSolid
    }

    fun sitPlayer(player: Player) {
        if (!canSit(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_SIT_IN_AIR)
            return
        }
        val location = player.location.clone()
        val yaw = player.location.yaw
        sitPlayerAt(player, location, yaw, "basic")
    }

    fun sitPlayerAt(player: Player, location: Location, yaw: Float, style: String = "basic", block: Block? = null) {
        hooker.playerStateManager.activateState(player, PlayerStateType.SITTING)
        val targetLocation = location.clone().apply { this.yaw = yaw }
        val armorStand = hooker.entityManager.createArmorStand(targetLocation, yaw)
        hooker.messageManager.startRepeatingActionBar(player, MessageKey.ACTION_POSE_UNSET)
        sittingMap[player] = SitData(armorStand.uniqueId, block, style)
        armorStand.addPassenger(player)
        hooker.tickScheduler.runLater(ARMORSTAND_REATTACH_DELAY_TICKS) {
            if (!armorStand.passengers.contains(player) && hooker.utils.isSitting(player)) {
                armorStand.addPassenger(player)
            } else if (!armorStand.passengers.contains(player)) {
                armorStand.remove()
                sittingMap.remove(player)
            }
        }
    }

    fun sitOnHead(player: Player, target: Player?, sender: Player? = null) {

        val currentTime = System.currentTimeMillis()
        if (currentTime - (lastInteract[player.uniqueId] ?: 0L) < INTERACT_DELAY_MS) return
        lastInteract[player.uniqueId] = currentTime

        var finalTarget = target ?: return
        var currentTarget: Player? = target
        val checkedPlayers = mutableSetOf<Player>()
        val maxDepth = 10
        var depth = 0
        while (currentTarget != null && depth < maxDepth) {
            if (currentTarget in checkedPlayers) return
            checkedPlayers.add(currentTarget)
            if (currentTarget == player) return
            finalTarget = currentTarget
            currentTarget = getHeadPassenger(currentTarget)
            depth++
        }

        if (hooker.utils.isHiddenFromPlayer(finalTarget, player)) {
            hooker.messageManager.sendChat(player, MessageKey.SITHEAD_HIDDEN_SELF)
            return
        }

        var baseTarget: Player? = finalTarget
        val baseCheckedPlayers = mutableSetOf<Player>()
        depth = 0
        do {
            if (baseTarget == null) break
            if (baseTarget in baseCheckedPlayers) return
            baseCheckedPlayers.add(baseTarget)
            if (baseTarget == player) return
            if (hooker.utils.isHiddenFromPlayer(baseTarget, player)) {
                hooker.messageManager.sendChat(sender ?: player, MessageKey.SITHEAD_HIDDEN_BY_ONE)
                return
            }
            val sitData = sittingMap[baseTarget]
            if (sitData == null || sitData.style != "head") {
                break
            }
            val baseStand = baseTarget.world.getEntity(sitData.armorStandId) ?: break
            baseTarget = baseStand.vehicle as? Player ?: break
            depth++
            if (depth >= maxDepth) break
        } while (true)

        currentTarget = target
        checkedPlayers.clear()
        depth = 0
        while (currentTarget != null && depth < maxDepth) {
            if (currentTarget in checkedPlayers) return
            checkedPlayers.add(currentTarget)
            if (hooker.utils.isHiddenFromPlayer(player, currentTarget)) {
                hooker.messageManager.sendChat(player, MessageKey.SITHEAD_YOU_HIDE_ONE)
                return
            }
            currentTarget = getHeadPassenger(currentTarget)
            depth++
        }

        baseTarget = finalTarget
        checkedPlayers.clear()
        depth = 0
        do {
            if (baseTarget == null) break
            if (baseTarget in checkedPlayers) return
            checkedPlayers.add(baseTarget)
            if (hooker.utils.isHiddenFromPlayer(player, baseTarget)) {
                hooker.messageManager.sendChat(player, MessageKey.SITHEAD_YOU_HIDE_ONE)
                return
            }
            val sitData = sittingMap[baseTarget]
            if (sitData == null || sitData.style != "head") {
                break
            }
            val baseStand = baseTarget.world.getEntity(sitData.armorStandId) ?: break
            baseTarget = baseStand.vehicle as? Player ?: break
            depth++
            if (depth >= maxDepth) break
        } while (true)

        val location = finalTarget.location.clone().apply {
            y += 1.8
            pitch = 0f
        }
        val armorStand = hooker.entityManager.createArmorStand(location, finalTarget.location.yaw)

        sittingMap[player] = SitData(armorStand.uniqueId, null, "head")
        armorStand.addPassenger(player)
        finalTarget.addPassenger(armorStand)
        hooker.messageManager.startRepeatingActionBar(player, MessageKey.ACTION_POSE_UNSET)

        hooker.tickScheduler.runLater(ARMORSTAND_REATTACH_DELAY_TICKS) {
            if (!armorStand.passengers.contains(player) && hooker.utils.isSitting(player)) {
                armorStand.addPassenger(player)
            } else if (!armorStand.passengers.contains(player)) {
                armorStand.remove()
                sittingMap.remove(player)
            }
            if (!finalTarget.passengers.contains(armorStand)) {
                finalTarget.addPassenger(armorStand)
            }
        }

    }

    fun getHeadPassenger(player: Player): Player? {
        val armorStand = player.passengers.firstOrNull { it is org.bukkit.entity.ArmorStand } as? org.bukkit.entity.ArmorStand
        if (armorStand == null) return null

        val passenger = armorStand.passengers.firstOrNull { it is Player } as? Player
        if (
            passenger != null &&
            sittingMap[passenger]?.style == "head" &&
            sittingMap[passenger]?.armorStandId == armorStand.uniqueId
            ) {
            return passenger
        }
        return null
    }

    fun launchHeadPassenger(player: Player) {
        val passenger = getHeadPassenger(player) ?: return
        val direction = player.location.direction.clone().normalize()
        direction.y = direction.y + 0.5
        direction.multiply(1.0)

        unsitPlayer(passenger, saveHeadPassenger = true)
        hooker.tickScheduler.runLater(1L) {
            if (passenger.isOnline) passenger.velocity = direction
        }
    }

    fun unsitPlayer(player: Player, saveHeadPassenger: Boolean = false) {
        val sitData = sittingMap[player] ?: run {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.SITTING)
            return
        }
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.getOrNull(2)?.let { "${it.className}.${it.methodName}:${it.lineNumber}" } ?: "unknown"

        val armorStand = player.world.getEntity(sitData.armorStandId)
        if (armorStand == null) {
            sittingMap.remove(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.SITTING)
            return
        }

        val location = player.location.clone()
        if (sitData.style == "head") {
            location.y = location.y - 0.2
            if (armorStand.vehicle != null) {
                location.y = armorStand.vehicle?.location?.y ?: location.y
            }
            val basePlayer = armorStand.vehicle as? Player
            basePlayer?.removePassenger(armorStand)
        }

        if (!saveHeadPassenger) {
            val headPassenger = getHeadPassenger(player)
            if (headPassenger != null) unsitPlayer(headPassenger)
        }

        armorStand.removePassenger(player)
        armorStand.remove()
        player.teleport(location)
        sittingMap.remove(player)

        if (!caller.contains("HideManager.hidePlayer") && !caller.contains("SitManager.sitOnHead")) {
            hooker.messageManager.stopRepeating(player, MessageChannel.ACTION_BAR)
        }

        hooker.playerStateManager.deactivateState(player, PlayerStateType.SITTING)
        if (player.isOnline && hooker.plugin.isEnabled) hooker.playerStateManager.refreshPlayerPose(player)
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

        lastInteract[player.uniqueId] = currentTime
        when (val data = block.blockData) {
            is Stairs -> {
                if (data.half == Bisected.Half.TOP) return false
                sitOnStairs(player, block)
                return true
            }
            is Slab -> {
                if (data.type != Slab.Type.BOTTOM) return false
                sitOnSlab(player, block)
                return true
            }
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
        val stairs = block.blockData as? Stairs
        val (yaw, offsetX, offsetZ) = when (stairs?.facing) {
            BlockFace.NORTH -> Triple(0f, 0.0, 0.13)
            BlockFace.SOUTH -> Triple(180f, 0.0, -0.13)
            BlockFace.WEST -> Triple(-90f, 0.13, 0.0)
            BlockFace.EAST -> Triple(90f, -0.13, 0.0)
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
            val stand = player.world.getEntity(it.armorStandId) ?: return@filterValues false
            stand.location.block.location == block.location
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
        hooker.tickScheduler.runRepeating(CHECK_TASK_PERIOD_TICKS, CHECK_TASK_PERIOD_TICKS) {
            val playersToUnsit = mutableListOf<Player>()
            for ((player, data) in sittingMap) {
                val entity = player.world.getEntity(data.armorStandId)
                if (entity == null || !entity.isValid) {
                    playersToUnsit.add(player)
                }
            }
            playersToUnsit.forEach { unsitPlayer(it) }
        }
    }
}
