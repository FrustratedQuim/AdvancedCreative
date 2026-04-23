package com.ratger.acreative.menus.decorationheads.cache

class LruCache<K, V>(
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
    fun size(): Int = map.size

    @Synchronized
    fun snapshotEntries(): List<Map.Entry<K, V>> = map.entries.map { java.util.AbstractMap.SimpleEntry(it.key, it.value) }

    @Synchronized
    fun clear() = map.clear()
}
