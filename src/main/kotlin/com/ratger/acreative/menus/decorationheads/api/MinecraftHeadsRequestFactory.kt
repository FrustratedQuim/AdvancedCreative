package com.ratger.acreative.menus.decorationheads.api

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MinecraftHeadsRequestFactory(
    private val baseUrl: String,
    private val appUuid: String,
    private val apiKey: String,
    private val demo: Boolean
) {
    fun categoriesUri(): URI = URI.create("$baseUrl/api/heads/categories?app_uuid=${encode(appUuid)}")

    fun customHeadsUri(page: Int?, categoryId: Int?): URI {
        val pairs = mutableListOf("app_uuid=${encode(appUuid)}")
        if (demo) pairs += "demo=true"
        if (page != null) pairs += "page=$page"
        if (categoryId != null) pairs += "category_id=$categoryId"
        pairs += "value=true"
        pairs += "id=true"
        return URI.create("$baseUrl/api/heads/custom-heads?${pairs.joinToString("&")}")
    }

    fun apiKey(): String? = apiKey.ifBlank { null }

    private fun encode(v: String): String = URLEncoder.encode(v, StandardCharsets.UTF_8)
}
