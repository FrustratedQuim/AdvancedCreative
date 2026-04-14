package com.ratger.acreative.menus.decorationheads.cache

class SearchIndex(limit: Int) {
    private val cache = LruCache<String, List<String>>(limit)

    fun put(query: String, stableKeys: List<String>) = cache.put(query, stableKeys)
    fun get(query: String): List<String>? = cache.get(query)
    fun clear() = cache.clear()
}
