package com.ratger.acreative.menus.decorationheads.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class MinecraftHeadsHttpClient(
    private val requestFactory: MinecraftHeadsRequestFactory,
    connectTimeoutMs: Long,
    private val readTimeoutMs: Long
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build()

    fun fetchCategories(): List<MinecraftHeadsCategoryDto> {
        val root = getJson(requestFactory.categoriesUri())
        val candidates = listOf("categories", "data")
            .firstNotNullOfOrNull { root[it]?.jsonArray }
            ?: if (root.values.firstOrNull() is kotlinx.serialization.json.JsonArray) root.values.first().jsonArray else emptyList()
        return candidates.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val name = obj["n"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            MinecraftHeadsCategoryDto(id, name)
        }
    }

    fun fetchCustomHeads(page: Int?, categoryId: Int?): JsonObject = getJson(requestFactory.customHeadsUri(page, categoryId))

    private fun getJson(uri: java.net.URI): JsonObject {
        val builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(readTimeoutMs))
            .GET()
            .header("Accept", "application/json")
        requestFactory.apiKey()?.let { builder.header("api-key", it) }
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("MinecraftHeads HTTP ${response.statusCode()} for $uri")
        }
        val parsed = json.parseToJsonElement(response.body())
        return when (parsed) {
            is JsonObject -> parsed
            else -> buildJsonObject { put("data", JsonPrimitive(response.body())) }
        }
    }
}
