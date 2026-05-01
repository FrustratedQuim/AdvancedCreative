package com.ratger.acreative.paint.model

import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Location

data class PaintCanvasSize(
    val width: Int,
    val height: Int
) {
    init {
        require(width in 1..4) { "Width must be between 1 and 4" }
        require(height in 1..4) { "Height must be between 1 and 4" }
    }

    val basePoint: PaintGridPoint
        get() = PaintGridPoint((width - 1) / 2, height - 1)

    fun initialPoints(): List<PaintGridPoint> {
        return buildList {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    add(PaintGridPoint(x, y))
                }
            }
        }
    }

    fun normalized(): String = "${width}x${height}"

    companion object {
        val DEFAULT = PaintCanvasSize(1, 1)
        val TAB_SUGGESTIONS: List<String> = buildList {
            for (width in 1..4) {
                for (height in 1..4) {
                    add("${width}x${height}")
                }
            }
        }

        fun parse(raw: String?): PaintCanvasSize? {
            if (raw.isNullOrBlank()) return DEFAULT
            val match = SIZE_REGEX.matchEntire(raw.trim().lowercase()) ?: return null
            val width = match.groupValues[1].toIntOrNull() ?: return null
            val height = match.groupValues[2].toIntOrNull() ?: return null
            if (width !in 1..4 || height !in 1..4) return null
            return PaintCanvasSize(width, height)
        }

        private val SIZE_REGEX = Regex("""^(\d+)x(\d+)$""")
    }
}

data class PaintGridPoint(
    val x: Int,
    val y: Int
)

data class PaintCanvasBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
) {
    val width: Int
        get() = maxX - minX + 1

    val height: Int
        get() = maxY - minY + 1

    companion object {
        fun from(points: Collection<PaintGridPoint>): PaintCanvasBounds? {
            if (points.isEmpty()) return null
            return PaintCanvasBounds(
                minX = points.minOf { it.x },
                maxX = points.maxOf { it.x },
                minY = points.minOf { it.y },
                maxY = points.maxOf { it.y }
            )
        }
    }
}

data class PaintSurfacePixel(
    val cellPoint: PaintGridPoint,
    val localX: Int,
    val localY: Int,
    val globalX: Int,
    val globalY: Int
)

data class PaintLineAnchor(
    val globalX: Int,
    val globalY: Int
)

data class PaintCanvasCell(
    val point: PaintGridPoint,
    val mapId: Int,
    val frame: WrapperEntity,
    val backPanel: WrapperEntity,
    val location: Location
)

data class PaintPixelChange(
    val x: Int,
    val y: Int,
    val oldColor: Byte,
    val newColor: Byte
)

data class PaintHistoryEntry(
    val changesByMapId: Map<Int, List<PaintPixelChange>>,
    val estimatedBytes: Long,
    val lineAnchorBefore: PaintLineAnchor? = null,
    val lineAnchorAfter: PaintLineAnchor? = null
) {
    val hasLineAnchorChange: Boolean
        get() = lineAnchorBefore != lineAnchorAfter
}
