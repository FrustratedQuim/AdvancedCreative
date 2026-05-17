package com.ratger.acreative.commands.paint.tools

import com.ratger.acreative.commands.paint.map.MapColorMatcher
import com.ratger.acreative.commands.paint.model.PaintCanvasBounds
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.model.PaintSurfacePixel
import java.util.UUID

data class FillComponentCache(
    val revision: Long,
    val ignoreShade: Boolean,
    val minGlobalX: Int,
    val minGlobalY: Int,
    val width: Int,
    val height: Int,
    val labels: IntArray,
    val colors: ByteArray,
    val componentSizes: IntArray,
    val componentStarts: IntArray,
    val componentIndices: IntArray
) {
    fun indexOf(globalX: Int, globalY: Int): Int? {
        val localX = globalX - minGlobalX
        val localY = globalY - minGlobalY
        if (localX !in 0 until width || localY !in 0 until height) return null
        return localY * width + localX
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FillComponentCache

        return revision == other.revision &&
            ignoreShade == other.ignoreShade &&
            minGlobalX == other.minGlobalX &&
            minGlobalY == other.minGlobalY &&
            width == other.width &&
            height == other.height &&
            labels.contentEquals(other.labels) &&
            colors.contentEquals(other.colors) &&
            componentSizes.contentEquals(other.componentSizes) &&
            componentStarts.contentEquals(other.componentStarts) &&
            componentIndices.contentEquals(other.componentIndices)
    }

    override fun hashCode(): Int {
        var result = revision.hashCode()
        result = 31 * result + ignoreShade.hashCode()
        result = 31 * result + minGlobalX
        result = 31 * result + minGlobalY
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + labels.contentHashCode()
        result = 31 * result + colors.contentHashCode()
        result = 31 * result + componentSizes.contentHashCode()
        result = 31 * result + componentStarts.contentHashCode()
        result = 31 * result + componentIndices.contentHashCode()
        return result
    }
}

data class FillArea(
    val cache: FillComponentCache,
    val componentId: Int,
    val start: Int,
    val size: Int
) {
    fun isEmpty(): Boolean = size <= 0

    inline fun forEachIndex(action: (Int) -> Unit) {
        val end = start + size
        for (offset in start until end) {
            action(cache.componentIndices[offset])
        }
    }
}

class FillComponentResolver {
    private val caches = mutableMapOf<UUID, FillComponentCache>()

    fun resolveArea(session: PaintSession, hit: PaintSurfacePixel, ignoreShade: Boolean): FillArea? {
        val (cache, componentId) = resolveComponent(session, hit, ignoreShade) ?: return null
        return resolveArea(cache, componentId)
    }

    fun resolveComponent(
        session: PaintSession,
        hit: PaintSurfacePixel,
        ignoreShade: Boolean
    ): Pair<FillComponentCache, Int>? {
        val cache = resolveCache(session, ignoreShade) ?: return null
        val startIndex = cache.indexOf(hit.globalX, hit.globalY) ?: return null
        if (startIndex !in cache.labels.indices) return null
        val componentId = cache.labels[startIndex]
        if (componentId <= 0) return null
        return cache to componentId
    }

    fun resolveArea(cache: FillComponentCache, componentId: Int): FillArea? {
        if (componentId !in cache.componentSizes.indices) return null
        val componentSize = cache.componentSizes[componentId].takeIf { it > 0 } ?: return null
        if (componentId !in cache.componentStarts.indices) return null
        return FillArea(cache, componentId, cache.componentStarts[componentId], componentSize)
    }

    fun clear(playerId: UUID) {
        caches.remove(playerId)
    }

    private fun resolveCache(session: PaintSession, ignoreShade: Boolean): FillComponentCache? {
        caches[session.playerId]?.let { cache ->
            if (cache.revision == session.canvasRevision && cache.ignoreShade == ignoreShade) {
                return cache
            }
        }
        return buildCache(session, ignoreShade)?.also { cache ->
            caches[session.playerId] = cache
        }
    }

