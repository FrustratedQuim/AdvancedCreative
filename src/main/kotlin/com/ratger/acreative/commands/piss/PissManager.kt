package com.ratger.acreative.commands.piss

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class ScorePoint(
    val location: Location,
    var score: Int,
    var display: BlockDisplay?,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    val creator: UUID? = null
)

class PissManager(private val hooker: FunctionHooker) {

    private val activeStreams = mutableListOf<MutableList<BlockDisplay>>()
    val scorePoints = mutableListOf<ScorePoint>()
    val pissingPlayers = mutableMapOf<Player, BukkitRunnable>()
    val hiddenPuddleDisplays = ConcurrentHashMap<UUID, MutableMap<UUID, MutableList<BlockDisplay>>>()

    fun pissPlayer(player: Player) {
        if (hooker.utils.isLaying(player)) {
            hooker.messageManager.sendMiniMessage(player, key = "error-cannot-lay")
            return
        }
        val scale = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE)?.baseValue ?: 1.0
        if (scale < 1.0) {
            hooker.messageManager.sendMiniMessage(player, key = "error-too-small")
            return
        }
        if (scale > 1.0) {
            hooker.messageManager.sendMiniMessage(player, key = "too-large")
            return
        }
        if (pissingPlayers.containsKey(player)) {
            stopPiss(player)
            return
        }
        val streamTask = object : BukkitRunnable() {
            override fun run() {
                spawnStream(player)
            }
        }
        streamTask.runTaskTimer(hooker.plugin, 0L, 3L)
        pissingPlayers[player] = streamTask
        object : BukkitRunnable() {
            override fun run() {
                stopPiss(player)
            }
        }.runTaskLater(hooker.plugin, 120L)
    }

    private fun spawnStream(player: Player) {
        val direction = player.location.direction.normalize()
        val startLocation = player.eyeLocation.clone().add(direction.x * 0.2, -0.9, direction.z * 0.2)

        val maxLength = 20.0
        val step = 0.3
        val velocity = direction.clone().multiply(1.2)
        val gravity = -0.05

        val spawnPositions = mutableListOf<Location>()
        var current = startLocation.clone()
        var traveled = 0.0

        while (traveled <= maxLength) {
            if (current.block.isSolid) {
                handleCollision(player, current.clone())
                break
            }
            val scatterXZ = 0.01 + (0.2 - 0.01) * (traveled / maxLength)
            val visualScale = 2.5
            val offsetX = (Random.nextDouble() * 2 - 1) * scatterXZ * visualScale
            val offsetZ = (Random.nextDouble() * 2 - 1) * scatterXZ * visualScale
            val spawnLocation = current.clone().add(offsetX, 0.0, offsetZ)
            spawnPositions.add(spawnLocation)
            current = current.add(velocity.x * step, velocity.y * step, velocity.z * step)
            velocity.y += gravity
            traveled += step
        }

        val streamBlocks = mutableListOf<BlockDisplay>()
        activeStreams.add(streamBlocks)

        object : BukkitRunnable() {
            private var index = 0

            override fun run() {
                if (index >= spawnPositions.size) {
                    cancel()
                    return
                }
                val spawnLocation = spawnPositions[index]
                val display = spawnLocation.world.spawn(spawnLocation, BlockDisplay::class.java) { d ->
                    d.block = Material.GOLD_BLOCK.createBlockData()
                    d.transformation = Transformation(
                        Vector3f(0f, 0f, 0f),
                        Quaternionf(),
                        Vector3f(0.1f, 0.1f, 0.1f),
                        Quaternionf()
                    )
                    d.isGlowing = hooker.utils.isGlowing(player)
                    for (onlinePlayer in org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer != player && hooker.utils.isHiddenFromPlayer(onlinePlayer, player)) {
                            onlinePlayer.hideEntity(hooker.plugin, d)
                        }
                    }
                }
                streamBlocks.add(display)
                object : BukkitRunnable() {
                    override fun run() {
                        display.remove()
                        streamBlocks.remove(display)
                    }
                }.runTaskLater(hooker.plugin, 2L)
                index++
            }
        }.runTaskTimer(hooker.plugin, 0L, 1L)
    }

    private fun handleCollision(player: Player, location: Location) {
        val block = location.block
        val spawnLocation = block.location.clone().add(0.5, 1.0, 0.5)
        val existing = scorePoints.find { it.location.distance(spawnLocation) <= 0.5 }
        if (existing != null) {
            existing.score++
            if (existing.score >= 5) {
                if (existing.display == null) {
                    val initialSize = 0.5f
                    val randX = (Random.nextInt(-10, 11) * 0.05)
                    val randZ = (Random.nextInt(-10, 11) * 0.05)
                    val randY = (Random.nextInt(-40, 41) * 0.0001).coerceIn(-0.0050, 0.0050)

                    val translationX = -initialSize / 2 + randX.toFloat()
                    val translationZ = -initialSize / 2 + randZ.toFloat()
                    val translationY = randY.toFloat()

                    val display = spawnLocation.world.spawn(spawnLocation, BlockDisplay::class.java) { d ->
                        d.block = Material.YELLOW_STAINED_GLASS.createBlockData()
                        d.transformation = Transformation(
                            Vector3f(translationX, translationY, translationZ),
                            Quaternionf(),
                            Vector3f(initialSize, 0.025f, initialSize),
                            Quaternionf()
                        )
                        d.isGlowing = hooker.utils.isGlowing(player)
                        for (onlinePlayer in org.bukkit.Bukkit.getOnlinePlayers()) {
                            if (hooker.utils.isHiddenFromPlayer(onlinePlayer, player)) {
                                onlinePlayer.hideEntity(hooker.plugin, d)
                                val hiddenMap = hiddenPuddleDisplays.computeIfAbsent(onlinePlayer.uniqueId) { ConcurrentHashMap() }
                                val list = hiddenMap.computeIfAbsent(player.uniqueId) { mutableListOf() }
                                list.add(d)
                            }
                        }
                    }
                    val point = ScorePoint(spawnLocation.clone(), existing.score, display, randX, randY, randZ, creator = player.uniqueId)
                    scorePoints.remove(existing)
                    scorePoints.add(point)
                    scheduleDecay(point)
                } else {
                    val maxSize = 2.0f
                    val sizeXZ = (0.5f + (existing.score - 5) * 0.25f).coerceAtMost(maxSize)
                    val translationX = -sizeXZ / 2 + existing.offsetX.toFloat()
                    val translationZ = -sizeXZ / 2 + existing.offsetZ.toFloat()
                    val translationY = existing.offsetY.toFloat()
                    existing.display!!.transformation = Transformation(
                        Vector3f(translationX, translationY, translationZ),
                        Quaternionf(),
                        Vector3f(sizeXZ, 0.025f, sizeXZ),
                        Quaternionf()
                    )
                    existing.display!!.isGlowing = hooker.utils.isGlowing(player)
                }
            }
        } else {
            val point = ScorePoint(spawnLocation.clone(), 1, null, 0.0, 0.0, 0.0, creator = null)
            scorePoints.add(point)
        }
    }

    private fun scheduleDecay(point: ScorePoint) {
        object : BukkitRunnable() {
            override fun run() {
                val display = point.display ?: return cancel()
                val current = display.transformation.scale
                if (current.x() <= 0.25f || current.z() <= 0.25f) {
                    display.remove()
                    scorePoints.remove(point)
                    for (entry in hiddenPuddleDisplays.entries) {
                        for (inner in entry.value.entries) {
                            inner.value.removeIf { it == display }
                            if (inner.value.isEmpty()) {
                                entry.value.remove(inner.key)
                            }
                        }
                        if (entry.value.isEmpty()) {
                            hiddenPuddleDisplays.remove(entry.key)
                        }
                    }
                    cancel()
                    return
                }
                val newSize = (current.x() - 0.25f).coerceAtLeast(0.25f)
                val translationX = -newSize / 2 + point.offsetX.toFloat()
                val translationZ = -newSize / 2 + point.offsetZ.toFloat()
                val translationY = point.offsetY.toFloat()
                display.transformation = Transformation(
                    Vector3f(translationX, translationY, translationZ),
                    Quaternionf(),
                    Vector3f(newSize, 0.025f, newSize),
                    Quaternionf()
                )
            }
        }.runTaskTimer(hooker.plugin, 15 * 20L, 5L)
    }

    fun stopPiss(player: Player) {
        pissingPlayers[player]?.cancel()
        pissingPlayers.remove(player)
    }

    fun clearDisplays() {
        activeStreams.forEach { stream ->
            stream.forEach { it.remove() }
        }
        activeStreams.clear()
        scorePoints.forEach { point ->
            point.display?.remove()
        }
        scorePoints.clear()
        hiddenPuddleDisplays.clear()
    }
}