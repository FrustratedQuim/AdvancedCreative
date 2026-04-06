package com.ratger.acreative.itemedit.meta

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemStackReplacementSupport {
    fun replaceItemId(original: ItemStack, newMaterial: Material): ItemStack {
        return original.withType(newMaterial)
    }

    fun resetAll(item: ItemStack) {
        if (item.type == Material.AIR) return
        val clean = ItemStack(item.type, item.amount)
        item.itemMeta = clean.itemMeta
    }
}
