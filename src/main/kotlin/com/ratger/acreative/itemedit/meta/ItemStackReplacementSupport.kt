package com.ratger.acreative.itemedit.meta

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemStackReplacementSupport {
    fun replaceItemId(original: ItemStack, newMaterial: Material): ItemStack {
        val replaced = ItemStack(newMaterial, original.amount)
        val oldMeta = original.itemMeta
        if (oldMeta != null && Bukkit.getItemFactory().isApplicable(oldMeta, newMaterial)) {
            replaced.itemMeta = oldMeta.clone()
        }
        return replaced
    }

    fun resetAll(item: ItemStack) {
        if (item.type == Material.AIR) return
        val clean = ItemStack(item.type, item.amount)
        item.itemMeta = clean.itemMeta
    }
}
