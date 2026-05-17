package com.ratger.acreative.commands.paint.interaction

import com.ratger.acreative.commands.paint.model.PaintCanvasBounds
import com.ratger.acreative.commands.paint.model.PaintFrameDirection
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.abs

class HitDetectionService {

    fun resolveHitPixel(
        player: Player,
        session: PaintSession,
        allowMissingCell: Boolean,
        clampToCanvasBounds: Boolean = false
    ): PaintSurfacePixel? {
        val origin = player.eyeLocation.toVector()
        val direction = player.eyeLocation.direction.clone().normalize()
        val planePoint = resolveRenderPlaneCenter(session.anchorLocation, session.frameDirection).toVector()
        val planeNormal = Vector(session.frameDirection.normalX, 0.0, session.frameDirection.normalZ)
        val facingDot = direction.dot(planeNormal.clone().multiply(-1.0))
        if (facingDot <= MIN_CANVAS_FACING_DOT) return null

        val denominator = direction.dot(planeNormal)
        if (abs(denominator) <= PLANE_EPSILON) return null
        val t = planePoint.clone().subtract(origin).dot(planeNormal) / denominator
        if (t <= 0.0) return null

        val hit = origin.clone().add(direction.multiply(t))
        val relative = hit.clone().subtract(planePoint)
        val canvasBounds = if (clampToCanvasBounds) resolveCanvasPlaneBounds(session) else null
        val half = FRAME_RENDER_SIZE / 2.0
        val renderAnchorPoint = renderAnchorPoint(session, session.appliedZoom)
        val rawHorizontal = relative.x * session.frameDirection.rightAxisX + relative.z * session.frameDirection.rightAxisZ
        val rawVertical = relative.y
        val horizontal = canvasBounds?.let { rawHorizontal.coerceIn(it.minHorizontal, it.maxHorizontal) } ?: rawHorizontal
        val vertical = canvasBounds?.let { rawVertical.coerceIn(it.minVertical, it.maxVertical) } ?: rawVertical

        var cellX = floor(horizontal + renderAnchorPoint.x.toDouble() + 0.5)
        var cellY = renderAnchorPoint.y - floor(vertical + 0.5)
        if (canvasBounds != null) {
            cellX = cellX.coerceIn(canvasBounds.minCellX, canvasBounds.maxCellX)
            cellY = cellY.coerceIn(canvasBounds.minCellY, canvasBounds.maxCellY)
        }

        var point = PaintGridPoint(cellX, cellY)
        if (canvasBounds != null && point !in session.canvasCells) {
            point = resolveNearestCanvasCellPoint(session, horizontal, vertical) ?: return null
            cellX = point.x
            cellY = point.y
        }
        if (!allowMissingCell && point !in session.canvasCells) return null

        val localHorizontal = (horizontal - (cellX - renderAnchorPoint.x).toDouble()).let { value ->
            if (canvasBounds == null) value else value.coerceIn(-half, half)
        }
        val localVertical = (vertical - (renderAnchorPoint.y - cellY).toDouble()).let { value ->
            if (canvasBounds == null) value else value.coerceIn(-half, half)
        }
        if (canvasBounds == null && (localHorizontal < -half || localHorizontal > half || localVertical < -half || localVertical > half)) {
            return null
        }

        val pixelX = floor(((localHorizontal + half) / FRAME_RENDER_SIZE) * MAP_WIDTH).coerceIn(0, MAP_WIDTH - 1)
        val pixelY = floor(((half - localVertical) / FRAME_RENDER_SIZE) * MAP_HEIGHT).coerceIn(0, MAP_HEIGHT - 1)
        val renderedGlobalX = cellX * MAP_WIDTH + pixelX
        val renderedGlobalY = cellY * MAP_HEIGHT + pixelY
        val logicalGlobalX = floorDiv(renderedGlobalX, session.appliedZoom)
        val logicalGlobalY = floorDiv(renderedGlobalY, session.appliedZoom)
        val logicalCellX = floorDiv(logicalGlobalX, MAP_WIDTH)
        val logicalCellY = floorDiv(logicalGlobalY, MAP_HEIGHT)
        val logicalPoint = PaintGridPoint(logicalCellX, logicalCellY)
        if (!allowMissingCell && logicalPoint !in session.logicalCells) return null
        return PaintSurfacePixel(
            cellPoint = logicalPoint,
            localX = floorMod(logicalGlobalX, MAP_WIDTH),
            localY = floorMod(logicalGlobalY, MAP_HEIGHT),
            globalX = logicalGlobalX,
            globalY = logicalGlobalY
        )
    }

