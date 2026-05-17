package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintLogicalPixelChange
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import com.ratger.acreative.commands.paint.tools.FillArea
import com.ratger.acreative.commands.paint.tools.PaintColorResolver

class PaintCanvasPixelProjector(
    private val canvasRenderer: CanvasRenderer,
    private val paintColorResolver: PaintColorResolver
) {

    fun resolveLogicalColor(session: PaintSession, pixel: PaintSurfacePixel): Byte? {
        val colors = session.logicalCells[pixel.cellPoint] ?: return null
        val index = pixel.localY * MAP_WIDTH + pixel.localX
        if (index !in colors.indices) return null
        return colors[index]
    }

    fun putLogicalChange(
        changes: MutableMap<Long, PaintLogicalPixelChange>,
        globalX: Int,
        globalY: Int,
        oldColor: Byte,
        newColor: Byte
    ) {
        val key = packGridPoint(globalX, globalY)
        val existing = changes[key]
        if (existing == null) {
            changes[key] = PaintLogicalPixelChange(globalX, globalY, oldColor, newColor)
            return
        }
        if (existing.oldColor == newColor) {
            changes.remove(key)
            return
        }
        changes[key] = existing.copy(newColor = newColor)
    }

    fun applyLogicalPixelChanges(
        session: PaintSession,
        pixelChanges: List<PaintLogicalPixelChange>
    ): List<MapDataExtractor.Patch> {
        if (pixelChanges.isEmpty()) return emptyList()
        val affectedLogicalPoints = linkedSetOf<PaintGridPoint>()
        pixelChanges.forEach { change ->
            val logicalPoint = PaintGridPoint(floorDiv(change.globalX, MAP_WIDTH), floorDiv(change.globalY, MAP_HEIGHT))
            val colors = session.logicalCells[logicalPoint] ?: return@forEach
            val localIndex = floorMod(change.globalY, MAP_HEIGHT) * MAP_WIDTH + floorMod(change.globalX, MAP_WIDTH)
            if (localIndex !in colors.indices) return@forEach
            colors[localIndex] = change.newColor
            affectedLogicalPoints += logicalPoint
        }
        return canvasRenderer.rerenderLogicalCells(session, affectedLogicalPoints)
    }

    fun buildPreviewOverlays(
        session: PaintSession,
        pixels: List<PaintSurfacePixel>,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        if (pixels.isEmpty()) return emptyList()
        val colorsByMapId = snapshotMapColorsById(session)
        val grouped = mutableMapOf<Int, PreviewOverlayBuilder>()
        pixels.forEach { pixel ->
            addRenderedPreviewPixel(
                session = session,
                grouped = grouped,
                colorsByMapId = colorsByMapId,
                logicalGlobalX = pixel.globalX,
                logicalGlobalY = pixel.globalY,
                previewColor = previewColor
            )
        }

        return grouped.values.map { it.build() }
    }

    fun buildFillPreviewOverlays(
        session: PaintSession,
        area: FillArea,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        if (area.isEmpty()) return emptyList()
        val colorsByMapId = snapshotMapColorsById(session)
        val grouped = mutableMapOf<Int, PreviewOverlayBuilder>()
        area.forEachIndex { index ->
            val globalX = area.cache.minGlobalX + index % area.cache.width
            val globalY = area.cache.minGlobalY + index / area.cache.width
            addRenderedPreviewPixel(
                session = session,
                grouped = grouped,
                colorsByMapId = colorsByMapId,
                logicalGlobalX = globalX,
                logicalGlobalY = globalY,
                previewColor = previewColor
            )
        }
        return grouped.values.map { it.build() }
    }

    fun resolveRenderedBrushSize(session: PaintSession, logicalBrushSize: Int): Int {
        return logicalBrushSize.coerceIn(1, 50) * session.appliedZoom
    }

    private fun addRenderedPreviewPixel(
        session: PaintSession,
        grouped: MutableMap<Int, PreviewOverlayBuilder>,
        colorsByMapId: Map<Int, ByteArray?>,
        logicalGlobalX: Int,
        logicalGlobalY: Int,
        previewColor: Byte
    ) {
        forEachRenderedPixelIndex(session, logicalGlobalX, logicalGlobalY) { renderedPoint, localIndex ->
            val cell = session.canvasCells[renderedPoint] ?: return@forEachRenderedPixelIndex
            val mapColors = colorsByMapId[cell.mapId] ?: return@forEachRenderedPixelIndex
            if (localIndex !in mapColors.indices) return@forEachRenderedPixelIndex
            val oldColor = mapColors[localIndex]
            grouped.getOrPut(cell.mapId) { PreviewOverlayBuilder(cell.mapId) }
                .put(localIndex, paintColorResolver.blendPreviewColor(oldColor, previewColor))
        }
    }

    private inline fun forEachRenderedPixelIndex(
        session: PaintSession,
        logicalGlobalX: Int,
        logicalGlobalY: Int,
        action: (PaintGridPoint, Int) -> Unit
    ) {
        val zoom = session.appliedZoom
        val renderedStartX = logicalGlobalX * zoom
        val renderedStartY = logicalGlobalY * zoom
        for (offsetY in 0 until zoom) {
            val renderedGlobalY = renderedStartY + offsetY
            val renderedCellY = floorDiv(renderedGlobalY, MAP_HEIGHT)
            val renderedLocalY = floorMod(renderedGlobalY, MAP_HEIGHT)
            for (offsetX in 0 until zoom) {
                val renderedGlobalX = renderedStartX + offsetX
                val renderedCellX = floorDiv(renderedGlobalX, MAP_WIDTH)
                val renderedLocalX = floorMod(renderedGlobalX, MAP_WIDTH)
                action(
                    PaintGridPoint(renderedCellX, renderedCellY),
                    renderedLocalY * MAP_WIDTH + renderedLocalX
                )
            }
        }
    }

    private fun snapshotMapColorsById(session: PaintSession): Map<Int, ByteArray?> {
        return session.canvasCells.values.associate { cell ->
            cell.mapId to MapDataExtractor.extract(cell.mapId)?.colors
        }
    }

    private fun floorDiv(value: Int, divisor: Int): Int = Math.floorDiv(value, divisor)

    private fun floorMod(value: Int, divisor: Int): Int = Math.floorMod(value, divisor)

    private fun packGridPoint(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xFFFFFFFFL)

    private companion object {
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
    }
}
