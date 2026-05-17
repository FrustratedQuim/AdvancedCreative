package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.*
import org.bukkit.Location
import org.bukkit.entity.Player

class CanvasRenderer(
    private val cellFactory: CanvasCellFactory,
    private val mapDataSender: MapDataSender,
    private val frameSpaceValidator: (Player, PaintSession, Location) -> Boolean = { _, session, location ->
        location.block.type.isAir && session.canvasCells.values.none { other ->
            other.location.world == location.world &&
                other.location.distanceSquared(location) < LOCATION_EPSILON
        }
    }
) {

    fun renderCanvas(player: Player, session: PaintSession, targetZoom: Int): Boolean {
        val plans = buildRenderedCellPlans(session, targetZoom)
        if (!canOccupyRenderedPlans(player, session, plans)) {
            return false
        }

        val reusableCells = session.canvasCells.values
            .sortedWith(compareBy<PaintCanvasCell> { it.point.y }.thenBy { it.point.x })
            .toMutableList()
        val reusableCount = minOf(reusableCells.size, plans.size)
        val extraPlans = plans.drop(reusableCount)
        val extraCells = mutableListOf<PaintCanvasCell>()

        for (plan in extraPlans) {
            val created = createRenderedCanvasCell(player, session, plan) ?: run {
                extraCells.forEach { cellFactory.removeCell(it) }
                return false
            }
            extraCells += created
        }

        val newCanvasCells = linkedMapOf<PaintGridPoint, PaintCanvasCell>()
        for (index in 0 until reusableCount) {
            val reused = reuseRenderedCanvasCell(session, reusableCells[index], plans[index])
            newCanvasCells[reused.point] = reused
        }
        extraCells.forEach { cell ->
            newCanvasCells[cell.point] = cell
        }

        reusableCells.drop(reusableCount).forEach { cellFactory.removeCell(it) }
        session.canvasCells.clear()
        session.canvasCells.putAll(newCanvasCells)

        return true
    }

    fun rerenderLogicalCells(session: PaintSession, logicalPoints: Set<PaintGridPoint>): List<MapDataExtractor.Patch> {
        if (logicalPoints.isEmpty()) return emptyList()

        val patches = mutableListOf<MapDataExtractor.Patch>()
        logicalPoints.forEach { logicalPoint ->
            val logicalColors = session.logicalCells[logicalPoint] ?: return@forEach
            for (subY in 0 until session.appliedZoom) {
                for (subX in 0 until session.appliedZoom) {
                    val renderedPoint = resolveRenderedCellPoint(logicalPoint, subX, subY, session.appliedZoom)
                    val cell = session.canvasCells[renderedPoint] ?: continue
                    val currentColors = MapDataExtractor.colorsView(cell.mapId) ?: continue
                    val renderedColors = buildRenderedMapColors(logicalColors, session.appliedZoom, subX, subY)
                    val changes = mutableListOf<PaintPixelChange>()

                    renderedColors.indices.forEach { index ->
                        val oldColor = currentColors[index]
                        val newColor = renderedColors[index]
                        if (oldColor != newColor) {
                            changes += PaintPixelChange(index % MAP_WIDTH, index / MAP_WIDTH, oldColor, newColor)
                        }
                    }

                    MapDataExtractor.setPixelChangesPatch(cell.mapId, changes)?.let { patch ->
                        patches += patch
                    }
                }
            }
        }
        return patches
    }

    fun removeCanvas(session: PaintSession) {
        session.canvasCells.values.forEach { cell ->
            cellFactory.removeCell(cell)
        }
        session.canvasCells.clear()
    }

    private fun buildRenderedCellPlans(session: PaintSession, targetZoom: Int): List<RenderedCellPlan> {
        val renderAnchorPoint = renderAnchorPoint(session, targetZoom)
        return session.logicalCells.keys
            .sortedWith(compareBy<PaintGridPoint> { it.y }.thenBy { it.x })
            .flatMap { logicalPoint ->
                val logicalColors = session.logicalCells[logicalPoint] ?: return@flatMap emptyList()
                buildList {
                    for (subY in 0 until targetZoom) {
                        for (subX in 0 until targetZoom) {
                            val renderedPoint = resolveRenderedCellPoint(logicalPoint, subX, subY, targetZoom)
                            add(
                                RenderedCellPlan(
                                    point = renderedPoint,
                                    location = resolveCellLocation(
                                        session.anchorLocation,
                                        renderAnchorPoint,
                                        renderedPoint,
                                        session.frameDirection
                                    ),
                                    colors = buildRenderedMapColors(logicalColors, targetZoom, subX, subY)
                                )
                            )
                        }
                    }
                }
            }
    }

    private fun canOccupyRenderedPlans(
        player: Player,
        session: PaintSession,
        plans: List<RenderedCellPlan>
    ): Boolean {
        return plans.all { plan -> isFrameSpaceAvailable(player, session, plan.location) }
    }

    private fun isFrameSpaceAvailable(player: Player, session: PaintSession, location: Location): Boolean {
        return frameSpaceValidator(player, session, location)
    }

    private fun createRenderedCanvasCell(
        player: Player,
        session: PaintSession,
        plan: RenderedCellPlan
    ): PaintCanvasCell? {
        val snapshot = MapDataExtractor.createFilled(player.world, BACKGROUND_COLOR_ID) ?: return null
        val renderedSnapshot = MapDataExtractor.replaceColors(snapshot.mapId, plan.colors) ?: return null

        val cell = cellFactory.createCell(
            plan.point,
            player,
            renderedSnapshot,
            session.frameDirection,
            session.viewers,
            plan.location
        ) ?: return null

        mapDataSender.sendToViewers(session.viewers, renderedSnapshot)
        return cell
    }

    private fun reuseRenderedCanvasCell(
        session: PaintSession,
        cell: PaintCanvasCell,
        plan: RenderedCellPlan
    ): PaintCanvasCell {
        cellFactory.reuseCell(session, cell, plan.location, plan.colors)
        return PaintCanvasCell(
            point = plan.point,
            mapId = cell.mapId,
            frame = cell.frame,
            backPanel = cell.backPanel,
            location = plan.location
        )
    }

    private fun buildRenderedMapColors(logicalColors: ByteArray, zoom: Int, subCellX: Int, subCellY: Int): ByteArray {
        val colors = ByteArray(MAP_WIDTH * MAP_HEIGHT)
        var index = 0
        for (actualY in 0 until MAP_HEIGHT) {
            val logicalY = ((subCellY * MAP_HEIGHT) + actualY) / zoom
            val logicalRowOffset = logicalY * MAP_WIDTH
            for (actualX in 0 until MAP_WIDTH) {
                val logicalX = ((subCellX * MAP_WIDTH) + actualX) / zoom
                colors[index++] = logicalColors[logicalRowOffset + logicalX]
            }
        }
        return colors
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

    private fun resolveRenderedCellPoint(logicalPoint: PaintGridPoint, subCellX: Int, subCellY: Int, zoom: Int): PaintGridPoint {
        return PaintGridPoint(logicalPoint.x * zoom + subCellX, logicalPoint.y * zoom + subCellY)
    }

    private fun resolveCellLocation(
        anchorLocation: Location,
        renderAnchorPoint: PaintGridPoint,
        renderedPoint: PaintGridPoint,
        direction: PaintFrameDirection
    ): Location {
        val horizontalOffset = renderedPoint.x - renderAnchorPoint.x
        val verticalOffset = renderAnchorPoint.y - renderedPoint.y
        return anchorLocation.clone().add(
            direction.rightAxisX * horizontalOffset,
            verticalOffset.toDouble(),
            direction.rightAxisZ * horizontalOffset
        )
    }

    private data class RenderedCellPlan(
        val point: PaintGridPoint,
        val location: Location,
        val colors: ByteArray
    )

    companion object {
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
        private const val LOCATION_EPSILON = 0.01
        private val BACKGROUND_COLOR_ID: Byte = MapDataExtractor.DEFAULT_CANVAS_COLOR_ID
    }
}
