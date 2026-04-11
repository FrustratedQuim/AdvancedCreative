package com.ratger.acreative.itemedit.text

data class TextStyleOption(
    val key: String,
    val maleLabel: String,
    val femaleLabel: String
)

object TextStylePalette {
    val colors: List<TextStyleOption> = listOf(
        TextStyleOption("white", "Белый", "Белая"),
        TextStyleOption("gray", "Светло-серый", "Светло-серая"),
        TextStyleOption("dark_gray", "Серый", "Серая"),
        TextStyleOption("black", "Чёрный", "Чёрная"),
        TextStyleOption("yellow", "Жёлтый", "Жёлтая"),
        TextStyleOption("gold", "Оранжевый", "Оранжевая"),
        TextStyleOption("red", "Красный", "Красная"),
        TextStyleOption("dark_red", "Тёмно-красный", "Тёмно-красная"),
        TextStyleOption("green", "Лаймовый", "Лаймовая"),
        TextStyleOption("dark_green", "Зелёный", "Зелёная"),
        TextStyleOption("aqua", "Голубой", "Голубая"),
        TextStyleOption("dark_aqua", "Бирюзовый", "Бирюзовая"),
        TextStyleOption("blue", "Синий", "Синяя"),
        TextStyleOption("dark_blue", "Тёмно-синий", "Тёмно-синяя"),
        TextStyleOption("light_purple", "Розовый", "Розовая"),
        TextStyleOption("dark_purple", "Фиолетовый", "Фиолетовая")
    )

    val shadowOptions: List<TextStyleOption?> = listOf(null) + colors
}
