package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.interaction.HitDetectionService
import com.ratger.acreative.commands.paint.map.MapColorMatcher
import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintShapeType
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import com.ratger.acreative.commands.paint.model.PaintToolMode
import com.ratger.acreative.commands.paint.palette.PaintPalette
import com.ratger.acreative.commands.paint.tools.BrushStrokeResolver
import com.ratger.acreative.commands.paint.tools.FillComponentCache
import com.ratger.acreative.commands.paint.tools.FillComponentResolver
import com.ratger.acreative.commands.paint.tools.ShapeSurfaceResolver
import com.ratger.acreative.menus.paint.PaintToolDefinition
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack as BukkitItemStack
import java.util.UUID

class PaintPreviewCoordinator(
    private val hitDetectionService: HitDetectionService,
    private val mapDataSender: MapDataSender,
    private val brushStrokeResolver: BrushStrokeResolver,
    private val fillComponentResolver: FillComponentResolver,
    private val shapeSurfaceResolver: ShapeSurfaceResolver,
    private val canvasPixelProjector: PaintCanvasPixelProjector,
    private val toolResolver: (BukkitItemStack?) -> PaintToolDefinition?,
    private val isDirectUseSuppressed: (UUID) -> Boolean,
    private val backgroundColorId: Byte
) {

    private data class FillPreviewCacheEntry(
        val key: String,
        val overlays: List<PreviewMapOverlay>
    )

    private data class BrushPreviewCacheEntry(
        val key: String,
        val overlays: List<PreviewMapOverlay>
    )

    private val fillPreviewCache = mutableMapOf<UUID, FillPreviewCacheEntry>()
    private val brushPreviewCache = mutableMapOf<UUID, BrushPreviewCacheEntry>()
    private val activePreviewOverlays = mutableMapOf<UUID, List<PreviewMapOverlay>>()

    fun updatePreview(player: Player, session: PaintSession) {
        if (session.isMenuOpen) {
            clearSuppression(session)
            restoreIfNeeded(player, session)
            return
        }
        if (session.previewPaused && isDirectUseSuppressed(player.uniqueId)) {
            restoreIfNeeded(player, session)
            return
        }
        if (session.previewPaused) {
            session.previewPaused = false
            clearSuppression(session)
        }

        val tool = toolResolver(player.inventory.itemInMainHand) ?: run {
            clearSuppression(session)
            clearStrokeState(session)
            clearLineAnchorState(session)
            restoreIfNeeded(player, session)
            return
        }
        if (!isLineShapeTool(tool, session)) {
            clearLineAnchorState(session)
        }
        if (tool.mode == PaintToolMode.EASEL) {
            clearSuppression(session)
            clearStrokeState(session)
            clearLineAnchorState(session)
            restoreIfNeeded(player, session)
            return
        }
        if (shouldSkipBrushPreviewDuringActiveStroke(session, tool)) {
            restoreIfNeeded(player, session)
            return
        }

        val hit = hitDetectionService.resolveHitPixel(player, session, allowMissingCell = false, clampToCanvasBounds = true) ?: run {
            clearSuppression(session)
            clearStrokeState(session)
            restoreIfNeeded(player, session)
            return
        }

        val overlays = resolvePreviewOverlays(session, tool, hit)
        val fingerprint = overlays.joinToString("|") { overlay ->
            "${overlay.mapId}:${overlay.indices.size}:${overlay.fingerprint}"
        }
        val suppressionKey = buildPreviewSuppressionKey(tool.id, fingerprint)

        if (session.previewSuppressionKey == suppressionKey) {
            restoreIfNeeded(player, session)
            return
        }
        if (session.previewSuppressionKey != null) {
            clearSuppression(session)
        }

        if (fingerprint == session.previewFingerprint) {
            return
        }

        restoreIfNeeded(player, session)
        overlays.forEach { overlay ->
            val patch = MapDataExtractor.extractPatch(overlay.mapId, overlay.indices, overlay.colors) ?: return@forEach
            mapDataSender.send(player, patch)
        }
        session.previewMapIds = overlays.mapTo(mutableSetOf()) { it.mapId }
        session.previewFingerprint = fingerprint
        activePreviewOverlays[session.playerId] = overlays
    }

    fun restoreIfNeeded(player: Player, session: PaintSession) {
        val overlays = activePreviewOverlays.remove(session.playerId)
        if (overlays.isNullOrEmpty()) {
            if (session.previewMapIds.isEmpty()) return
            session.previewMapIds.forEach { mapId ->
                MapDataExtractor.extract(mapId)?.let { snapshot ->
                    mapDataSender.send(player, snapshot)
                }
            }
        } else {
            overlays.forEach { overlay ->
                val patch = MapDataExtractor.extractPatch(overlay.mapId, overlay.indices) ?: return@forEach
                mapDataSender.send(player, patch)
            }
        }
        session.previewMapIds.clear()
        session.previewFingerprint = null
    }

    fun clearStrokeState(session: PaintSession) {
        brushStrokeResolver.clearStrokeState(session)
        brushPreviewCache.remove(session.playerId)
    }

    fun clearLineAnchorState(session: PaintSession) {
        session.shapeLineAnchor = null
    }

    fun clearSuppression(session: PaintSession) {
        session.previewSuppressionKey = null
    }

    fun suppressLargeBrushPreview(session: PaintSession, brushSize: Int) {
        if (brushSize < LARGE_BRUSH_PREVIEW_SUPPRESS_SIZE) return
        session.brushPreviewSuppressedUntilMillis = System.currentTimeMillis() + LARGE_BRUSH_PREVIEW_SUPPRESS_MILLIS
    }

    fun clearHistoryState(session: PaintSession) {
        session.history.clear()
        session.historyBytes = 0L
        clearLineAnchorState(session)
        fillPreviewCache.remove(session.playerId)
    }

    fun markCanvasChanged(session: PaintSession) {
        session.canvasRevision += 1
        brushPreviewCache.remove(session.playerId)
        fillPreviewCache.remove(session.playerId)
        fillComponentResolver.clear(session.playerId)
    }

    fun markCanvasTopologyChanged(session: PaintSession) {
        session.canvasTopologyRevision += 1
        brushStrokeResolver.clearCachedPixels(session.playerId)
        markCanvasChanged(session)
    }

    fun clearRuntimeState(playerId: UUID) {
        fillPreviewCache.remove(playerId)
        brushPreviewCache.remove(playerId)
        activePreviewOverlays.remove(playerId)
        brushStrokeResolver.clearCachedPixels(playerId)
        fillComponentResolver.clear(playerId)
    }

    fun buildCurrentPreviewSuppressionKey(player: Player, session: PaintSession): String? {
        val tool = toolResolver(player.inventory.itemInMainHand) ?: return null
        val fingerprint = session.previewFingerprint ?: return null
        return buildPreviewSuppressionKey(tool.id, fingerprint)
    }

    fun isLineShapeTool(tool: PaintToolDefinition, session: PaintSession): Boolean {
        return tool.mode == PaintToolMode.SHAPE && session.toolSettings.shape.shapeType == PaintShapeType.LINE
    }

    private fun shouldSkipBrushPreviewDuringActiveStroke(session: PaintSession, tool: PaintToolDefinition): Boolean {
        if (System.currentTimeMillis() > session.brushPreviewSuppressedUntilMillis) return false
        return when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH,
            PaintToolMode.CUSTOM_BRUSH,
            PaintToolMode.ERASER,
            PaintToolMode.SHEARS -> true

            else -> false
        }
    }

    private fun resolvePreviewOverlays(
        session: PaintSession,
        tool: PaintToolDefinition,
        hit: PaintSurfacePixel
    ): List<PreviewMapOverlay> {
        return when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val paletteKey = requireNotNull(tool.fixedPaletteKey)
                val settings = session.toolSettings.basicBrush(paletteKey)
                val palette = PaintPalette.entry(paletteKey)
                resolveBrushPreviewOverlays(
                    session = session,
                    toolId = tool.id,
                    hit = hit,
                    size = settings.normalizedSize(),
                    previewColor = palette.packed(settings.shade)
                )
            }

            PaintToolMode.CUSTOM_BRUSH -> {
                val settings = session.toolSettings.customBrush
                resolveBrushPreviewOverlays(
                    session = session,
                    toolId = tool.id,
                    hit = hit,
                    size = settings.normalizedSize(),
                    previewColor = PaintPalette.entry(settings.paletteKey).packed(settings.shade)
                )
            }

            PaintToolMode.ERASER -> resolveBrushPreviewOverlays(
                session = session,
                toolId = tool.id,
                hit = hit,
                size = session.toolSettings.eraser.normalizedSize(),
                previewColor = backgroundColorId
            )

            PaintToolMode.SHEARS -> resolveBrushPreviewOverlays(
                session = session,
                toolId = tool.id,
                hit = hit,
                size = session.toolSettings.shears.normalizedSize(),
                previewColor = MapColorMatcher.TRANSPARENT_COLOR_ID
            )

            PaintToolMode.FILL -> resolveFillPreviewOverlays(session, hit)

            PaintToolMode.SHAPE -> {
                val settings = session.toolSettings.shape
                val shapePixels = if (settings.shapeType == PaintShapeType.LINE) {
                    val anchor = session.shapeLineAnchor ?: return emptyList()
                    shapeSurfaceResolver.resolveLinePixels(session, anchor.globalX, anchor.globalY, hit.globalX, hit.globalY, settings)
                } else {
                    shapeSurfaceResolver.resolvePixels(session, hit, settings)
                }
                canvasPixelProjector.buildPreviewOverlays(
                    session,
                    shapePixels,
                    PaintPalette.entry(settings.paletteKey).packed(settings.shade)
                )
            }

            PaintToolMode.EASEL -> emptyList()
        }
    }

    private fun resolveFillPreviewOverlays(session: PaintSession, hit: PaintSurfacePixel): List<PreviewMapOverlay> {
        val settings = session.toolSettings.fill
        val (cache, componentId) = fillComponentResolver.resolveComponent(session, hit, settings.ignoreShade) ?: return emptyList()
        val cacheKey = buildFillPreviewCacheKey(session, cache, componentId)
        fillPreviewCache[session.playerId]?.takeIf { it.key == cacheKey }?.let { return it.overlays }
        val area = fillComponentResolver.resolveArea(cache, componentId) ?: return emptyList()

        val overlays = canvasPixelProjector.buildFillPreviewOverlays(
            session = session,
            area = area,
            previewColor = PaintPalette.entry(settings.paletteKey).packed(settings.baseShade)
        )
        fillPreviewCache[session.playerId] = FillPreviewCacheEntry(cacheKey, overlays)
        return overlays
    }

    private fun resolveBrushPreviewOverlays(
        session: PaintSession,
        toolId: String,
        hit: PaintSurfacePixel,
        size: Int,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        val now = System.currentTimeMillis()
        val strokeStart = brushStrokeResolver.resolveStrokeContinuationStart(session, now)
        val cacheKey = buildBrushPreviewCacheKey(session, toolId, hit, size, previewColor, strokeStart)
        brushPreviewCache[session.playerId]?.takeIf { it.key == cacheKey }?.let { return it.overlays }

        val brushPixels = brushStrokeResolver.resolveCachedPixels(session, hit, strokeStart, size)
        val overlays = canvasPixelProjector.buildPreviewOverlays(session, brushPixels, previewColor)
        brushPreviewCache[session.playerId] = BrushPreviewCacheEntry(cacheKey, overlays)
        return overlays
    }

    private fun buildBrushPreviewCacheKey(
        session: PaintSession,
        toolId: String,
        hit: PaintSurfacePixel,
        size: Int,
        previewColor: Byte,
        strokeStart: Pair<Int, Int>?
    ): String {
        return listOf(
            session.canvasRevision,
            toolId,
            hit.globalX,
            hit.globalY,
            size,
            previewColor.toInt() and 0xFF,
            strokeStart?.first ?: "x",
            strokeStart?.second ?: "y"
        ).joinToString("|")
    }

    private fun buildFillPreviewCacheKey(
        session: PaintSession,
        cache: FillComponentCache,
        componentId: Int
    ): String {
        val settings = session.toolSettings.fill
        return listOf(
            session.canvasRevision,
            componentId,
            settings.paletteKey,
            settings.baseShade.name,
            cache.ignoreShade
        ).joinToString("|")
    }

    private fun buildPreviewSuppressionKey(toolId: String, fingerprint: String): String {
        return "$toolId|$fingerprint"
    }

    private companion object {
        private const val LARGE_BRUSH_PREVIEW_SUPPRESS_SIZE = 35
        private const val LARGE_BRUSH_PREVIEW_SUPPRESS_MILLIS = 140L
    }
}
