package com.ratger.acreative.commands.edit

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta

object EditLegacyMetaKeySupport {
    @Suppress("DEPRECATION")
    fun setCanPlaceOn(meta: ItemMeta, keys: Set<NamespacedKey>) {
        meta.setPlaceableKeys(keys)
    }

    @Suppress("DEPRECATION")
    fun setCanBreak(meta: ItemMeta, keys: Set<NamespacedKey>) {
        meta.setDestroyableKeys(keys)
    }
}
