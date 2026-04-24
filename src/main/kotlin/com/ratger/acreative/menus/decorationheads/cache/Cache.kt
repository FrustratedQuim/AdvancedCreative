package com.ratger.acreative.menus.decorationheads.cache

import com.ratger.acreative.menus.decorationheads.model.Entry

class Cache(
    private val dynamicLimit: Int,
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

    fun dynamicEntriesSnapshot(): List<Entry> = headByStableKey.snapshotEntries().map { it.value }

    fun dynamicSize(): Int = headByStableKey.size()

    fun dynamicLimit(): Int = dynamicLimit

}
