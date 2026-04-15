package com.ratger.acreative.menus.edit.meta

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemStackReplacementSupport {
    fun replaceItemId(original: ItemStack, newMaterial: Material): ItemStack {
        return original.withType(newMaterial)
    }

}
