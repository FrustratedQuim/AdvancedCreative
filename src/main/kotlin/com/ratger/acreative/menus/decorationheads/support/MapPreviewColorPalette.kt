package com.ratger.acreative.menus.decorationheads.support

import org.bukkit.Color

data class MapPreviewColorOption(
    val key: String,
    val label: String,
    val legacyMiniColorTag: String,
    val labelMiniColor: String,
    val color: Color?
)

object MapPreviewColorPalette {
    const val ORDINARY_KEY: String = "ordinary"

    val options: List<MapPreviewColorOption> = listOf(
        MapPreviewColorOption(ORDINARY_KEY, "Обычный", "white", "#FFF3E0", null),
        MapPreviewColorOption("white", "Белый", "white", "#FFFFFF", Color.WHITE),
        MapPreviewColorOption("light_gray", "Светло-серый", "gray", "#D3D3D3", Color.fromRGB(0xD3D3D3)),
        MapPreviewColorOption("gray", "Серый", "dark_gray", "#808080", Color.GRAY),
        MapPreviewColorOption("black", "Чёрный", "black", "#1E1E1E", Color.BLACK),
        MapPreviewColorOption("yellow", "Жёлтый", "yellow", "#FFD600", Color.YELLOW),
        MapPreviewColorOption("orange", "Оранжевый", "gold", "#FF7A00", Color.ORANGE),
        MapPreviewColorOption("red", "Красный", "red", "#FF2B2B", Color.RED),
        MapPreviewColorOption("dark_red", "Тёмно-красный", "dark_red", "#8B0000", Color.fromRGB(0x8B0000)),
        MapPreviewColorOption("lime", "Лаймовый", "green", "#7BFF00", Color.LIME),
        MapPreviewColorOption("green", "Зелёный", "dark_green", "#00C853", Color.GREEN),
        MapPreviewColorOption("light_blue", "Голубой", "aqua", "#33C3FF", Color.fromRGB(0x33C3FF)),
        MapPreviewColorOption("turquoise", "Бирюзовый", "aqua", "#00CFC1", Color.fromRGB(0x00CFC1)),
        MapPreviewColorOption("blue", "Синий", "blue", "#005BFF", Color.BLUE),
        MapPreviewColorOption("dark_blue", "Тёмно-синий", "dark_blue", "#002FA7", Color.fromRGB(0x002FA7)),
        MapPreviewColorOption("pink", "Розовый", "light_purple", "#FF4DB8", Color.fromRGB(0xFF4DB8)),
        MapPreviewColorOption("purple", "Фиолетовый", "dark_purple", "#A020F0", Color.PURPLE)
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
