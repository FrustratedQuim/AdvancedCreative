package com.ratger.acreative.menus.banner.service

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BannerTakeCooldownService(
    private val cooldownMillis: Long = 6L * 60L * 60L * 1000L
) {
    private val takenByBanner = ConcurrentHashMap<Long, ConcurrentHashMap<UUID, Long>>()

    fun shouldCountTake(bannerId: Long, playerId: UUID, nowMillis: Long = System.currentTimeMillis()): Boolean {
        pruneExpiredAll(nowMillis)
        val entries = takenByBanner.computeIfAbsent(bannerId) { ConcurrentHashMap() }
        val expiresAt = entries[playerId]
        if (expiresAt != null && expiresAt > nowMillis) {
            return false
        }

        entries[playerId] = nowMillis + cooldownMillis
        pruneExpired(bannerId, entries, nowMillis)
        return true
    }

    private fun pruneExpired(bannerId: Long, entries: MutableMap<UUID, Long>, nowMillis: Long) {
        entries.entries.removeIf { it.value <= nowMillis }
        if (entries.isEmpty()) {
            takenByBanner.remove(bannerId, entries)
        }
    }

    private fun pruneExpiredAll(nowMillis: Long) {
        takenByBanner.entries.removeIf { (bannerId, entries) ->
            pruneExpired(bannerId, entries, nowMillis)
            entries.isEmpty()
        }
    }
}
