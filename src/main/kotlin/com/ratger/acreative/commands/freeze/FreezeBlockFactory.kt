package com.ratger.acreative.commands.freeze

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Quaternion4f
import com.github.retrooper.packetevents.util.Vector3f
import com.ratger.acreative.core.FunctionHooker
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal class FreezeBlockFactory(private val hooker: FunctionHooker) {

    fun createFor(player: Player): MutableList<WrapperEntity> {
        val location = player.location
        val blocks = mutableListOf<WrapperEntity>()

        for (viewer in location.world?.players?.filter { it.isOnline } ?: emptyList()) {
            if (viewer != player && hooker.utils.isHiddenFromPlayer(viewer, player)) continue
            viewer.playSound(location, Sound.BLOCK_ANVIL_LAND, 1f, Random.nextDouble(0.8, 1.2).toFloat())
        }

        val scale = player.getAttribute(Attribute.SCALE)?.value ?: 1.0
        val blockCount = 4
        val blockState = WrappedBlockState.getDefaultState(
            PacketEvents.getAPI().serverManager.version.toClientVersion(),
            StateTypes.ICE
        )

        val hitboxRadius = 0.05 * scale
        val centerBias = -0.1 * scale
        val minY = 0.2 * scale
        val maxY = 1.8 * scale
        val sectorHeight = (maxY - minY) / blockCount

        val possibleCombos = listOf(1 to 1, 1 to -1, -1 to -1, -1 to 1).shuffled().take(blockCount).shuffled()
        val usedOffsets = mutableListOf<Pair<Double, Double>>()
        val baseDirection = if (player.isGliding) player.location.direction.normalize() else Vector(0.0, 1.0, 0.0)

        repeat(blockCount) { index ->
            val offsetAlongDir = minY + index * sectorHeight + Random.nextDouble(-0.1, 0.1) * scale
            val combo = possibleCombos[index]
            var offsetX: Double
            var offsetZ: Double
            var attempts = 0

            do {
                offsetX = combo.first * Random.nextDouble(0.0, hitboxRadius)
                offsetZ = combo.second * Random.nextDouble(0.0, hitboxRadius)
                attempts++
            } while (
                attempts < 20 && usedOffsets.any { (x, z) ->
                    val dx = offsetX - x
                    val dz = offsetZ - z
                    sqrt(dx * dx + dz * dz) < 0.15 * scale
                }
            )
            usedOffsets.add(offsetX to offsetZ)

            val blockLoc = location.clone()
                .add(baseDirection.clone().multiply(offsetAlongDir + centerBias))
                .add(offsetX, 0.0, offsetZ)

            val entity = WrapperEntity(EntityTypes.BLOCK_DISPLAY)
            val blockMeta = entity.entityMeta as BlockDisplayMeta
            blockMeta.blockId = blockState.globalId
            val size = (Random.nextDouble(0.5, 0.6) * scale).toFloat()
            blockMeta.scale = Vector3f(size, size, size)
            blockMeta.rightRotation = randomQuaternion()

            val packetLoc = Location(blockLoc.x, blockLoc.y, blockLoc.z, blockLoc.yaw, blockLoc.pitch)
            entity.addViewer(player.uniqueId)
            for (viewer in location.world?.players?.filter { it.isOnline && it != player && !hooker.utils.isHiddenFromPlayer(it, player) }
                ?: emptyList()) {
                entity.addViewer(viewer.uniqueId)
            }
            entity.spawn(packetLoc)
            blocks.add(entity)
        }

        return blocks
    }

    private fun randomQuaternion(): Quaternion4f {
        val yaw = Random.nextFloat() * 360f
        val pitch = Random.nextFloat() * 180f - 90f
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()
        val cy = cos(yawRad * 0.5f)
        val sy = sin(yawRad * 0.5f)
        val cp = cos(pitchRad * 0.5f)
        val sp = sin(pitchRad * 0.5f)
        return Quaternion4f(sp * cy, cp * sy, -sp * sy, cp * cy)
    }
}
