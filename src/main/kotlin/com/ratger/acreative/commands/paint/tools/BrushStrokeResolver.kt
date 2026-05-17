package com.ratger.acreative.commands.paint.tools

import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

class BrushStrokeResolver {
    private data class BrushPixelCacheEntry(
        val key: String,
        val pixels: List<PaintSurfacePixel>
    )

    private data class BrushRowInterval(
        val startX: Int,
        val endX: Int
    )

    private val brushPixelCache = mutableMapOf<UUID, BrushPixelCacheEntry>()
    private val brushStampCache = mutableMapOf<Int, IntArray>()

    fun resolveStrokePixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        size: Int
    ): List<PaintSurfacePixel> {
        return resolveCachedPixels(
            session = session,
            hit = hit,
            strokeStart = resolveStrokeContinuationStart(session, System.currentTimeMillis()),
            size = size
        )
    }

    fun resolveStrokeContinuationStart(session: PaintSession, now: Long): Pair<Int, Int>? {
        val previousX = session.lastStrokeGlobalX
        val previousY = session.lastStrokeGlobalY
        val canContinueStroke =
            previousX != null &&
                previousY != null &&
                now - session.lastStrokeAtMillis <= STROKE_CONTINUE_WINDOW_MILLIS
        return if (canContinueStroke) previousX to previousY else null
    }

    fun resolveCachedPixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int
    ): List<PaintSurfacePixel> {
        val cacheKey = buildBrushPixelCacheKey(session, hit, strokeStart, size)
        brushPixelCache[session.playerId]?.takeIf { it.key == cacheKey }?.let { return it.pixels }
        val pixels = collectBrushPixels(session, hit, strokeStart, size)
        brushPixelCache[session.playerId] = BrushPixelCacheEntry(cacheKey, pixels)
        return pixels
    }

    fun rememberStroke(session: PaintSession, hit: PaintSurfacePixel, color: Byte) {
        session.lastStrokeGlobalX = hit.globalX
        session.lastStrokeGlobalY = hit.globalY
        session.lastStrokeColor = color
        session.lastStrokeAtMillis = System.currentTimeMillis()
    }

    fun clearStrokeState(session: PaintSession) {
        session.lastStrokeGlobalX = null
        session.lastStrokeGlobalY = null
        session.lastStrokeColor = null
        session.lastStrokeAtMillis = 0L
        session.brushPreviewSuppressedUntilMillis = 0L
    }

    fun clearCachedPixels(playerId: UUID) {
        brushPixelCache.remove(playerId)
    }

    private fun buildBrushPixelCacheKey(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int
    ): String {
        return listOf(
            session.canvasTopologyRevision,
            hit.globalX,
            hit.globalY,
            size,
            strokeStart?.first ?: "x",
            strokeStart?.second ?: "y"
        ).joinToString("|")
    }

    private fun collectBrushPixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int
    ): List<PaintSurfacePixel> {
        if (strokeStart != null) {
            return collectBrushLinePixels(session, strokeStart.first, strokeStart.second, hit.globalX, hit.globalY, size)
        }
        return collectBrushStampPixels(session, hit.globalX, hit.globalY, size)
    }

    private fun collectBrushStampPixels(
        session: PaintSession,
        centerX: Int,
        centerY: Int,
        size: Int
    ): List<PaintSurfacePixel> {
        val offsets = brushStampOffsets(size)
        val pixels = linkedMapOf<Int, PaintSurfacePixel>()
        var offsetIndex = 0
        while (offsetIndex < offsets.size) {
            resolvePixelByGlobal(session, centerX + offsets[offsetIndex], centerY + offsets[offsetIndex + 1])?.let { pixel ->
                pixels.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
            }
            offsetIndex += 2
        }
        return pixels.values.toList()
    }

    private fun collectBrushLinePixels(
        session: PaintSession,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        size: Int
    ): List<PaintSurfacePixel> {
        val radius = max(0, size.coerceIn(1, 50) - 1)
        if (radius == 0) {
            val pixels = linkedMapOf<Int, PaintSurfacePixel>()
            traceLine(startX, startY, endX, endY).forEach { (globalX, globalY) ->
                resolvePixelByGlobal(session, globalX, globalY)?.let { pixel ->
                    pixels.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
            return pixels.values.toList()
        }
        if (startX == endX && startY == endY) {
            return collectBrushStampPixels(session, endX, endY, size)
        }

        val brushRadius = radius.toDouble()
        val pixels = linkedMapOf<Int, PaintSurfacePixel>()
        for (globalY in minOf(startY, endY) - radius..maxOf(startY, endY) + radius) {
            val intervals = mutableListOf<BrushRowInterval>()
            addCircleRowInterval(intervals, startX, startY, globalY, brushRadius)
            addCircleRowInterval(intervals, endX, endY, globalY, brushRadius)
            addSegmentBodyRowInterval(intervals, startX, startY, endX, endY, globalY, brushRadius)
            putMergedBrushRowIntervals(session, pixels, globalY, intervals)
        }
        return pixels.values.toList()
    }

    private fun addCircleRowInterval(
        intervals: MutableList<BrushRowInterval>,
        centerX: Int,
        centerY: Int,
        rowY: Int,
        radius: Double
    ) {
        val deltaY = rowY - centerY
        val remaining = radius * radius - deltaY * deltaY
        if (remaining < 0.0) return
        val span = sqrt(remaining)
        intervals += BrushRowInterval(
            startX = ceil(centerX - span - ROW_INTERVAL_EPSILON).toInt(),
            endX = floor(centerX + span + ROW_INTERVAL_EPSILON).toInt()
        )
    }

    private fun addSegmentBodyRowInterval(
        intervals: MutableList<BrushRowInterval>,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        rowY: Int,
        radius: Double
    ) {
        val segmentX = (endX - startX).toDouble()
        val segmentY = (endY - startY).toDouble()
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared <= 0.0) return

        val rowDeltaY = (rowY - startY).toDouble()
        var minX = Double.NEGATIVE_INFINITY
        var maxX = Double.POSITIVE_INFINITY

        fun intersectLinearRange(coefficient: Double, constant: Double, rangeStart: Double, rangeEnd: Double): Boolean {
            if (abs(coefficient) <= ROW_INTERVAL_EPSILON) {
                return constant + ROW_INTERVAL_EPSILON >= rangeStart && constant - ROW_INTERVAL_EPSILON <= rangeEnd
            }
            val first = (rangeStart - constant) / coefficient
            val second = (rangeEnd - constant) / coefficient
            minX = max(minX, minOf(first, second))
            maxX = minOf(maxX, maxOf(first, second))
            return minX <= maxX + ROW_INTERVAL_EPSILON
        }

        val projectionConstant = -startX * segmentX + rowDeltaY * segmentY
        if (!intersectLinearRange(segmentX, projectionConstant, 0.0, lengthSquared)) return

        val length = sqrt(lengthSquared)
        val perpendicularConstant = -startX * segmentY - rowDeltaY * segmentX
        if (!intersectLinearRange(segmentY, perpendicularConstant, -radius * length, radius * length)) return

        intervals += BrushRowInterval(
            startX = ceil(minX - ROW_INTERVAL_EPSILON).toInt(),
            endX = floor(maxX + ROW_INTERVAL_EPSILON).toInt()
        )
    }

    private fun putMergedBrushRowIntervals(
        session: PaintSession,
        pixels: MutableMap<Int, PaintSurfacePixel>,
        globalY: Int,
        intervals: MutableList<BrushRowInterval>
    ) {
        if (intervals.isEmpty()) return
        intervals.sortBy { it.startX }
        var currentStart = intervals.first().startX
        var currentEnd = intervals.first().endX
        for (index in 1 until intervals.size) {
            val interval = intervals[index]
            if (interval.startX <= currentEnd + 1) {
                currentEnd = max(currentEnd, interval.endX)
                continue
            }
            putBrushRow(session, pixels, globalY, currentStart, currentEnd)
            currentStart = interval.startX
            currentEnd = interval.endX
        }
        putBrushRow(session, pixels, globalY, currentStart, currentEnd)
    }

    private fun putBrushRow(
        session: PaintSession,
        pixels: MutableMap<Int, PaintSurfacePixel>,
        globalY: Int,
        startX: Int,
        endX: Int
    ) {
        if (endX < startX) return
        for (globalX in startX..endX) {
            resolvePixelByGlobal(session, globalX, globalY)?.let { pixel ->
                pixels.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
            }
        }
    }

    private fun brushStampOffsets(size: Int): IntArray {
        val normalizedSize = size.coerceIn(1, 50)
        brushStampCache[normalizedSize]?.let { return it }
        val radius = max(0, normalizedSize - 1)
        val offsets = mutableListOf<Int>()
        for (offsetY in -radius..radius) {
            for (offsetX in -radius..radius) {
                if (offsetX * offsetX + offsetY * offsetY > radius * radius) continue
                offsets += offsetX
                offsets += offsetY
            }
        }
        return offsets.toIntArray().also { brushStampCache[normalizedSize] = it }
    }

    private fun resolvePixelByGlobal(session: PaintSession, globalX: Int, globalY: Int): PaintSurfacePixel? {
        val cellX = Math.floorDiv(globalX, MAP_WIDTH)
        val cellY = Math.floorDiv(globalY, MAP_HEIGHT)
        val point = PaintGridPoint(cellX, cellY)
        if (!session.logicalCells.containsKey(point)) return null
        val localX = Math.floorMod(globalX, MAP_WIDTH)
        val localY = Math.floorMod(globalY, MAP_HEIGHT)
        return PaintSurfacePixel(point, localX, localY, globalX, globalY)
    }

    private fun traceLine(startX: Int, startY: Int, endX: Int, endY: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        var x = startX
        var y = startY
        val dx = abs(endX - startX)
        val dy = abs(endY - startY)
        val sx = if (startX < endX) 1 else -1
        val sy = if (startY < endY) 1 else -1
        var error = dx - dy

        while (true) {
            points += x to y
            if (x == endX && y == endY) {
                return points
            }

            val doubledError = error * 2
            if (doubledError > -dy) {
                error -= dy
                x += sx
            }
            if (doubledError < dx) {
                error += dx
                y += sy
            }
        }
    }

    private companion object {
        const val MAP_WIDTH = 128
        const val MAP_HEIGHT = 128
        const val ROW_INTERVAL_EPSILON = 1.0E-9
        const val STROKE_CONTINUE_WINDOW_MILLIS = 250L
        const val GLOBAL_CANVAS_HASH_BASE = 100_000
    }
}
