package com.ratger.acreative.commands.piss

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Vector3f
import com.ratger.acreative.core.FunctionHooker
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.Location as BukkitLocation
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class ScorePoint(
    val location: BukkitLocation,
    var score: Int,
    var display: WrapperEntity?,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    val creator: UUID? = null
)

class PissManager(private val hooker: FunctionHooker) {

    private val activeStreams = mutableListOf<MutableList<WrapperEntity>>()
    val scorePoints = mutableListOf<ScorePoint>()
    val pissingPlayers = mutableMapOf<Player, BukkitRunnable>()
    val hiddenPuddleDisplays = ConcurrentHashMap<UUID, MutableMap<UUID, MutableList<WrapperEntity>>>()

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
            hooker.messageManager.sendMiniMessage(player, key = "error-too-large")
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

        val spawnPositions = mutableListOf<BukkitLocation>()
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

        val streamBlocks = mutableListOf<WrapperEntity>()
        activeStreams.add(streamBlocks)

        object : BukkitRunnable() {
            private var index = 0
            private var tickCounter = 0

            override fun run() {
                if (index >= spawnPositions.size) {
                    cancel()
                    return
                }

                val blocksToSpawn = if (tickCounter % 2 == 0) 2 else 1
                repeat(blocksToSpawn) {
                    if (index >= spawnPositions.size) return@repeat
                    val spawnLocation = spawnPositions[index]
                    val api = PacketEvents.getAPI()
                    val blockState = WrappedBlockState.getDefaultState(
                        api.serverManager.version.toClientVersion(),
                        StateTypes.GOLD_BLOCK
                    )

                    val entity = WrapperEntity(EntityTypes.BLOCK_DISPLAY)
                    val blockMeta = entity.entityMeta as BlockDisplayMeta
                    blockMeta.blockId = blockState.globalId
                    blockMeta.scale = Vector3f(0.1f, 0.1f, 0.1f)
                    blockMeta.isGlowing = hooker.utils.isGlowing(player)

                    val packetLoc = PacketLocation(
                        spawnLocation.x,
                        spawnLocation.y,
                        spawnLocation.z,
                        spawnLocation.yaw,
                        spawnLocation.pitch
                    )

                    entity.addViewer(player.uniqueId)
                    for (viewer in Bukkit.getOnlinePlayers().filter { it != player && !hooker.utils.isHiddenFromPlayer(it, player) }) {
                        entity.addViewer(viewer.uniqueId)
                    }
                    entity.spawn(packetLoc)

                    streamBlocks.add(entity)
                    object : BukkitRunnable() {
                        override fun run() {
                            if (entity.isSpawned) {
                                entity.remove()
                                streamBlocks.remove(entity)
                            }
                        }
                    }.runTaskLater(hooker.plugin, 2L)
                    index++
                }
                tickCounter++
            }
        }.runTaskTimer(hooker.plugin, 0L, 1L)
    }

    private fun handleCollision(player: Player, location: BukkitLocation) {
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

                    val api = PacketEvents.getAPI()
                    val blockState = WrappedBlockState.getDefaultState(
                        api.serverManager.version.toClientVersion(),
                        StateTypes.YELLOW_STAINED_GLASS
                    )

                    val entity = WrapperEntity(EntityTypes.BLOCK_DISPLAY)
                    val blockMeta = entity.entityMeta as BlockDisplayMeta
                    blockMeta.blockId = blockState.globalId
                    blockMeta.scale = Vector3f(initialSize, 0.025f, initialSize)
                    blockMeta.translation = Vector3f(translationX, translationY, translationZ)
                    blockMeta.isGlowing = hooker.utils.isGlowing(player)

                    val packetLoc = PacketLocation(
                        spawnLocation.x,
                        spawnLocation.y,
                        spawnLocation.z,
                        spawnLocation.yaw,
                        spawnLocation.pitch
                    )

                    entity.addViewer(player.uniqueId)
                    for (viewer in Bukkit.getOnlinePlayers().filter { it != player && !hooker.utils.isHiddenFromPlayer(it, player) }) {
                        entity.addViewer(viewer.uniqueId)
                    }
                    entity.spawn(packetLoc)

                    val point = ScorePoint(spawnLocation.clone(), existing.score, entity, randX, randY, randZ, creator = player.uniqueId)
                    scorePoints.remove(existing)
                    scorePoints.add(point)

                    for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                        if (hooker.utils.isHiddenFromPlayer(onlinePlayer, player)) {
                            val hiddenMap = hiddenPuddleDisplays.computeIfAbsent(onlinePlayer.uniqueId) { ConcurrentHashMap() }
                            val list = hiddenMap.computeIfAbsent(player.uniqueId) { mutableListOf() }
                            list.add(entity)
                            entity.removeViewer(onlinePlayer.uniqueId)
                        }
                    }

                    scheduleDecay(point)
                } else {
                    val maxSize = 2.0f
                    val sizeXZ = (0.5f + (existing.score - 5) * 0.25f).coerceAtMost(maxSize)
                    val translationX = -sizeXZ / 2 + existing.offsetX.toFloat()
                    val translationZ = -sizeXZ / 2 + existing.offsetZ.toFloat()
                    val translationY = existing.offsetY.toFloat()
                    val blockMeta = existing.display!!.entityMeta as BlockDisplayMeta
                    blockMeta.scale = Vector3f(sizeXZ, 0.025f, sizeXZ)
                    blockMeta.translation = Vector3f(translationX, translationY, translationZ)
                    blockMeta.isGlowing = hooker.utils.isGlowing(player)
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
                val blockMeta = display.entityMeta as BlockDisplayMeta
                val current = blockMeta.scale
                if (current.x <= 0.25f || current.z <= 0.25f) {
                    if (display.isSpawned) {
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
                    }
                    cancel()
                    return
                }
                val newSize = (current.x - 0.25f).coerceAtLeast(0.25f)
                val translationX = -newSize / 2 + point.offsetX.toFloat()
                val translationZ = -newSize / 2 + point.offsetZ.toFloat()
                val translationY = point.offsetY.toFloat()
                blockMeta.scale = Vector3f(newSize, 0.025f, newSize)
                blockMeta.translation = Vector3f(translationX, translationY, translationZ)
            }
        }.runTaskTimer(hooker.plugin, 15 * 20L, 5L)
    }

    fun stopPiss(player: Player) {
        pissingPlayers[player]?.cancel()
        pissingPlayers.remove(player)
    }
}