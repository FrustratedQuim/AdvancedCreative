package com.ratger.acreative.commands.paint.tools

import com.ratger.acreative.commands.paint.map.MapColorMatcher
import com.ratger.acreative.commands.paint.model.PaintBinaryBrushSettings
import com.ratger.acreative.commands.paint.model.PaintBrushSettings
import com.ratger.acreative.commands.paint.model.PaintLineAnchor
import com.ratger.acreative.commands.paint.model.PaintLogicalPixelChange
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintShapeSettings
import com.ratger.acreative.commands.paint.model.PaintShapeType
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import com.ratger.acreative.commands.paint.model.PaintToolMode
import com.ratger.acreative.commands.paint.palette.PaintPalette
import com.ratger.acreative.commands.paint.rendering.PaintCanvasPixelProjector
import com.ratger.acreative.commands.paint.rendering.PaintPreviewCoordinator
import com.ratger.acreative.menus.paint.PaintToolDefinition
import org.bukkit.entity.Player
import kotlin.random.Random

class PaintToolApplier(
    private val brushStrokeResolver: BrushStrokeResolver,
    private val fillComponentResolver: FillComponentResolver,
    private val shapeSurfaceResolver: ShapeSurfaceResolver,
    private val paintColorResolver: PaintColorResolver,
    private val canvasPixelProjector: PaintCanvasPixelProjector,
    private val previewCoordinator: PaintPreviewCoordinator,
    private val historyManager: HistoryManager,
    private val openEaselMenu: (Player, PaintSession) -> Unit,
    private val backgroundColorId: Byte
) {

    fun executeTool(
        player: Player,
        session: PaintSession,
        tool: PaintToolDefinition,
        hit: PaintSurfacePixel
    ) {
        when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val paletteKey = requireNotNull(tool.fixedPaletteKey)
                applyBrushLikeTool(
                    session = session,
                    hit = hit,
                    settings = session.toolSettings.basicBrush(paletteKey),
                    paletteKey = paletteKey
                )
            }

            PaintToolMode.CUSTOM_BRUSH -> {
                applyBrushLikeTool(
                    session = session,
                    hit = hit,
                    settings = session.toolSettings.customBrush,
                    paletteKey = session.toolSettings.customBrush.paletteKey
                )
            }

            PaintToolMode.ERASER -> {
                applyBinaryBrushTool(session, hit, session.toolSettings.eraser, backgroundColorId)
            }

            PaintToolMode.SHEARS -> {
                applyBinaryBrushTool(session, hit, session.toolSettings.shears, MapColorMatcher.TRANSPARENT_COLOR_ID)
            }

            PaintToolMode.FILL -> {
                applyFillTool(session, hit)
            }

            PaintToolMode.SHAPE -> {
                applyShapeTool(session, hit)
            }

            PaintToolMode.EASEL -> openEaselMenu(player, session)
        }
    }

    private fun applyBrushLikeTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBrushSettings,
        paletteKey: String
    ) {
        val entry = PaintPalette.entry(paletteKey)
        val color = entry.packed(settings.shade)
        val brushPixels = brushStrokeResolver.resolveStrokePixels(session, hit, settings.normalizedSize())
        if (brushPixels.isEmpty()) return

        val changes = linkedMapOf<Long, PaintLogicalPixelChange>()
        val fillPercent = settings.normalizedFillPercent()
        val colors = paintColorResolver.resolveMixedPaletteColors(entry, settings.shade, settings.normalizedShadeMix())
        for (pixel in brushPixels) {
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) continue
            val oldColor = canvasPixelProjector.resolveLogicalColor(session, pixel) ?: continue
            val newColor = paintColorResolver.randomColor(colors)
            if (oldColor == newColor) continue
            canvasPixelProjector.putLogicalChange(changes, pixel.globalX, pixel.globalY, oldColor, newColor)
        }

        previewCoordinator.suppressLargeBrushPreview(
            session,
            canvasPixelProjector.resolveRenderedBrushSize(session, settings.normalizedSize())
        )
        brushStrokeResolver.rememberStroke(session, hit, color)
        historyManager.applyHistoryChanges(session, changes.values.toList())
    }

    private fun applyBinaryBrushTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBinaryBrushSettings,
        color: Byte
    ) {
        val brushPixels = brushStrokeResolver.resolveStrokePixels(session, hit, settings.normalizedSize())
        if (brushPixels.isEmpty()) return

        val changes = linkedMapOf<Long, PaintLogicalPixelChange>()
        val fillPercent = settings.normalizedFillPercent()
        for (pixel in brushPixels) {
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) continue
            val oldColor = canvasPixelProjector.resolveLogicalColor(session, pixel) ?: continue
            if (oldColor == color) continue
            canvasPixelProjector.putLogicalChange(changes, pixel.globalX, pixel.globalY, oldColor, color)
        }

        previewCoordinator.suppressLargeBrushPreview(
            session,
            canvasPixelProjector.resolveRenderedBrushSize(session, settings.normalizedSize())
        )
        brushStrokeResolver.rememberStroke(session, hit, color)
        historyManager.applyHistoryChanges(session, changes.values.toList())
    }

    private fun applyFillTool(session: PaintSession, hit: PaintSurfacePixel) {
        val now = System.currentTimeMillis()
        if (now < session.fillCooldownUntilMillis) return
        session.fillCooldownUntilMillis = now + FILL_COOLDOWN_MILLIS

        val settings = session.toolSettings.fill
        val area = fillComponentResolver.resolveArea(session, hit, settings.ignoreShade) ?: return
        if (area.isEmpty()) return

        val changes = linkedMapOf<Long, PaintLogicalPixelChange>()
        val entry = PaintPalette.entry(settings.paletteKey)
        val fillPercent = settings.normalizedFillPercent()
        val colors = paintColorResolver.resolveMixedPaletteColors(entry, settings.baseShade, settings.normalizedShadeMix())
        area.forEachIndex { index ->
            val oldColor = area.cache.colors[index]
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) return@forEachIndex
            val newColor = paintColorResolver.randomColor(colors)
            if (oldColor == newColor) return@forEachIndex
            val globalX = area.cache.minGlobalX + index % area.cache.width
            val globalY = area.cache.minGlobalY + index / area.cache.width
            canvasPixelProjector.putLogicalChange(changes, globalX, globalY, oldColor, newColor)
        }

        previewCoordinator.clearStrokeState(session)
        historyManager.applyHistoryChanges(session, changes.values.toList())
    }

    private fun applyShapeTool(session: PaintSession, hit: PaintSurfacePixel) {
        val settings = session.toolSettings.shape
        if (settings.shapeType == PaintShapeType.LINE) {
            applyLineShapeTool(session, hit, settings)
            return
        }

        val entry = PaintPalette.entry(settings.paletteKey)
        val pixels = shapeSurfaceResolver.resolvePixels(session, hit, settings)
        if (pixels.isEmpty()) return

        val changes = linkedMapOf<Long, PaintLogicalPixelChange>()
        val fillPercent = settings.normalizedFillPercent()
        val colors = paintColorResolver.resolveMixedPaletteColors(entry, settings.shade, settings.normalizedShadeMix())
        pixels.forEach { pixel ->
            val oldColor = canvasPixelProjector.resolveLogicalColor(session, pixel) ?: return@forEach
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) return@forEach
            val newColor = paintColorResolver.randomColor(colors)
            if (oldColor == newColor) return@forEach
            canvasPixelProjector.putLogicalChange(changes, pixel.globalX, pixel.globalY, oldColor, newColor)
        }

        previewCoordinator.clearStrokeState(session)
        historyManager.applyHistoryChanges(session, changes.values.toList())
    }

    private fun applyLineShapeTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintShapeSettings
    ) {
        val previousAnchor = session.shapeLineAnchor
        val nextAnchor = PaintLineAnchor(hit.globalX, hit.globalY)
        if (previousAnchor == null) {
            previewCoordinator.clearStrokeState(session)
            historyManager.applyHistoryChanges(
                session = session,
                pixelChanges = emptyList(),
                lineAnchorBefore = null,
                lineAnchorAfter = nextAnchor
            )
            return
        }

        val entry = PaintPalette.entry(settings.paletteKey)
        val pixels = shapeSurfaceResolver.resolveLinePixels(
            session = session,
            startX = previousAnchor.globalX,
            startY = previousAnchor.globalY,
            endX = hit.globalX,
            endY = hit.globalY,
            settings = settings
        )
        val changes = linkedMapOf<Long, PaintLogicalPixelChange>()
        val fillPercent = settings.normalizedFillPercent()
        val colors = paintColorResolver.resolveMixedPaletteColors(entry, settings.shade, settings.normalizedShadeMix())
        pixels.forEach { pixel ->
            val oldColor = canvasPixelProjector.resolveLogicalColor(session, pixel) ?: return@forEach
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) return@forEach
            val newColor = paintColorResolver.randomColor(colors)
            if (oldColor == newColor) return@forEach
            canvasPixelProjector.putLogicalChange(changes, pixel.globalX, pixel.globalY, oldColor, newColor)
        }

        previewCoordinator.clearStrokeState(session)
        historyManager.applyHistoryChanges(
            session = session,
            pixelChanges = changes.values.toList(),
            lineAnchorBefore = previousAnchor,
            lineAnchorAfter = nextAnchor
        )
    }

    private companion object {
        private const val FILL_COOLDOWN_MILLIS = 100L
    }
}
