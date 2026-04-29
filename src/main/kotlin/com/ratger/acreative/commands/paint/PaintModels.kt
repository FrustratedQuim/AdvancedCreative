package com.ratger.acreative.commands.paint

import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Location
import java.util.ArrayDeque
import java.util.UUID

data class PaintCanvasSize(
    val width: Int,
    val height: Int
) {
    init {
        require(width in 1..4) { "Width must be between 1 and 4" }
        require(height in 1..4) { "Height must be between 1 and 4" }
    }

    val basePoint: PaintGridPoint
        get() = PaintGridPoint((width - 1) / 2, height - 1)

    fun initialPoints(): List<PaintGridPoint> {
        return buildList {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    add(PaintGridPoint(x, y))
                }
            }
        }
    }

    fun normalized(): String = "${width}x${height}"

    companion object {
        val DEFAULT = PaintCanvasSize(1, 1)
        val TAB_SUGGESTIONS: List<String> = buildList {
            for (width in 1..4) {
                for (height in 1..4) {
                    add("${width}x${height}")
                }
            }
        }

        fun parse(raw: String?): PaintCanvasSize? {
            if (raw.isNullOrBlank()) return DEFAULT
            val match = SIZE_REGEX.matchEntire(raw.trim().lowercase()) ?: return null
            val width = match.groupValues[1].toIntOrNull() ?: return null
            val height = match.groupValues[2].toIntOrNull() ?: return null
            if (width !in 1..4 || height !in 1..4) return null
            return PaintCanvasSize(width, height)
        }

        private val SIZE_REGEX = Regex("""^(\d+)x(\d+)$""")
    }
}

data class PaintGridPoint(
    val x: Int,
    val y: Int
)

data class PaintCanvasBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
) {
    val width: Int
        get() = maxX - minX + 1

    val height: Int
        get() = maxY - minY + 1

    companion object {
        fun from(points: Collection<PaintGridPoint>): PaintCanvasBounds? {
            if (points.isEmpty()) return null
            return PaintCanvasBounds(
                minX = points.minOf { it.x },
                maxX = points.maxOf { it.x },
                minY = points.minOf { it.y },
                maxY = points.maxOf { it.y }
            )
        }
    }
}

data class PaintSurfacePixel(
    val cellPoint: PaintGridPoint,
    val localX: Int,
    val localY: Int,
    val globalX: Int,
    val globalY: Int
)

data class PaintCanvasCell(
    val point: PaintGridPoint,
    val mapId: Int,
    val frame: WrapperEntity,
    val location: Location
)

data class PaintPixelChange(
    val x: Int,
    val y: Int,
    val oldColor: Byte,
    val newColor: Byte
)

data class PaintHistoryEntry(
    val changesByMapId: Map<Int, List<PaintPixelChange>>
)

enum class PaintMenuKind {
    BASIC_BRUSH,
    ERASER,
    FILL,
    CUSTOM_BRUSH,
    SHAPE,
    EASEL,
    COLOR_PICKER
}

enum class PaintToolMode {
    BASIC_COLOR_BRUSH,
    CUSTOM_BRUSH,
    ERASER,
    SHEARS,
    FILL,
    SHAPE,
    EASEL
}

enum class PaintShapeType(val displayName: String) {
    SQUARE("Квадрат"),
    TRIANGLE("Треугольник"),
    STAR("Звезда"),
    LINE("Линия"),
    CIRCLE("Круг")
}

enum class PaintShade(
    val actualId: Int,
    val mixNumber: Int,
    val displayName: String
) {
    NORMAL(2, 1, "Обычный"),
    SLIGHTLY_DARKER(1, 2, "Чуть темнее"),
    DARK(0, 3, "Тёмный"),
    DARKEST(3, 4, "Самый тёмный");

    companion object {
        val ordered: List<PaintShade> = listOf(NORMAL, SLIGHTLY_DARKER, DARK, DARKEST)

    }
}

data class PaintBrushSettings(
    var size: Int = 1,
    var fillPercent: Int = 100,
    var shade: PaintShade = PaintShade.NORMAL,
    val shadeMix: MutableSet<PaintShade> = linkedSetOf(PaintShade.NORMAL),
    var shadeMixFocusIndex: Int = 0,
    var paletteKey: String = PaintPalette.SNOW.key
) {
    fun normalizedSize(): Int = size.coerceIn(1, 50)

    fun normalizedFillPercent(): Int = fillPercent.coerceIn(1, 100)

    fun normalizedShadeMix(): Set<PaintShade> {
        if (shadeMix.isEmpty()) {
            shadeMix += shade
        }
        return PaintShade.ordered.filterTo(linkedSetOf()) { it in shadeMix }
    }

    fun applyShadeSelection(newShade: PaintShade) {
        val selectedBefore = normalizedShadeMix()
        val onlyBaseWasSelected = selectedBefore.size == 1 && selectedBefore.first() == shade
        shade = newShade
        if (onlyBaseWasSelected) {
            shadeMix.clear()
            shadeMix += newShade
        }
    }
}

