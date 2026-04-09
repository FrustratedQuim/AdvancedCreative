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

        readPreview(item)?.let { return it }

        val fallback = previewFromLegacyStringLock(item) ?: return null
        writePreview(item, fallback)
        return fallback.clone()
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

    private fun lockableState(item: ItemStack): Lockable? {
        val meta = item.itemMeta as? BlockStateMeta ?: return null
        return meta.blockState as? Lockable
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

    @Suppress("DEPRECATION") // Legacy lock-string fallback for old items without preview bytes
    private fun previewFromLegacyStringLock(item: ItemStack): ItemStack? {
        val lockable = lockableState(item) ?: return null
        if (!lockable.isLocked) return null

        val material = parseMaterialFromLock(lockable.lock) ?: return null
        return ItemStack(material)
    }

    private fun parseMaterialFromLock(lock: String): Material? {
        if (lock.isBlank()) return null

        val explicitKey = LOCK_ITEMS_PATTERN.find(lock)?.groupValues?.getOrNull(1)
            ?: LOCK_ID_PATTERN.find(lock)?.groupValues?.getOrNull(1)
        if (explicitKey != null) {
            keyToMaterial(explicitKey)?.let { return it }
        }

        LOCK_NAMESPACE_PATTERN.findAll(lock)
            .mapNotNull { keyToMaterial(it.value) }
            .firstOrNull()
            ?.let { return it }

        return keyToMaterial(lock)
    }

    private fun keyToMaterial(raw: String): Material? {
        val normalized = raw.trim().trim('"')
        val fromNamespaced = Material.matchMaterial(normalized, true)
        if (fromNamespaced != null) {
            return fromNamespaced
        }

        val tail = normalized.substringAfter(':', normalized)
        return Material.matchMaterial(tail.uppercase(), true)
    }

    private val PREVIEW_KEY = NamespacedKey.minecraft("ac_lock_preview")

    private val LOCK_ITEMS_PATTERN = Regex("\"?items\"?\\s*:\\s*\"?([a-z0-9_:]+)", RegexOption.IGNORE_CASE)
    private val LOCK_ID_PATTERN = Regex("\"?id\"?\\s*:\\s*\"?([a-z0-9_:]+)", RegexOption.IGNORE_CASE)
    private val LOCK_NAMESPACE_PATTERN = Regex("[a-z0-9_]+:[a-z0-9_]+", RegexOption.IGNORE_CASE)
}
