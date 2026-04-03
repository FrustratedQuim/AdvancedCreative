package com.ratger.acreative.itemedit.meta

import org.bukkit.inventory.meta.ItemMeta

object MaxStackSizeSupport {
    fun clearCustomMaxStackSize(meta: ItemMeta): Boolean {
        if (!meta.hasMaxStackSize()) return true
        meta.setMaxStackSize(null)
        return !meta.hasMaxStackSize()
    }
}
