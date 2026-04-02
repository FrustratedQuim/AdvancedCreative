package com.ratger.acreative.menus

import org.bukkit.inventory.ItemStack
import java.util.UUID

data class ItemEditSession(
    val playerId: UUID,
    val originalMainHandSlot: Int,
    var editableItem: ItemStack
)
