package com.ratger.acreative.paint.model

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class PaintInventorySnapshot(
    val storageContents: List<ItemStack?>,
    val armorContents: List<ItemStack?>,
    val extraContents: List<ItemStack?>,
    val heldSlot: Int
) {
    fun restore(player: Player) {
        val inventory = player.inventory
        inventory.storageContents = storageContents.map { it?.clone() }.toTypedArray()
        inventory.armorContents = armorContents.map { it?.clone() }.toTypedArray()
        inventory.extraContents = extraContents.map { it?.clone() }.toTypedArray()
        inventory.heldItemSlot = heldSlot
    }

    companion object {
        fun capture(player: Player): PaintInventorySnapshot {
            val inventory = player.inventory
            return PaintInventorySnapshot(
                storageContents = inventory.storageContents.map { it?.clone() },
                armorContents = inventory.armorContents.map { it?.clone() },
                extraContents = inventory.extraContents.map { it?.clone() },
                heldSlot = inventory.heldItemSlot
            )
        }
    }
}
