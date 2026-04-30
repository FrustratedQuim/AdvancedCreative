package com.ratger.acreative.paint.model

import com.ratger.acreative.paint.palette.PaintPalette

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
    var paletteKey: String = PaintPalette.COLOR_BLACK.key,
    var baseShade: PaintShade = PaintShade.NORMAL,
    val shadeMix: MutableSet<PaintShade> = linkedSetOf(PaintShade.NORMAL),
    var shadeMixFocusIndex: Int = 0,
    var ignoreShade: Boolean = false
) {
    fun normalizedFillPercent(): Int = fillPercent.coerceIn(1, 100)

    fun normalizedShadeMix(): Set<PaintShade> {
        if (shadeMix.isEmpty()) {
            shadeMix += baseShade
        }
        return PaintShade.ordered.filterTo(linkedSetOf()) { it in shadeMix }
    }

    fun applyShadeSelection(newShade: PaintShade) {
        val selectedBefore = normalizedShadeMix()
        val onlyBaseWasSelected = selectedBefore.size == 1 && selectedBefore.first() == baseShade
        baseShade = newShade
        if (onlyBaseWasSelected) {
            shadeMix.clear()
            shadeMix += newShade
        }
    }
}

data class PaintShapeSettings(
    var size: Int = 1,
    var fillPercent: Int = 100,
    var paletteKey: String = PaintPalette.COLOR_BLACK.key,
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
    val basicBrushes: MutableMap<String, PaintBrushSettings> = mutableMapOf(),
    val eraser: PaintBinaryBrushSettings = PaintBinaryBrushSettings(),
    val shears: PaintBinaryBrushSettings = PaintBinaryBrushSettings(),
    val fill: PaintFillSettings = PaintFillSettings(),
    val customBrush: PaintBrushSettings = PaintBrushSettings(),
    val shape: PaintShapeSettings = PaintShapeSettings()
) {
    fun basicBrush(paletteKey: String): PaintBrushSettings {
        return basicBrushes.getOrPut(paletteKey) {
            PaintBrushSettings(paletteKey = paletteKey)
        }
    }
}

enum class PaintInputKind {
    DIRECT_USE,
    DROP_SINGLE,
    DROP_STACK
}
