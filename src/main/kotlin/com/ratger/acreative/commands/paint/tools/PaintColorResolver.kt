package com.ratger.acreative.commands.paint.tools

import com.ratger.acreative.commands.paint.map.MapColorMatcher
import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintShade
import com.ratger.acreative.commands.paint.palette.PaintPaletteEntry
import java.awt.Color
import kotlin.random.Random

class PaintColorResolver {

    private val previewBlendCache = mutableMapOf<Int, Byte>()

    fun resolveMixedPaletteColors(
        palette: PaintPaletteEntry,
        baseShade: PaintShade,
        mixedShades: Set<PaintShade>
    ): ByteArray {
        if (palette.isTransparent) return byteArrayOf(MapColorMatcher.TRANSPARENT_COLOR_ID)
        val available = mixedShades.ifEmpty { setOf(baseShade) }
        val colors = ByteArray(available.size)
        var index = 0
        available.forEach { shade ->
            colors[index++] = palette.packed(shade)
        }
        return colors
    }

    fun randomColor(colors: ByteArray): Byte {
        return if (colors.size == 1) colors[0] else colors[Random.nextInt(colors.size)]
    }

    fun blendPreviewColor(baseColorId: Byte, brushColorId: Byte): Byte {
        if (brushColorId == MapColorMatcher.TRANSPARENT_COLOR_ID) {
            return brushColorId
        }
        val cacheKey = ((baseColorId.toInt() and 0xFF) shl 8) or (brushColorId.toInt() and 0xFF)
        previewBlendCache[cacheKey]?.let { return it }
        val base = runCatching { MapDataExtractor.resolvePaletteColor(baseColorId) }.getOrNull() ?: return brushColorId
        val brush = runCatching { MapDataExtractor.resolvePaletteColor(brushColorId) }.getOrNull() ?: return brushColorId
        if (base.rgb == brush.rgb) return baseColorId.also { previewBlendCache[cacheKey] = it }

        val red = (base.red * (1.0 - PREVIEW_ALPHA) + brush.red * PREVIEW_ALPHA).toInt().coerceIn(0, 255)
        val green = (base.green * (1.0 - PREVIEW_ALPHA) + brush.green * PREVIEW_ALPHA).toInt().coerceIn(0, 255)
        val blue = (base.blue * (1.0 - PREVIEW_ALPHA) + brush.blue * PREVIEW_ALPHA).toInt().coerceIn(0, 255)
        return MapColorMatcher.match(Color(red, green, blue)).also { previewBlendCache[cacheKey] = it }
    }

    private companion object {
        private const val PREVIEW_ALPHA = 0.42
    }
}