    private fun buildCache(session: PaintSession, ignoreShade: Boolean): FillComponentCache? {
        val bounds = PaintCanvasBounds.from(session.logicalCells.keys) ?: return null
        val minGlobalX = bounds.minX * MAP_WIDTH
        val minGlobalY = bounds.minY * MAP_HEIGHT
        val width = bounds.width * MAP_WIDTH
        val height = bounds.height * MAP_HEIGHT
        val totalPixels = width * height
        val labels = IntArray(totalPixels) { FILL_INVALID_LABEL }
        val colors = ByteArray(totalPixels)

        session.logicalCells.forEach { (point, logicalColors) ->
            val offsetX = point.x * MAP_WIDTH - minGlobalX
            val offsetY = point.y * MAP_HEIGHT - minGlobalY
            for (localY in 0 until MAP_HEIGHT) {
                val targetRowStart = (offsetY + localY) * width + offsetX
                val sourceRowStart = localY * MAP_WIDTH
                logicalColors.copyInto(
                    destination = colors,
                    destinationOffset = targetRowStart,
                    startIndex = sourceRowStart,
                    endIndex = sourceRowStart + MAP_WIDTH
                )
                for (localX in 0 until MAP_WIDTH) {
                    labels[targetRowStart + localX] = FILL_UNLABELED
                }
            }
        }

        val componentSizes = IntArray(totalPixels + 1)
        val componentStarts = IntArray(totalPixels + 2)
        val componentIndices = IntArray(totalPixels)
        val queue = IntArray(totalPixels)
        var componentCursor = 0
        var componentId = 0
        for (startIndex in 0 until totalPixels) {
            if (labels[startIndex] != FILL_UNLABELED) continue
            componentId += 1
            componentStarts[componentId] = componentCursor
            val startColor = colors[startIndex]
            var head = 0
            var tail = 0
            queue[tail++] = startIndex
            labels[startIndex] = componentId

            while (head < tail) {
                val currentIndex = queue[head++]
                val x = currentIndex % width
                tail = enqueueNeighbor(currentIndex - 1, x > 0, labels, colors, startColor, ignoreShade, componentId, queue, tail)
                tail = enqueueNeighbor(currentIndex + 1, x + 1 < width, labels, colors, startColor, ignoreShade, componentId, queue, tail)
                tail = enqueueNeighbor(currentIndex - width, currentIndex >= width, labels, colors, startColor, ignoreShade, componentId, queue, tail)
                tail = enqueueNeighbor(currentIndex + width, currentIndex + width < totalPixels, labels, colors, startColor, ignoreShade, componentId, queue, tail)
            }

            componentSizes[componentId] = tail
            queue.copyInto(componentIndices, destinationOffset = componentCursor, startIndex = 0, endIndex = tail)
            componentCursor += tail
        }

        return FillComponentCache(
            revision = session.canvasRevision,
            ignoreShade = ignoreShade,
            minGlobalX = minGlobalX,
            minGlobalY = minGlobalY,
            width = width,
            height = height,
            labels = labels,
            colors = colors,
            componentSizes = componentSizes.copyOf(componentId + 1),
            componentStarts = componentStarts.copyOf(componentId + 1),
            componentIndices = componentIndices.copyOf(componentCursor)
        )
    }

    private fun enqueueNeighbor(
        index: Int,
        isValidNeighbor: Boolean,
        labels: IntArray,
        colors: ByteArray,
        startColor: Byte,
        ignoreShade: Boolean,
        componentId: Int,
        queue: IntArray,
        tail: Int
    ): Int {
        if (!isValidNeighbor || labels[index] != FILL_UNLABELED) return tail
        if (!matchesFillColor(startColor, colors[index], ignoreShade)) return tail
        labels[index] = componentId
        queue[tail] = index
        return tail + 1
    }

    private fun matchesFillColor(expected: Byte, candidate: Byte, ignoreShade: Boolean): Boolean {
        if (!ignoreShade) {
            return expected == candidate
        }
        if (expected == MapColorMatcher.TRANSPARENT_COLOR_ID || candidate == MapColorMatcher.TRANSPARENT_COLOR_ID) {
            return expected == candidate
        }
        return packedColorFamily(expected) == packedColorFamily(candidate)
    }

    private fun packedColorFamily(color: Byte): Int = (color.toInt() and 0xFF) / 4

    private companion object {
        const val MAP_WIDTH = 128
        const val MAP_HEIGHT = 128
        const val FILL_INVALID_LABEL = 0
        const val FILL_UNLABELED = -1
    }
}
