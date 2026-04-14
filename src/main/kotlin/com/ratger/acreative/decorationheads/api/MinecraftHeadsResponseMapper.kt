package com.ratger.acreative.decorationheads.api

import com.ratger.acreative.decorationheads.model.DecorationHeadEntry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

class MinecraftHeadsResponseMapper(
    private val textureValueNormalizer: TextureValueNormalizer
) {
    fun mapHeads(response: JsonObject): List<DecorationHeadEntry> {
        val list = extractHeadsArray(response)
        return list.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj["n"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val categoryId = obj["c"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val apiId = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            val textureValue = textureValueNormalizer.resolveTextureValue(
                value = obj["v"]?.jsonPrimitive?.contentOrNull,
                url = obj["u"]?.jsonPrimitive?.contentOrNull
            ) ?: return@mapNotNull null
            val stableKey = apiId?.toString() ?: textureValueNormalizer.fallbackStableKey(
                obj["u"]?.jsonPrimitive?.contentOrNull ?: textureValue.take(24)
            )
            val publishedAt = obj["published_at"]?.jsonPrimitive?.contentOrNull?.let {
                runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
            }
            DecorationHeadEntry(apiId, stableKey, name, categoryId, textureValue, publishedAt)
        }
    }

    fun readRecords(response: JsonObject, fetched: Int): Int {
        val recordsFromMeta = response["meta"]?.let { metaEl ->
            (metaEl as? JsonObject)?.get("records")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        }
        return recordsFromMeta ?: fetched
    }

    private fun extractHeadsArray(response: JsonObject): JsonArray {
        val direct = response["heads"]?.jsonArray
            ?: response["data"]?.jsonArray
        if (direct != null) return direct

        return response["data"]?.let { dataEl ->
            (dataEl as? JsonObject)?.get("heads")?.jsonArray
        } ?: JsonArray(emptyList())
    }
}
