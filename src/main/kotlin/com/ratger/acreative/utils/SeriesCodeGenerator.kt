package com.ratger.acreative.utils

import kotlin.random.Random

object SeriesCodeGenerator {
    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun generate(length: Int = 6): String {
        val safeLength = length.coerceAtLeast(1)
        val suffix = (1..safeLength)
            .map { CHARSET[Random.nextInt(CHARSET.length)] }
            .joinToString("")
        return "#$suffix"
    }
}
