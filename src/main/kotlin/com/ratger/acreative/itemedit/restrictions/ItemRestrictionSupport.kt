package com.ratger.acreative.itemedit.restrictions

import com.ratger.acreative.itemedit.meta.LegacyMetaKeySupport
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack

object ItemRestrictionSupport {
    data class RestrictionEntry(
        val key: NamespacedKey,
        val displayId: String,
        val material: Material?
    )

    fun entries(item: ItemStack, mode: RestrictionMode): List<RestrictionEntry> {
        return keys(item, mode)
            .sortedBy { displayId(it) }
            .map { key ->
                RestrictionEntry(
                    key = key,
                    displayId = displayId(key),
                    material = Registry.MATERIAL.get(key)?.takeIf { it.isBlock }
                )
            }
    }

    fun add(item: ItemStack, mode: RestrictionMode, key: NamespacedKey): Boolean {
        val meta = item.itemMeta ?: return false
        val changed = when (mode) {
            RestrictionMode.CAN_PLACE_ON -> LegacyMetaKeySupport.addPlaceable(meta, key)
            RestrictionMode.CAN_BREAK -> LegacyMetaKeySupport.addDestroyable(meta, key)
        }
        if (changed) {
            item.itemMeta = meta
        }
        return changed
    }

    fun remove(item: ItemStack, mode: RestrictionMode, key: NamespacedKey): Boolean {
        val meta = item.itemMeta ?: return false
        val changed = when (mode) {
            RestrictionMode.CAN_PLACE_ON -> LegacyMetaKeySupport.removePlaceable(meta, key)
            RestrictionMode.CAN_BREAK -> LegacyMetaKeySupport.removeDestroyable(meta, key)
        }
        if (changed) {
            item.itemMeta = meta
        }
        return changed
    }

    fun clear(item: ItemStack, mode: RestrictionMode) {
        val meta = item.itemMeta ?: return
        when (mode) {
            RestrictionMode.CAN_PLACE_ON -> LegacyMetaKeySupport.clearPlaceable(meta)
            RestrictionMode.CAN_BREAK -> LegacyMetaKeySupport.clearDestroyable(meta)
        }
        item.itemMeta = meta
    }

    fun keys(item: ItemStack, mode: RestrictionMode): Set<NamespacedKey> {
        return when (mode) {
            RestrictionMode.CAN_PLACE_ON -> LegacyMetaKeySupport.placeableKeys(item.itemMeta)
            RestrictionMode.CAN_BREAK -> LegacyMetaKeySupport.destroyableKeys(item.itemMeta)
        }
    }

    fun displayId(key: NamespacedKey): String {
        return if (key.namespace.equals("minecraft", ignoreCase = true)) key.key else key.asString()
    }
}
