package com.ratger.acreative.commands.paint.resize

import com.ratger.acreative.commands.paint.interaction.HitDetectionService
import com.ratger.acreative.commands.paint.model.PaintCanvasBounds
import com.ratger.acreative.commands.paint.model.PaintFrameDirection
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintSession
import org.bukkit.Location
import org.bukkit.entity.Player

data class PaintResizeTarget(
    val point: PaintGridPoint,
    val location: Location,
    val state: PaintResizeTargetState
)

enum class PaintResizeTargetState {
    ADD,
    REMOVE_OWN,
    INVALID
}

class PaintResizeTargetResolver(
    private val hitDetectionService: HitDetectionService,
    private val maxCanvasSide: Int,
    private val resolveCellLocation: (Location, PaintGridPoint, PaintGridPoint, PaintFrameDirection) -> Location,
    private val isEmptyFrameSpace: (Player, Location) -> Boolean
) {

    fun resolveTarget(player: Player, session: PaintSession): PaintResizeTarget? {
        val hit = hitDetectionService.resolveHitPixel(player, session, allowMissingCell = true) ?: return null
        val point = hit.cellPoint
        if (!isWithinResizePreviewRange(session, point)) {
            return null
        }

        val location = resolveCellLocation(session.anchorLocation, session.anchorPoint, point, session.frameDirection)

        if (point in session.logicalCells) {
            return PaintResizeTarget(point, location, PaintResizeTargetState.REMOVE_OWN)
        }

        if (!isEmptyFrameSpace(player, location)) {
            return PaintResizeTarget(point, location, PaintResizeTargetState.INVALID)
        }

        if (!isAdjacentToCanvas(session, point)) {
            return PaintResizeTarget(point, location, PaintResizeTargetState.INVALID)
        }

        val bounds = PaintCanvasBounds.from(session.logicalCells.keys + point)
            ?: return PaintResizeTarget(point, location, PaintResizeTargetState.INVALID)
        if (bounds.width > maxCanvasSide || bounds.height > maxCanvasSide) {
            return PaintResizeTarget(point, location, PaintResizeTargetState.INVALID)
        }

        return PaintResizeTarget(point, location, PaintResizeTargetState.ADD)
    }

    private fun isAdjacentToCanvas(session: PaintSession, point: PaintGridPoint): Boolean {
        return adjacentPoints(point).any { it in session.logicalCells }
    }

    private fun isWithinResizePreviewRange(session: PaintSession, point: PaintGridPoint): Boolean {
        val bounds = PaintCanvasBounds.from(session.logicalCells.keys) ?: return false
        return point.x in (bounds.minX - MAX_RESIZE_PREVIEW_DISTANCE_CELLS)..(bounds.maxX + MAX_RESIZE_PREVIEW_DISTANCE_CELLS) &&
            point.y in (bounds.minY - MAX_RESIZE_PREVIEW_DISTANCE_CELLS)..(bounds.maxY + MAX_RESIZE_PREVIEW_DISTANCE_CELLS)
    }

    private fun adjacentPoints(point: PaintGridPoint): List<PaintGridPoint> = listOf(
        PaintGridPoint(point.x + 1, point.y),
        PaintGridPoint(point.x - 1, point.y),
        PaintGridPoint(point.x, point.y + 1),
        PaintGridPoint(point.x, point.y - 1)
    )

    private companion object {
        private const val MAX_RESIZE_PREVIEW_DISTANCE_CELLS = 3
    }
}
