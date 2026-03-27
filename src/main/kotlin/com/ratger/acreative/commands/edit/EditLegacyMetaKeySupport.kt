@file:Suppress("DEPRECATION")

package com.ratger.acreative.commands.edit

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta

object EditLegacyMetaKeySupport {
    fun setCanPlaceOn(meta: ItemMeta, keys: Set<NamespacedKey>) {
        meta.setPlaceableKeys(keys)
    }

    fun setCanBreak(meta: ItemMeta, keys: Set<NamespacedKey>) {
        meta.setDestroyableKeys(keys)
    }
}
