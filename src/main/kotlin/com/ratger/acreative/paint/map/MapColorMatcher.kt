package com.ratger.acreative.paint.map

import net.minecraft.world.level.material.MapColor
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object MapColorMatcher {

    const val TRANSPARENT_COLOR_ID: Byte = 0

    private val palette = Array(256) { packedId ->
        Color(MapColor.getColorFromPackedId(packedId), true)
    }
    private val cache = ConcurrentHashMap<Int, Byte>()

    fun match(color: Color): Byte {
        if (color.alpha < MIN_VISIBLE_ALPHA) {
            return TRANSPARENT_COLOR_ID
        }

        val rgb = color.rgb and RGB_MASK
        return cache.getOrPut(rgb) { findNearestPackedColor(color) }
    }

    private fun findNearestPackedColor(color: Color): Byte {
        var bestId = 0
        var bestDistance = Double.MAX_VALUE

        for (packedId in palette.indices) {
            val candidate = palette[packedId]
            if (candidate.alpha < MIN_VISIBLE_ALPHA) {
                continue
            }

            val distance = weightedDistance(color, candidate)
            if (distance < bestDistance) {
                bestDistance = distance
                bestId = packedId
            }
        }

        return bestId.toByte()
    }

    private fun weightedDistance(first: Color, second: Color): Double {
        val redMean = (first.red + second.red) / 2.0
        val redDelta = (first.red - second.red).toDouble()
        val greenDelta = (first.green - second.green).toDouble()
        val blueDelta = (first.blue - second.blue).toDouble()

        return ((512.0 + redMean) * redDelta * redDelta) / 256.0 +
            4.0 * greenDelta * greenDelta +
            ((767.0 - redMean) * blueDelta * blueDelta) / 256.0
    }

    private const val MIN_VISIBLE_ALPHA = 128
    private const val RGB_MASK = 0xFFFFFF
}
