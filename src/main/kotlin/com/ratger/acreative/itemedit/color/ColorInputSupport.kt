package com.ratger.acreative.itemedit.color

import org.bukkit.Color

object ColorInputSupport {
    private val aliasToHex: Map<String, String> = linkedMapOf(
        "lime" to "#7BFF00",
        "green" to "#00C853",
        "dark_green" to "#008F39",
        "yellow" to "#FFD600",
        "orange" to "#FF7A00",
        "red" to "#FF2B2B",
        "dark_red" to "#B00020",
        "light_blue" to "#33C3FF",
        "blue" to "#005BFF",
        "dark_blue" to "#002FA7",
        "pink" to "#FF4DB8",
        "purple" to "#A020F0",
        "dark_purple" to "#6A00FF",
        "white" to "#FFFFFF",
        "gray" to "#808080",
        "black" to "#000000"
    )

    fun parseColor(input: String): Color? {
        val normalized = input.trim().lowercase()
        val hex = aliasToHex[normalized] ?: normalizeHex(input) ?: return null
        return Color.fromRGB(hex.substring(1).toInt(16))
    }

    fun normalizeHex(input: String): String? {
        val value = input.trim()
        if (!value.startsWith("#") || value.length != 7) return null
        val hexPart = value.substring(1)
        if (!hexPart.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return "#${hexPart.uppercase()}"
    }

    fun normalizeHex(color: Color): String {
        return "#%06X".format(color.asRGB() and 0xFFFFFF)
    }

    fun suggestions(prefix: String): List<String> {
        return aliasToHex.keys.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
