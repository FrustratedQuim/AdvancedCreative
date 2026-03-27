package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.item.ItemContainerContents
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object EditContainerSupport {
    fun containerCapacity(material: Material): Int? {
        if (material.name.endsWith("SHULKER_BOX")) return 27
        return when (material) {
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL -> 27
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER -> 3
            Material.DISPENSER,
            Material.DROPPER,
            Material.CRAFTER -> 9
            Material.HOPPER -> 5
            Material.BREWING_STAND -> 5
            Material.JUKEBOX -> 1
            Material.CHISELED_BOOKSHELF -> 6
            Material.LECTERN -> 1
            Material.DECORATED_POT -> 1
            Material.SUSPICIOUS_SAND,
            Material.SUSPICIOUS_GRAVEL -> 1
            Material.SOUL_CAMPFIRE,
            Material.CAMPFIRE -> 4
            Material.GRINDSTONE -> 3
            else -> null
        }
    }

    fun buildContainerContents(capacity: Int, current: ItemContainerContents?): MutableList<ItemStack> {
        val out = MutableList(capacity) { ItemStack(Material.AIR) }
        val existing = current?.contents() ?: emptyList()
        for (index in 0 until minOf(capacity, existing.size)) {
            val source = existing[index]
            out[index] = if (source.type == Material.AIR || source.amount <= 0) ItemStack(Material.AIR) else source.clone()
        }
        return out
    }

    fun isEmpty(stack: ItemStack?): Boolean = stack == null || stack.type == Material.AIR || stack.amount <= 0
}
