package com.ratger.acreative.menus.decorationheads.cache

import com.ratger.acreative.menus.decorationheads.model.DecorationHeadEntry
import java.util.concurrent.ConcurrentHashMap

class DecorationHeadCache(
    dynamicLimit: Int,
    searchLimit: Int
) {
    private val headByStableKey = ConcurrentHashMap<String, DecorationHeadEntry>()
    private val pinnedWarmKeys = ConcurrentHashMap.newKeySet<String>()
    private val dynamicCache = DecorationHeadLruCache<String, DecorationHeadEntry>(dynamicLimit)
    val pageIndex = DecorationHeadPageIndex()
    val searchIndex = DecorationHeadSearchIndex(searchLimit)

    fun put(entry: DecorationHeadEntry, pinned: Boolean = false) {
        headByStableKey[entry.stableKey] = entry
        if (pinned) pinnedWarmKeys += entry.stableKey else dynamicCache.put(entry.stableKey, entry)
    }

    fun putAll(entries: Collection<DecorationHeadEntry>, pinned: Boolean = false) {
        entries.forEach { put(it, pinned) }
    }

    fun get(stableKey: String): DecorationHeadEntry? =
        headByStableKey[stableKey] ?: dynamicCache.get(stableKey)

    fun values(): List<DecorationHeadEntry> = headByStableKey.values.toList()

    fun clearIndexes() {
        pageIndex.clear()
        searchIndex.clear()
    }
}
