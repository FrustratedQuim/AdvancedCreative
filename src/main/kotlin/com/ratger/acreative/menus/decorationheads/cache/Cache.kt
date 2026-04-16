package com.ratger.acreative.menus.decorationheads.cache

import com.ratger.acreative.menus.decorationheads.model.Entry

class Cache(
    dynamicLimit: Int,
    searchLimit: Int
) {
    private val headByStableKey = LruCache<String, Entry>(dynamicLimit)
    val searchIndex = SearchIndex(searchLimit)

    fun put(entry: Entry) {
        headByStableKey.put(entry.stableKey, entry)
    }

    fun putAll(entries: Collection<Entry>) {
        entries.forEach(::put)
    }

    fun get(stableKey: String): Entry? = headByStableKey.get(stableKey)

    fun clearIndexes() {
        searchIndex.clear()
    }
}