data class PaintBinaryBrushSettings(
    var size: Int = 1,
    var fillPercent: Int = 100
) {
    fun normalizedSize(): Int = size.coerceIn(1, 50)

    fun normalizedFillPercent(): Int = fillPercent.coerceIn(1, 100)
}

data class PaintFillSettings(
    var fillPercent: Int = 100,
    var paletteKey: String = PaintPalette.SNOW.key,
    var baseShade: PaintShade = PaintShade.NORMAL,
    val shadeMix: MutableSet<PaintShade> = linkedSetOf(PaintShade.NORMAL),
    var shadeMixFocusIndex: Int = 0,
    var ignoreShade: Boolean = false
) {
    fun normalizedFillPercent(): Int = fillPercent.coerceIn(1, 100)

    fun normalizedShadeMix(): Set<PaintShade> {
        if (shadeMix.isEmpty() || baseShade !in shadeMix) {
            shadeMix += baseShade
        }
        return PaintShade.ordered.filterTo(linkedSetOf()) { it in shadeMix }
    }
}

data class PaintShapeSettings(
    var size: Int = 1,
    var fillPercent: Int = 100,
    var paletteKey: String = PaintPalette.SNOW.key,
    var shade: PaintShade = PaintShade.NORMAL,
    val shadeMix: MutableSet<PaintShade> = linkedSetOf(PaintShade.NORMAL),
    var shadeMixFocusIndex: Int = 0,
    var shapeType: PaintShapeType = PaintShapeType.SQUARE,
    var filled: Boolean = false
) {
    fun normalizedSize(): Int = size.coerceIn(1, 50)

    fun normalizedFillPercent(): Int = fillPercent.coerceIn(1, 100)

    fun normalizedShadeMix(): Set<PaintShade> {
        if (shadeMix.isEmpty()) {
            shadeMix += shade
        }
        return PaintShade.ordered.filterTo(linkedSetOf()) { it in shadeMix }
    }

    fun applyShadeSelection(newShade: PaintShade) {
        val selectedBefore = normalizedShadeMix()
        val onlyBaseWasSelected = selectedBefore.size == 1 && selectedBefore.first() == shade
        shade = newShade
        if (onlyBaseWasSelected) {
            shadeMix.clear()
            shadeMix += newShade
        }
    }
}

data class PaintToolSettingsBundle(
    val basicBrush: PaintBrushSettings = PaintBrushSettings(),
    val eraser: PaintBinaryBrushSettings = PaintBinaryBrushSettings(),
    val shears: PaintBinaryBrushSettings = PaintBinaryBrushSettings(),
    val fill: PaintFillSettings = PaintFillSettings(),
    val customBrush: PaintBrushSettings = PaintBrushSettings(),
    val shape: PaintShapeSettings = PaintShapeSettings()
)

data class PaintResizePreview(
    val frame: WrapperEntity,
    val teamName: String,
    val viewers: MutableSet<UUID> = mutableSetOf(),
    var targetPoint: PaintGridPoint? = null,
    var glowingGreen: Boolean = true
)

data class PaintSession(
    val playerId: UUID,
    val frameDirection: PaintFrameDirection,
    val anchorLocation: Location,
    val anchorPoint: PaintGridPoint,
    val initialSize: PaintCanvasSize,
    val inventorySnapshot: PaintInventorySnapshot,
    var viewerTaskId: Int,
    var previewTaskId: Int,
    val viewers: MutableSet<UUID>,
    val canvasCells: MutableMap<PaintGridPoint, PaintCanvasCell>,
    val toolSettings: PaintToolSettingsBundle = PaintToolSettingsBundle(),
    val history: ArrayDeque<PaintHistoryEntry> = ArrayDeque(),
    val seriesCode: String,
    var previewMapIds: MutableSet<Int> = mutableSetOf(),
    var previewFingerprint: String? = null,
    var lastDirectUseAtMillis: Long = 0L,
    var lastStrokeGlobalX: Int? = null,
    var lastStrokeGlobalY: Int? = null,
    var lastStrokeColor: Byte? = null,
    var lastStrokeAtMillis: Long = 0L,
    var isMenuOpen: Boolean = false,
    var openMenuKind: PaintMenuKind? = null,
    var activeColorMenuReturnTo: PaintMenuKind? = null,
    var resizeMode: Boolean = false,
    var resizePreview: PaintResizePreview? = null,
    var currentTick: Long = 0L,
    var fillCooldownUntilTick: Long = 0L,
    var paletteRotation: Int = 0
) {
    fun cellsSortedTopLeft(): List<PaintCanvasCell> {
        return canvasCells.values.sortedWith(compareBy<PaintCanvasCell> { it.point.y }.thenBy { it.point.x })
    }

}
