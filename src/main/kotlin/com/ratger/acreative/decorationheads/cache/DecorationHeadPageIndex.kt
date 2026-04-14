package com.ratger.acreative.decorationheads.cache

import java.util.concurrent.ConcurrentHashMap

class DecorationHeadPageIndex {
    private val pages = ConcurrentHashMap<String, List<String>>()

    fun put(categoryKey: String, page: Int, keys: List<String>) {
        pages["$categoryKey:$page"] = keys
    }

    fun get(categoryKey: String, page: Int): List<String>? = pages["$categoryKey:$page"]

    fun clear() = pages.clear()
}