    private fun resolveRenderPlaneCenter(anchorLocation: Location, direction: PaintFrameDirection): Location {
        val anchorX = floor(anchorLocation.x) + 0.5
        val anchorY = floor(anchorLocation.y) + 0.5
        val anchorZ = floor(anchorLocation.z) + 0.5
        return Location(
            anchorLocation.world,
            anchorX - direction.normalX * FRAME_HANGING_CENTER_OFFSET,
            anchorY,
            anchorZ - direction.normalZ * FRAME_HANGING_CENTER_OFFSET
        )
    }

    private fun resolveCanvasPlaneBounds(session: PaintSession): CanvasPlaneBounds? {
        val bounds = PaintCanvasBounds.from(session.canvasCells.keys) ?: return null
        val renderAnchorPoint = renderAnchorPoint(session, session.appliedZoom)
        val half = FRAME_RENDER_SIZE / 2.0
        val minHorizontal = bounds.minX.toDouble() - renderAnchorPoint.x.toDouble() - half
        val maxHorizontal = bounds.maxX.toDouble() - renderAnchorPoint.x.toDouble() + half
        val minVertical = renderAnchorPoint.y.toDouble() - bounds.maxY.toDouble() - half
        val maxVertical = renderAnchorPoint.y.toDouble() - bounds.minY.toDouble() + half
        return CanvasPlaneBounds(
            minCellX = bounds.minX,
            maxCellX = bounds.maxX,
            minCellY = bounds.minY,
            maxCellY = bounds.maxY,
            minHorizontal = minHorizontal,
            maxHorizontal = maxHorizontal,
            minVertical = minVertical,
            maxVertical = maxVertical
        )
    }

    private fun resolveNearestCanvasCellPoint(
        session: PaintSession,
        horizontal: Double,
        vertical: Double
    ): PaintGridPoint? {
        val half = FRAME_RENDER_SIZE / 2.0
        val renderAnchorPoint = renderAnchorPoint(session, session.appliedZoom)
        return session.canvasCells.keys.minByOrNull { point ->
            val centerHorizontal = point.x.toDouble() - renderAnchorPoint.x.toDouble()
            val centerVertical = renderAnchorPoint.y.toDouble() - point.y.toDouble()
            val nearestHorizontal = horizontal.coerceIn(centerHorizontal - half, centerHorizontal + half)
            val nearestVertical = vertical.coerceIn(centerVertical - half, centerVertical + half)
            val deltaHorizontal = horizontal - nearestHorizontal
            val deltaVertical = vertical - nearestVertical
            deltaHorizontal * deltaHorizontal + deltaVertical * deltaVertical
        }
    }

    private fun renderAnchorPoint(session: PaintSession, zoom: Int): PaintGridPoint {
        val referenceSubCell = resolveZoomReferenceSubCell(zoom)
        return PaintGridPoint(
            session.anchorPoint.x * zoom + referenceSubCell.x,
            session.anchorPoint.y * zoom + referenceSubCell.y
        )
    }

    private fun resolveZoomReferenceSubCell(zoom: Int): PaintGridPoint {
        if (zoom <= 1) return PaintGridPoint(0, 0)
        if (zoom == 2) return PaintGridPoint(0, 1)
        return PaintGridPoint((zoom - 1) / 2, zoom / 2)
    }

    private fun floor(value: Double): Int = kotlin.math.floor(value).toInt()
    private fun floorDiv(value: Int, divisor: Int): Int = Math.floorDiv(value, divisor)
    private fun floorMod(value: Int, divisor: Int): Int = Math.floorMod(value, divisor)

    companion object {
        private const val FRAME_RENDER_SIZE = 1.05
        private const val FRAME_HANGING_CENTER_OFFSET = 0.46875
        private const val PLANE_EPSILON = 1.0E-6
        private const val MIN_CANVAS_FACING_DOT = -0.1
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
    }

    private data class CanvasPlaneBounds(
        val minCellX: Int,
        val maxCellX: Int,
        val minCellY: Int,
        val maxCellY: Int,
        val minHorizontal: Double,
        val maxHorizontal: Double,
        val minVertical: Double,
        val maxVertical: Double
    )
}
