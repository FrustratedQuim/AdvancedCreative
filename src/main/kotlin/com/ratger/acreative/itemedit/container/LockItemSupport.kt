@file:Suppress("UnstableApiUsage") // Experimental Lockable

package com.ratger.acreative.itemedit.container

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Lockable
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType

object LockItemSupport {

    fun supports(item: ItemStack): Boolean = item.type.name.endsWith("SHULKER_BOX")

    fun get(item: ItemStack): ItemStack? = preview(item)

    fun preview(item: ItemStack): ItemStack? {
        if (!supports(item)) {
            return null
        }

        return readPreview(item)
    }

    fun set(item: ItemStack, key: ItemStack) {
        if (!supports(item)) {
            clear(item)
            return
        }
        if (isEmpty(key)) {
            clear(item)
            return
        }

        val normalizedKey = key.clone()
        val meta = item.itemMeta as? BlockStateMeta ?: return
        val state = meta.blockState
        val lockable = state as? Lockable ?: return

        lockable.setLockItem(normalizedKey)
        meta.blockState = state
        item.itemMeta = meta

        writePreview(item, normalizedKey)
    }

    fun clear(item: ItemStack) {
        val meta = item.itemMeta as? BlockStateMeta
        val state = meta?.blockState
        val lockable = state as? Lockable

        lockable?.setLockItem(null)
        if (meta != null && state != null) {
            meta.blockState = state
            item.itemMeta = meta
        }

        clearPreview(item)
    }

    fun setOrClear(item: ItemStack, key: ItemStack?) {
        if (isEmpty(key)) {
            clear(item)
            return
        }
        set(item, requireNotNull(key))
    }

    fun isEmpty(stack: ItemStack?): Boolean {
        return stack == null || stack.type == Material.AIR || stack.amount <= 0
    }

    private fun readPreview(item: ItemStack): ItemStack? {
        val meta = item.itemMeta ?: return null
        val serialized = meta.persistentDataContainer.get(PREVIEW_KEY, PersistentDataType.BYTE_ARRAY) ?: return null
        if (serialized.isEmpty()) {
            return null
        }

        return runCatching { ItemStack.deserializeBytes(serialized) }
            .getOrNull()
            ?.takeUnless(::isEmpty)
            ?.clone()
    }

    private fun writePreview(item: ItemStack, key: ItemStack) {
        if (isEmpty(key)) {
            clearPreview(item)
            return
        }

        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(PREVIEW_KEY, PersistentDataType.BYTE_ARRAY, key.clone().serializeAsBytes())
        item.itemMeta = meta
    }

    private fun clearPreview(item: ItemStack) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.remove(PREVIEW_KEY)
        item.itemMeta = meta
    }

    private val PREVIEW_KEY = NamespacedKey.minecraft("ac_lock_preview")
}
