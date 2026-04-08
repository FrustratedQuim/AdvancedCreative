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

    fun placeableKeys(meta: ItemMeta?): Set<NamespacedKey> =
        runCatching { meta?.placeableKeys?.mapNotNull(::asNamespacedKey)?.toSet() ?: emptySet() }.getOrDefault(emptySet())

    fun destroyableKeys(meta: ItemMeta?): Set<NamespacedKey> =
        runCatching { meta?.destroyableKeys?.mapNotNull(::asNamespacedKey)?.toSet() ?: emptySet() }.getOrDefault(emptySet())

    fun addPlaceable(meta: ItemMeta, key: NamespacedKey): Boolean {
        val updated = LinkedHashSet(placeableKeys(meta))
        val changed = updated.add(key)
        if (changed) {
            setCanPlaceOn(meta, updated)
        }
        return changed
    }

    fun addDestroyable(meta: ItemMeta, key: NamespacedKey): Boolean {
        val updated = LinkedHashSet(destroyableKeys(meta))
        val changed = updated.add(key)
        if (changed) {
            setCanBreak(meta, updated)
        }
        return changed
    }

    fun removePlaceable(meta: ItemMeta, key: NamespacedKey): Boolean {
        val updated = LinkedHashSet(placeableKeys(meta))
        val changed = updated.remove(key)
        if (changed) {
            setCanPlaceOn(meta, updated)
        }
        return changed
    }

    fun removeDestroyable(meta: ItemMeta, key: NamespacedKey): Boolean {
        val updated = LinkedHashSet(destroyableKeys(meta))
        val changed = updated.remove(key)
        if (changed) {
            setCanBreak(meta, updated)
        }
        return changed
    }

    fun clearPlaceable(meta: ItemMeta) {
        setCanPlaceOn(meta, emptySet())
    }

    fun clearDestroyable(meta: ItemMeta) {
        setCanBreak(meta, emptySet())
    }

    private fun asNamespacedKey(value: Any?): NamespacedKey? {
        if (value == null) return null
        if (value is NamespacedKey) return value
        val namespace = runCatching { value.javaClass.getMethod("getNamespace").invoke(value) as? String }.getOrNull()
        val key = runCatching {
            when (val raw = value.javaClass.getMethod("getKey").invoke(value)) {
                is String -> raw
                is NamespacedKey -> raw.key
                else -> null
            }
        }.getOrNull()
        if (namespace.isNullOrBlank() || key.isNullOrBlank()) return null
        return NamespacedKey.fromString("$namespace:$key")
    }
}
