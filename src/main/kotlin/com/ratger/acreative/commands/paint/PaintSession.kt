package com.ratger.acreative.commands.paint

import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Location
import java.util.UUID

data class PaintSession(
    val playerId: UUID,
    val frame: WrapperEntity,
    val frameLocation: Location,
    val mapId: Int,
    val sourceHandMapSlot: Int?,
    val inventorySnapshot: PaintInventorySnapshot,
    val viewerTaskId: Int,
    val viewers: MutableSet<UUID>
)
