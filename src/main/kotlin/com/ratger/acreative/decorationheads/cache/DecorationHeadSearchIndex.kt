package com.ratger.acreative.decorationheads.cache

class DecorationHeadSearchIndex(limit: Int) {
    private val cache = DecorationHeadLruCache<String, List<String>>(limit)

    fun put(query: String, stableKeys: List<String>) = cache.put(query, stableKeys)
    fun get(query: String): List<String>? = cache.get(query)
    fun clear() = cache.clear()
}
