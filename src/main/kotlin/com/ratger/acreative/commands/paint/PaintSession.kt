package com.ratger.acreative.commands.paint

import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Location
import java.util.UUID

data class PaintSession(
    val playerId: UUID,
    val frame: WrapperEntity,
    val frameLocation: Location,
    val frameDirection: PaintFrameDirection,
    val mapId: Int,
    val sourceHandMapSlot: Int?,
    val inventorySnapshot: PaintInventorySnapshot,
    val viewerTaskId: Int,
    val paintTaskId: Int,
    val viewers: MutableSet<UUID>,
    var previewPixelX: Int? = null,
    var previewPixelY: Int? = null,
    var previewOriginalColor: Byte? = null,
    var previewShownColor: Byte? = null,
    var lastUseAtMillis: Long = 0L,
    var lastStrokePixelX: Int? = null,
    var lastStrokePixelY: Int? = null,
    var lastStrokeColor: Byte? = null,
    var lastStrokeAtMillis: Long = 0L
)
