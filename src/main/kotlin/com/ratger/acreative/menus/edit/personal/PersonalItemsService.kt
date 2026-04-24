package com.ratger.acreative.menus.edit.personal

import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class PersonalItemsService(
    private val repository: PersonalItemsRepository,
    private val executor: ExecutorService,
    private val limit: Int
) {
    data class MemorySnapshot(
        val cachedPlayers: Int,
        val cachedItems: Int,
        val items: List<ItemStack>
    )

    private data class CachedPersonalItem(
        val contentHash: String,
        val item: ItemStack,
        var lastUsedAtEpochSeconds: Long
    )

    private val cacheByPlayer = ConcurrentHashMap<UUID, MutableList<CachedPersonalItem>>()
    private val dirtyPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val playersWithDeferredPromotions = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val playersWithPruneCheck = ConcurrentHashMap.newKeySet<UUID>()

    fun list(playerId: UUID): List<ItemStack> {
        val entries = loadPlayerCacheIfMissing(playerId)
        return synchronized(entries) {
            entries.map { it.item.clone() }
        }
    }

    fun onEditSessionClosed(playerId: UUID, editedItem: ItemStack, initialContentHash: String?) {
        executor.submit {
            commitDeferredPromotionsInternal(playerId)
            pushEditedItemInternal(playerId, editedItem, initialContentHash)
        }
    }

    fun commitDeferredPromotions(playerId: UUID) {
        executor.submit { commitDeferredPromotionsInternal(playerId) }
    }

    fun rememberInteractionForDeferredPromotion(playerId: UUID, item: ItemStack) {
        val key = contentHash(item) ?: return
        val touched = playersWithDeferredPromotions.computeIfAbsent(playerId) { ConcurrentHashMap.newKeySet() }
        touched.add(key)
    }

    fun pruneExpiredOnFirstJoin(playerId: UUID) {
        if (!playersWithPruneCheck.add(playerId)) {
            return
        }
        executor.submit {
            val entries = loadPlayerCacheIfMissing(playerId)
            val cutoff = nowEpochSeconds() - WEEK_SECONDS
            var changed = false
            synchronized(entries) {
                changed = entries.removeIf { it.lastUsedAtEpochSeconds < cutoff }
            }
            if (changed) dirtyPlayers.add(playerId)
        }
    }

    fun flushDirtyToDatabase() {
        val players = dirtyPlayers.toList()
        players.forEach(::flushPlayerToDatabase)
    }

    fun memorySnapshot(): MemorySnapshot {
        val items = cacheByPlayer.values.flatMap { entries ->
            synchronized(entries) { entries.map { it.item.clone() } }
        }
        return MemorySnapshot(
            cachedPlayers = cacheByPlayer.size,
            cachedItems = items.size,
            items = items
        )
    }

    fun evictPlayer(playerId: UUID) {
        executor.submit {
            commitDeferredPromotionsInternal(playerId)
            flushPlayerToDatabase(playerId)
            cacheByPlayer.remove(playerId)
            dirtyPlayers.remove(playerId)
            playersWithDeferredPromotions.remove(playerId)
            playersWithPruneCheck.remove(playerId)
        }
    }

    fun giveFromMenu(player: Player, item: ItemStack, clickEvent: ClickEvent) {
        clickEvent.handle.isCancelled = true
        val giveItem = item.clone().apply { amount = if (clickEvent.isMiddle || clickEvent.type == ClickType.CONTROL_DROP) 64 else 1 }

        val isShiftClick = clickEvent.isShiftLeft ||
            clickEvent.isShiftRight ||
            clickEvent.handle.isShiftClick ||
            (player.isSneaking && (clickEvent.isLeft || clickEvent.isRight))

        when {
            isDropClick(clickEvent) -> dropItem(player, giveItem)
            isShiftClick -> {
                val remainingAmount = PlayerInventoryTransferSupport.tryAddToExistingStacks(player.inventory, giveItem)
                if (remainingAmount > 0) {
                    val remainingItem = giveItem.clone().also { it.amount = remainingAmount }
                    val targetSlot = PlayerInventoryTransferSupport.findPreferredEmptySlot(player.inventory)
                    if (targetSlot != null) {
                        player.inventory.setItem(targetSlot, remainingItem)
                    } else {
                        dropItem(player, remainingItem)
                    }
                }
            }
            else -> player.setItemOnCursor(giveItem)
        }

        rememberInteractionForDeferredPromotion(player.uniqueId, item)
    }

    private fun commitDeferredPromotionsInternal(playerId: UUID) {
        val touched = playersWithDeferredPromotions.remove(playerId).orEmpty()
        if (touched.isEmpty()) {
            return
        }
        val entries = loadPlayerCacheIfMissing(playerId)
        val now = nowEpochSeconds()
        synchronized(entries) {
            touched.forEach { hash ->
                val index = entries.indexOfFirst { it.contentHash == hash }
                if (index >= 0) {
                    val existing = entries.removeAt(index)
                    existing.lastUsedAtEpochSeconds = now
                    entries.add(0, existing)
                }
            }
        }
        dirtyPlayers.add(playerId)
    }

    private fun pushEditedItemInternal(playerId: UUID, item: ItemStack, initialContentHash: String?) {
        if (item.type.isAir || item.amount <= 0) return
        val normalizedItem = normalizeForStorage(item)
        val hash = contentHash(normalizedItem) ?: return
        if (hash == initialContentHash) return
        val entries = loadPlayerCacheIfMissing(playerId)
        val now = nowEpochSeconds()
        synchronized(entries) {
            entries.removeIf { it.contentHash == hash }
            entries.add(0, CachedPersonalItem(hash, normalizedItem, now))
            if (entries.size > limit) {
                entries.subList(limit, entries.size).clear()
            }
        }
        dirtyPlayers.add(playerId)
    }

    private fun loadPlayerCacheIfMissing(playerId: UUID): MutableList<CachedPersonalItem> {
        return cacheByPlayer.computeIfAbsent(playerId) {
            repository.list(playerId)
                .take(limit)
                .map { CachedPersonalItem(it.contentHash, it.item.clone(), it.lastUsedAtEpochSeconds) }
                .toMutableList()
        }
    }

    private fun flushPlayerToDatabase(playerId: UUID) {
        val entries = cacheByPlayer[playerId] ?: return
        if (!dirtyPlayers.contains(playerId)) {
            return
        }
        val snapshot = synchronized(entries) {
            entries.take(limit).map {
                PersonalItemsRepository.StoredPersonalItem(
                    contentHash = it.contentHash,
                    item = it.item,
                    lastUsedAtEpochSeconds = it.lastUsedAtEpochSeconds
                )
            }
        }
        repository.replaceAll(playerId, snapshot)
        dirtyPlayers.remove(playerId)
    }

    private fun contentHash(item: ItemStack): String? {
        if (item.type.isAir || item.amount <= 0) return null
        val bytes = normalizeForStorage(item).serializeAsBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun normalizeForStorage(item: ItemStack): ItemStack {
        return item.clone().apply { amount = 1 }
    }

    private fun nowEpochSeconds(): Long = Instant.now().epochSecond

    private fun isDropClick(event: ClickEvent): Boolean {
        return event.type == ClickType.DROP || event.type == ClickType.CONTROL_DROP
    }

    private fun dropItem(player: Player, item: ItemStack) {
        val drop = player.world.dropItem(player.location.clone().add(0.0, 1.0, 0.0), item)
        drop.velocity = player.eyeLocation.direction.normalize().multiply(0.3).add(org.bukkit.util.Vector(0.0, 0.1, 0.0))
    }

    private companion object {
        const val WEEK_SECONDS = 7L * 24L * 60L * 60L
    }
}
