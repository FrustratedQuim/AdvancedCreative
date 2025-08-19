package com.ratger.acreative.commands.freeze

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt
import kotlin.random.Random

class FreezeManager(private val hooker: FunctionHooker) {

    val frozenPlayers = ConcurrentHashMap<Player, MutableList<BlockDisplay>>()
    private val freezeTaskIds = ConcurrentHashMap<Player, Int>()
    val hiddenFreezeBlocks = ConcurrentHashMap<UUID, MutableMap<UUID, MutableList<BlockDisplay>>>()

    fun prepareToFreezePlayer(initiator: Player, targetName: String?) {
        if (targetName == null || !initiator.hasPermission("advancedcreative.freeze.other")) {
            if (!hooker.utils.checkAndRemovePose(initiator)) {
                return
            }
            freezePlayer(initiator, initiator)
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendMiniMessage(initiator, key = "error-unknown-player")
            return
        }

        if (!hooker.utils.checkAndRemovePose(target)) {
            return
        }
        freezePlayer(target, initiator)
    }

    fun freezePlayer(player: Player, initiator: Player? = null) {
        if (hooker.utils.isDisguised(player)) {
            return
        }
        if (frozenPlayers.containsKey(player)) {
            unfreezePlayer(player)
            return
        }
        player.freezeTicks = player.maxFreezeTicks
        val blocks = spawnFreezeBlocks(player)
        frozenPlayers[player] = blocks

        if (hooker.utils.isGlowing(player)) {
            blocks.forEach { it.isGlowing = true }
        }

        for (hider in Bukkit.getOnlinePlayers()) {
            if (hider != player && hooker.utils.isHiddenFromPlayer(hider, player)) {
                val hiddenBlocks = hiddenFreezeBlocks.computeIfAbsent(hider.uniqueId) { ConcurrentHashMap() }
                hiddenBlocks[player.uniqueId] = blocks
                blocks.forEach { hider.hideEntity(hooker.plugin, it) }
            }
        }

        val taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(hooker.plugin, {
            if (!frozenPlayers.containsKey(player) || !player.isOnline) {
                unfreezePlayer(player)
                return@scheduleSyncRepeatingTask
            }
            player.freezeTicks = player.maxFreezeTicks
        }, 0L, 20L)
        freezeTaskIds[player] = taskId

        if (initiator == null || initiator == player) {
            hooker.messageManager.sendMiniMessage(player, key = "success-freeze-self")
        } else {
            hooker.messageManager.sendMiniMessage(
                initiator,
                key = "success-freeze",
                variables = mapOf("target" to player.name)
            )
        }
    }

    fun unfreezePlayer(player: Player) {
        frozenPlayers[player]?.forEach {
            it.isGlowing = false
            it.remove()
        }
        frozenPlayers.remove(player)
        player.freezeTicks = 0

        for (hider in Bukkit.getOnlinePlayers()) {
            val hiddenBlocks = hiddenFreezeBlocks[hider.uniqueId]
            if (hiddenBlocks != null && hiddenBlocks.containsKey(player.uniqueId)) {
                if (!hooker.utils.isHiddenFromPlayer(hider, player)) {
                    hiddenBlocks[player.uniqueId]?.forEach { block ->
                        if (block.isValid) {
                            hider.showEntity(hooker.plugin, block)
                        }
                    }
                }
                hiddenBlocks.remove(player.uniqueId)
                if (hiddenBlocks.isEmpty()) {
                    hiddenFreezeBlocks.remove(hider.uniqueId)
                }
            }
        }

        freezeTaskIds[player]?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
            freezeTaskIds.remove(player)
        }
    }

    private fun spawnFreezeBlocks(player: Player): MutableList<BlockDisplay> {
        val location = player.location
        val blocks = mutableListOf<BlockDisplay>()

        for (viewer in location.world?.players?.filter { it.isOnline } ?: emptyList()) {
            if (viewer != player && hooker.utils.isHiddenFromPlayer(viewer, player)) {
                continue
            }
            viewer.playSound(
                location,
                org.bukkit.Sound.BLOCK_ANVIL_LAND,
                1f,
                Random.nextDouble(0.8, 1.2).toFloat()
            )
        }

        val scale = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE)?.value ?: 1.0
        val blockCount = 4
        val data = Material.ICE.createBlockData()

        val hitboxRadius = 0.05 * scale
        val centerBias = -0.1 * scale
        val minY = 0.2 * scale
        val maxY = 1.8 * scale
        val availableHeight = maxY - minY
        val sectorHeight = availableHeight / blockCount

        val allCombos = listOf(
            Pair(1, 1),
            Pair(1, -1),
            Pair(-1, -1),
            Pair(-1, 1)
        ).shuffled()
        val possibleCombos = allCombos.take(blockCount).shuffled()

        val usedOffsets = mutableListOf<Pair<Double, Double>>()

        val baseDirection = if (player.isGliding) {
            player.location.direction.normalize()
        } else {
            org.bukkit.util.Vector(0.0, 1.0, 0.0)
        }

        repeat(blockCount) { i ->
            val baseDist = minY + i * sectorHeight
            val offsetAlongDir = baseDist + Random.nextDouble(-0.1, 0.1) * scale

            val combo = possibleCombos[i]
            var offsetX: Double
            var offsetZ: Double
            var attempts = 0

            do {
                offsetX = combo.first * Random.nextDouble(0.0, hitboxRadius)
                offsetZ = combo.second * Random.nextDouble(0.0, hitboxRadius)
                attempts++
            } while (
                attempts < 20 &&
                usedOffsets.any { (ux, uz) ->
                    val dx = offsetX - ux
                    val dz = offsetZ - uz
                    sqrt(dx * dx + dz * dz) < 0.15 * scale
                }
            )

            usedOffsets.add(offsetX to offsetZ)

            val blockLoc = location.clone()
                .add(baseDirection.clone().multiply(offsetAlongDir + centerBias))
                .add(offsetX, 0.0, offsetZ)

            val yaw = Random.nextFloat() * 360f
            val pitch = Random.nextFloat() * 180f - 90f
            val size = (Random.nextDouble(0.5, 0.6) * scale).toFloat()

            val display = blockLoc.world.spawn(blockLoc, BlockDisplay::class.java) { d ->
                d.block = data
                d.transformation = Transformation(
                    Vector3f(0f, 0f, 0f),
                    Quaternionf().rotateXYZ(
                        Math.toRadians(pitch.toDouble()).toFloat(),
                        Math.toRadians(yaw.toDouble()).toFloat(),
                        0f
                    ),
                    Vector3f(size, size, size),
                    Quaternionf()
                )
            }
            blocks.add(display)
        }
        return blocks
    }

    fun updateIceGlowing(player: Player, isGlowing: Boolean) {
        frozenPlayers[player]?.forEach { block ->
            block.isGlowing = isGlowing && !hiddenFreezeBlocks.any { it.value.containsKey(player.uniqueId) }
        }
    }
}