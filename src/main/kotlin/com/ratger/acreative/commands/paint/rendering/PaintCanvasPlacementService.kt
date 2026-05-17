package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.model.PaintFrameDirection
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.math.floor

class PaintCanvasPlacementService {

    fun resolveDirection(player: Player): PaintFrameDirection {
        val yaw = ((player.location.yaw % 360f) + 360f) % 360f
        return when {
            yaw >= 45f && yaw < 135f -> PaintFrameDirection.WEST
            yaw >= 135f && yaw < 225f -> PaintFrameDirection.NORTH
            yaw >= 225f && yaw < 315f -> PaintFrameDirection.EAST
            else -> PaintFrameDirection.SOUTH
        }
    }

    fun resolveFrameLocation(player: Player, direction: PaintFrameDirection): Location {
        val baseLocation = player.location
        val desiredX = baseLocation.x + direction.offsetX * FRAME_DISTANCE
        val desiredY = player.eyeLocation.y - FRAME_EYE_OFFSET
        val desiredZ = baseLocation.z + direction.offsetZ * FRAME_DISTANCE
        val anchorX = floor(desiredX + direction.normalX * FRAME_HANGING_CENTER_OFFSET)
        val anchorY = floor(desiredY)
        val anchorZ = floor(desiredZ + direction.normalZ * FRAME_HANGING_CENTER_OFFSET)
        return Location(
            baseLocation.world,
            anchorX + 0.5 - direction.normalX * FRAME_HANGING_CENTER_OFFSET,
            anchorY + 0.5,
            anchorZ + 0.5 - direction.normalZ * FRAME_HANGING_CENTER_OFFSET,
            direction.spawnYaw,
            0f
        )
    }

    fun resolveCellLocation(
        anchorLocation: Location,
        anchorPoint: PaintGridPoint,
        point: PaintGridPoint,
        direction: PaintFrameDirection
    ): Location {
        val horizontalOffset = point.x - anchorPoint.x
        val verticalOffset = anchorPoint.y - point.y
        return anchorLocation.clone().add(
            direction.rightAxisX * horizontalOffset,
            verticalOffset.toDouble(),
            direction.rightAxisZ * horizontalOffset
        )
    }

    private companion object {
        private const val FRAME_DISTANCE = 0.75
        private const val FRAME_EYE_OFFSET = 0.25
        private const val FRAME_HANGING_CENTER_OFFSET = 0.46875
    }
}
