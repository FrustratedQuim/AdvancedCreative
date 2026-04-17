package com.ratger.acreative.menus.decorationheads.support

import org.bukkit.Color

data class MapPreviewColorOption(
    val key: String,
    val label: String,
    val labelMiniColor: String,
    val color: Color?
)

object MapPreviewColorPalette {
    const val ORDINARY_KEY: String = "ordinary"

    val options: List<MapPreviewColorOption> = listOf(
        MapPreviewColorOption(ORDINARY_KEY, "Обычный", "#FFF3E0", null),
        MapPreviewColorOption("white", "Белый", "#FFFFFF", Color.WHITE),
        MapPreviewColorOption("light_gray", "Светло-серый", "#D3D3D3", Color.fromRGB(0xD3D3D3)),
        MapPreviewColorOption("gray", "Серый", "#808080", Color.GRAY),
        MapPreviewColorOption("black", "Чёрный", "#1E1E1E", Color.BLACK),
        MapPreviewColorOption("yellow", "Жёлтый", "#FFD600", Color.YELLOW),
        MapPreviewColorOption("orange", "Оранжевый", "#FF7A00", Color.ORANGE),
        MapPreviewColorOption("red", "Красный", "#FF2B2B", Color.RED),
        MapPreviewColorOption("dark_red", "Тёмно-красный", "#8B0000", Color.fromRGB(0x8B0000)),
        MapPreviewColorOption("lime", "Лаймовый", "#7BFF00", Color.LIME),
        MapPreviewColorOption("green", "Зелёный", "#00C853", Color.GREEN),
        MapPreviewColorOption("light_blue", "Голубой", "#33C3FF", Color.fromRGB(0x33C3FF)),
        MapPreviewColorOption("turquoise", "Бирюзовый", "#00CFC1", Color.fromRGB(0x00CFC1)),
        MapPreviewColorOption("blue", "Синий", "#005BFF", Color.BLUE),
        MapPreviewColorOption("dark_blue", "Тёмно-синий", "#002FA7", Color.fromRGB(0x002FA7)),
        MapPreviewColorOption("pink", "Розовый", "#FF4DB8", Color.fromRGB(0xFF4DB8)),
        MapPreviewColorOption("purple", "Фиолетовый", "#A020F0", Color.PURPLE)
    )

    private val byKey: Map<String, MapPreviewColorOption> = options.associateBy { it.key }

    fun normalizeKey(key: String?): String = byKey[key]?.key ?: ORDINARY_KEY
    fun nextKey(current: String?): String {
        val index = options.indexOfFirst { it.key == normalizeKey(current) }.coerceAtLeast(0)
        return options[(index + 1) % options.size].key
    }

    fun previousKey(current: String?): String {
        val index = options.indexOfFirst { it.key == normalizeKey(current) }.coerceAtLeast(0)
        return options[(index - 1 + options.size) % options.size].key
    }

    fun colorFor(key: String?): Color? = byKey[normalizeKey(key)]?.color
    fun optionFor(key: String?): MapPreviewColorOption = byKey[normalizeKey(key)] ?: options.first()
}
