package com.ratger.acreative.menus.decorationheads.cache

import com.ratger.acreative.menus.decorationheads.model.Entry

class SearchIndex(limit: Int) {
    private val cache = LruCache<String, List<Entry>>(limit)

    fun put(query: String, page: Int, pageSize: Int, entries: List<Entry>) =
        cache.put(key(query, page, pageSize), entries)

    fun get(query: String, page: Int, pageSize: Int): List<Entry>? =
        cache.get(key(query, page, pageSize))

    fun snapshot(): List<Pair<String, List<Entry>>> = cache.snapshotEntries().map { it.key to it.value }

    fun clear() = cache.clear()

    private fun key(query: String, page: Int, pageSize: Int): String = "$query:$page:$pageSize"
}
