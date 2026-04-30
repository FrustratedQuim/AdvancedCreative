package com.ratger.acreative.paint.model

import me.tofaa.entitylib.meta.other.ItemFrameMeta

enum class PaintFrameDirection(
    val offsetX: Double,
    val offsetZ: Double,
    val spawnYaw: Float,
    val orientation: ItemFrameMeta.Orientation,
    val normalX: Double,
    val normalZ: Double,
    val rightAxisX: Double,
    val rightAxisZ: Double
) {
    NORTH(0.0, -1.0, 180f, ItemFrameMeta.Orientation.SOUTH, 0.0, 1.0, 1.0, 0.0),
    SOUTH(0.0, 1.0, 0f, ItemFrameMeta.Orientation.NORTH, 0.0, -1.0, -1.0, 0.0),
    EAST(1.0, 0.0, 270f, ItemFrameMeta.Orientation.WEST, -1.0, 0.0, 0.0, 1.0),
    WEST(-1.0, 0.0, 90f, ItemFrameMeta.Orientation.EAST, 1.0, 0.0, 0.0, -1.0)
}
