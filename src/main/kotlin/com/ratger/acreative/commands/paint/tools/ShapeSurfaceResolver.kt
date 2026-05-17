package com.ratger.acreative.commands.paint.tools

import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintShapeSettings
import com.ratger.acreative.commands.paint.model.PaintShapeType
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class ShapeSurfaceResolver {
    fun resolvePixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintShapeSettings
    ): List<PaintSurfacePixel> {
        if (settings.shapeType == PaintShapeType.LINE) {
            return emptyList()
        }
        val radius = max(1, settings.normalizedSize())
        val searchRadius = shapeSearchRadius(settings, radius)
        val results = linkedMapOf<Int, PaintSurfacePixel>()
        for (offsetY in -searchRadius..searchRadius) {
            for (offsetX in -searchRadius..searchRadius) {
                if (!shapeContains(settings, radius, offsetX, offsetY)) continue
                resolvePixelByGlobal(session, hit.globalX + offsetX, hit.globalY + offsetY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
        }
        return results.values.toList()
    }

    fun resolveLinePixels(
        session: PaintSession,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        settings: PaintShapeSettings
    ): List<PaintSurfacePixel> {
        val results = linkedMapOf<Int, PaintSurfacePixel>()
        val radius = max(0, settings.normalizedSize() - 1)
        if (radius == 0) {
            traceLine(startX, startY, endX, endY).forEach { (globalX, globalY) ->
                resolvePixelByGlobal(session, globalX, globalY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
            return results.values.toList()
        }

        val radiusSquared = radius.toDouble() * radius.toDouble()
        val innerRadius = if (radius <= 1) 0.5 else radius - 1.0
        val innerRadiusSquared = innerRadius * innerRadius
        for (globalY in minOf(startY, endY) - radius..maxOf(startY, endY) + radius) {
            for (globalX in minOf(startX, endX) - radius..maxOf(startX, endX) + radius) {
                val distanceSquared = squaredDistanceToSegment(globalX, globalY, startX, startY, endX, endY)
                val contains = if (settings.filled) {
                    distanceSquared <= radiusSquared
                } else {
                    distanceSquared <= radiusSquared && distanceSquared >= innerRadiusSquared
                }
                if (!contains) continue
                resolvePixelByGlobal(session, globalX, globalY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
        }
        return results.values.toList()
    }

    private fun shapeSearchRadius(settings: PaintShapeSettings, radius: Int): Int {
        if (settings.shapeType != PaintShapeType.STAR || settings.filled) {
            return radius
        }
        return radius + ceil(max(1.0, radius * STAR_OUTLINE_WIDTH_RATIO)).toInt()
    }

    private fun squaredDistanceToSegment(
        pointX: Int,
        pointY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int
    ): Double {
        val segmentX = (endX - startX).toDouble()
        val segmentY = (endY - startY).toDouble()
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared == 0.0) {
            val dx = pointX - startX
            val dy = pointY - startY
            return (dx * dx + dy * dy).toDouble()
        }
        val projection = (((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared)
            .coerceIn(0.0, 1.0)
        val closestX = startX + projection * segmentX
        val closestY = startY + projection * segmentY
        val dx = pointX - closestX
        val dy = pointY - closestY
        return dx * dx + dy * dy
    }

    private fun shapeContains(
        settings: PaintShapeSettings,
        radius: Int,
        offsetX: Int,
        offsetY: Int
    ): Boolean {
        return when (settings.shapeType) {
            PaintShapeType.SQUARE -> {
                if (settings.filled) {
                    abs(offsetX) <= radius && abs(offsetY) <= radius
                } else {
                    abs(offsetX) == radius || abs(offsetY) == radius
                }
            }

            PaintShapeType.CIRCLE -> {
                val distance = sqrt((offsetX * offsetX + offsetY * offsetY).toDouble())
                if (settings.filled) {
                    distance <= radius
                } else {
                    distance in (radius - 0.9)..(radius + 0.9)
                }
            }

            PaintShapeType.LINE -> false

            PaintShapeType.TRIANGLE -> {
                val normalizedY = offsetY + radius
                if (normalizedY < 0 || normalizedY > radius * 2) {
                    false
                } else {
                    val width = normalizedY / 2.0
                    if (settings.filled) {
                        abs(offsetX) <= width
                    } else {
                        abs(abs(offsetX) - width) <= 0.8 || normalizedY == radius * 2
                    }
                }
            }

            PaintShapeType.STAR -> starContains(settings, radius, offsetX, offsetY)
        }
    }

    private fun starContains(
        settings: PaintShapeSettings,
        radius: Int,
        offsetX: Int,
        offsetY: Int
    ): Boolean {
        val vertices = regularStarVertices(radius.toDouble())
        val x = offsetX.toDouble()
        val y = offsetY.toDouble()
        if (settings.filled) {
            return pointInPolygon(x, y, vertices)
        }
        val outlineWidth = max(1.0, radius * STAR_OUTLINE_WIDTH_RATIO)
        return vertices.indices.any { index ->
            val start = vertices[index]
            val end = vertices[(index + 1) % vertices.size]
            distanceToSegment(x, y, start.first, start.second, end.first, end.second) <= outlineWidth
        }
    }

    private fun regularStarVertices(radius: Double): List<Pair<Double, Double>> {
        val innerRadius = STAR_INNER_RADIUS_RATIO
        return List(STAR_VERTEX_COUNT) { index ->
            val angle = -Math.PI / 2.0 + index * Math.PI / STAR_POINTS
            val vertexRadius = if (index % 2 == 0) radius else radius * innerRadius
            cos(angle) * vertexRadius to sin(angle) * vertexRadius
        }
    }

    private fun pointInPolygon(x: Double, y: Double, vertices: List<Pair<Double, Double>>): Boolean {
        var inside = false
        var previousIndex = vertices.lastIndex
        vertices.indices.forEach { index ->
            val current = vertices[index]
            val previous = vertices[previousIndex]
            val crossesY = (current.second > y) != (previous.second > y)
            if (crossesY) {
                val intersectionX = (previous.first - current.first) *
                    (y - current.second) /
                    (previous.second - current.second) +
                    current.first
                if (x < intersectionX) {
                    inside = !inside
                }
            }
            previousIndex = index
        }
        return inside
    }

    private fun distanceToSegment(
        pointX: Double,
        pointY: Double,
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double
    ): Double {
        val segmentX = endX - startX
        val segmentY = endY - startY
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared <= 0.0) {
            return sqrt((pointX - startX) * (pointX - startX) + (pointY - startY) * (pointY - startY))
        }
        val projection = (((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared).coerceIn(0.0, 1.0)
        val closestX = startX + projection * segmentX
        val closestY = startY + projection * segmentY
        return sqrt((pointX - closestX) * (pointX - closestX) + (pointY - closestY) * (pointY - closestY))
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
        const val GLOBAL_CANVAS_HASH_BASE = 100_000
        const val STAR_POINTS = 5
        const val STAR_VERTEX_COUNT = STAR_POINTS * 2
        const val STAR_OUTLINE_WIDTH_RATIO = 0.12
        val STAR_INNER_RADIUS_RATIO: Double = sin(Math.PI / 10.0) / sin(3.0 * Math.PI / 10.0)
    }
}
