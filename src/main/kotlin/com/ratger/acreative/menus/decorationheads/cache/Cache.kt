package com.ratger.acreative.menus.decorationheads.cache

import com.ratger.acreative.menus.decorationheads.model.Entry
import java.util.concurrent.ConcurrentHashMap

class Cache(
    dynamicLimit: Int,
    searchLimit: Int
) {
    private val headByStableKey = ConcurrentHashMap<String, Entry>()
    private val pinnedWarmKeys = ConcurrentHashMap.newKeySet<String>()
    private val dynamicCache = LruCache<String, Entry>(dynamicLimit)
    val pageIndex = PageIndex()
    val searchIndex = SearchIndex(searchLimit)

    fun put(entry: Entry, pinned: Boolean = false) {
        headByStableKey[entry.stableKey] = entry
        if (pinned) pinnedWarmKeys += entry.stableKey else dynamicCache.put(entry.stableKey, entry)
    }

    fun putAll(entries: Collection<Entry>, pinned: Boolean = false) {
        entries.forEach { put(it, pinned) }
    }

    fun get(stableKey: String): Entry? =
        headByStableKey[stableKey] ?: dynamicCache.get(stableKey)

    fun values(): List<Entry> = headByStableKey.values.toList()

    fun clearIndexes() {
        pageIndex.clear()
        searchIndex.clear()
    }
}
