package com.ratger.acreative.itemedit.potion

import com.ratger.acreative.itemedit.color.ColorInputSupport
import org.bukkit.Color

object PotionColorSupport {
    fun parseColor(input: String): Color? = ColorInputSupport.parseColor(input)

    fun normalizeHex(input: String): String? = ColorInputSupport.normalizeHex(input)

    fun normalizeHex(color: Color): String = ColorInputSupport.normalizeHex(color)

    fun suggestions(prefix: String): List<String> = ColorInputSupport.suggestions(prefix)
}
