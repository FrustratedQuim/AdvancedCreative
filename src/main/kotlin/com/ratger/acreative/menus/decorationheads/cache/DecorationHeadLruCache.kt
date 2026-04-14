package com.ratger.acreative.menus.decorationheads.cache

class DecorationHeadLruCache<K, V>(
    private val maxSize: Int
) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun clear() = map.clear()
}
