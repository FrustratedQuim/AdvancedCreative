package com.ratger.acreative.menus.decorationheads.cache

import java.util.concurrent.ConcurrentHashMap

class DecorationHeadPageIndex {
    private val pages = ConcurrentHashMap<String, List<String>>()

    fun get(categoryKey: String, page: Int): List<String>? = pages["$categoryKey:$page"]

    fun clear() = pages.clear()
}
