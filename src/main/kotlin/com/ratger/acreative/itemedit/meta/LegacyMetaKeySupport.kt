@file:Suppress("DEPRECATION")

package com.ratger.acreative.itemedit.meta

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta

object LegacyMetaKeySupport {
    fun placeableCount(meta: ItemMeta?): Int = runCatching { meta?.placeableKeys?.size ?: 0 }.getOrDefault(0)

    fun destroyableCount(meta: ItemMeta?): Int = runCatching { meta?.destroyableKeys?.size ?: 0 }.getOrDefault(0)

    fun hasPlaceable(meta: ItemMeta): Boolean = runCatching { meta.placeableKeys.isNotEmpty() }.getOrDefault(false)

    fun hasDestroyable(meta: ItemMeta): Boolean = runCatching { meta.destroyableKeys.isNotEmpty() }.getOrDefault(false)

    fun setCanPlaceOn(meta: ItemMeta, keys: Set<NamespacedKey>) {
        meta.setPlaceableKeys(keys)
    }

    fun setCanBreak(meta: ItemMeta, keys: Set<NamespacedKey>) {
        meta.setDestroyableKeys(keys)
    }
}
