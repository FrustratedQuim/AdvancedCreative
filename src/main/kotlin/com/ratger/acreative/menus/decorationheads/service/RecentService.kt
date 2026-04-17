package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.persistence.RecentRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class RecentService(
    private val recentRepository: RecentRepository,
    private val executor: ExecutorService,
    private val limit: Int
) {
    private data class CachedRecentEntry(
        val entry: Entry,
        var savedAtEpochSeconds: Long
    )

    private val cacheByPlayer = ConcurrentHashMap<UUID, MutableList<CachedRecentEntry>>()
    private val dirtyPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val playersWithDeferredPromotions = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val playersWithPruneCheck = ConcurrentHashMap.newKeySet<UUID>()

    fun init() = Unit

    fun push(playerId: UUID, entry: Entry, onCountUpdated: ((Int) -> Unit)? = null) {
        executor.submit {
            val entries = loadPlayerCacheIfMissing(playerId)
            val now = nowEpochSeconds()
            val sizeAfterUpdate = synchronized(entries) {
                entries.removeIf { it.entry.stableKey == entry.stableKey }
                entries.add(0, CachedRecentEntry(entry = entry, savedAtEpochSeconds = now))
                if (entries.size > limit) {
                    entries.subList(limit, entries.size).clear()
                }
                entries.size
            }
            dirtyPlayers.add(playerId)
            onCountUpdated?.invoke(sizeAfterUpdate)
        }
    }

    fun list(playerId: UUID): List<Entry> {
        val entries = loadPlayerCacheIfMissing(playerId)
        return synchronized(entries) {
            entries.map { it.entry }
        }
    }

    fun rememberInteractionForDeferredPromotion(playerId: UUID, stableKey: String) {
        val touched = playersWithDeferredPromotions.computeIfAbsent(playerId) { ConcurrentHashMap.newKeySet<String>() }
        touched.add(stableKey)
    }

    fun commitDeferredPromotions(playerId: UUID) {
        executor.submit {
            val touched = playersWithDeferredPromotions.remove(playerId).orEmpty()
            if (touched.isEmpty()) {
                return@submit
            }
            val entries = loadPlayerCacheIfMissing(playerId)
            val now = nowEpochSeconds()
            synchronized(entries) {
                touched.forEach { stableKey ->
                    val index = entries.indexOfFirst { it.entry.stableKey == stableKey }
                    if (index >= 0) {
                        val existing = entries.removeAt(index)
                        existing.savedAtEpochSeconds = now
                        entries.add(0, existing)
                    }
                }
            }
            dirtyPlayers.add(playerId)
        }
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
                changed = entries.removeIf { it.savedAtEpochSeconds < cutoff }
            }
            if (changed) dirtyPlayers.add(playerId)
        }
    }

    fun flushDirtyToDatabase() {
        val players = dirtyPlayers.toList()
        players.forEach { playerId ->
            val entries = cacheByPlayer[playerId] ?: return@forEach
            val snapshot = synchronized(entries) {
                entries.take(limit).map { RecentRepository.StoredRecentEntry(it.entry, it.savedAtEpochSeconds) }
            }
            recentRepository.replaceAll(playerId, snapshot)
            dirtyPlayers.remove(playerId)
        }
    }

    private fun loadPlayerCacheIfMissing(playerId: UUID): MutableList<CachedRecentEntry> {
        return cacheByPlayer.computeIfAbsent(playerId) {
            recentRepository.listStored(playerId)
                .take(limit)
                .map { CachedRecentEntry(it.entry, it.savedAtEpochSeconds) }
                .toMutableList()
        }
    }

    private fun nowEpochSeconds(): Long = Instant.now().epochSecond

    private companion object {
        const val WEEK_SECONDS = 7L * 24L * 60L * 60L
    }
}
