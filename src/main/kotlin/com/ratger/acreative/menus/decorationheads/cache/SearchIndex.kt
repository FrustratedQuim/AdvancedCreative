package com.ratger.acreative.menus.decorationheads.cache

import com.ratger.acreative.menus.decorationheads.model.Entry

class SearchIndex(
    private val limit: Int
) {
    private val cache = LruCache<String, List<Entry>>(limit)

    fun put(query: String, page: Int, pageSize: Int, entries: List<Entry>) =
        cache.put(key(query, page, pageSize), entries)

    fun get(query: String, page: Int, pageSize: Int): List<Entry>? =
        cache.get(key(query, page, pageSize))

    fun snapshot(): List<Pair<String, List<Entry>>> = cache.snapshotEntries().map { it.key to it.value }

    fun size(): Int = cache.size()

    fun limit(): Int = limit

    fun clear() = cache.clear()

    private fun key(query: String, page: Int, pageSize: Int): String = "$query:$page:$pageSize"
}
