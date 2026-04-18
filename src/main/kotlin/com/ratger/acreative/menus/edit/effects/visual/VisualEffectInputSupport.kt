package com.ratger.acreative.menus.edit.effects.visual

import org.bukkit.potion.PotionEffect

object VisualEffectInputSupport {
    const val DEFAULT_DURATION_SECONDS: Int = 30
    const val MAX_LEVEL: Int = 255
    const val MAX_DURATION_SECONDS: Int = 7 * 24 * 60 * 60
    const val INFINITE_SECONDS: Int = -1

    fun parseLevel(raw: String?): Int? {
        val parsed = raw?.trim()?.toIntOrNull() ?: return null
        return parsed.coerceIn(1, MAX_LEVEL)
    }

    fun parseDurationSeconds(raw: String?): Int? {
        if (raw == null) return DEFAULT_DURATION_SECONDS
        val trimmed = raw.trim()
        if (trimmed.equals("inf", ignoreCase = true)) return INFINITE_SECONDS

        val value = trimmed.toLongOrNull() ?: return null
        if (value < 0L) return INFINITE_SECONDS
        if (value == 0L) return 1
        if (value > MAX_DURATION_SECONDS.toLong()) return INFINITE_SECONDS
        return value.toInt()
    }

    fun parseProbabilityPercent(raw: String?): Int? {
        if (raw == null) return 100
        val normalized = raw.removeSuffix("%").trim()
        val number = normalized.toFloatOrNull() ?: return null
        return number.toInt().coerceIn(0, 100)
    }

    fun visibleTicksFromDurationSeconds(seconds: Int): Int {
        if (seconds == INFINITE_SECONDS) return PotionEffect.INFINITE_DURATION
        return (seconds.coerceAtLeast(1).toLong() * 20L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

}
