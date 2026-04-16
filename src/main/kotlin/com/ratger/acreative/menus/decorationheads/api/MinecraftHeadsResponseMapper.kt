package com.ratger.acreative.menus.decorationheads.api

import com.ratger.acreative.menus.decorationheads.model.Entry
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Base64

class MinecraftHeadsResponseMapper {
    private companion object {
        const val TEXTURE_HOST_PREFIX = "http://textures.minecraft.net/texture/"
        val bareTextureHashRegex = Regex("^[0-9a-fA-F]{32,128}$")
    }

    fun mapHeads(response: JsonObject): List<Entry> {
        val list = extractHeadsArray(response)
        return list.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val name = obj["n"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val categoryId = obj["c"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val apiId = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            val textureValue = resolveTextureValue(
                value = obj["v"]?.jsonPrimitive?.contentOrNull,
                url = obj["u"]?.jsonPrimitive?.contentOrNull
            ) ?: return@mapNotNull null
            val stableKey = apiId?.toString() ?: fallbackStableKey(
                obj["u"]?.jsonPrimitive?.contentOrNull ?: textureValue.take(24)
            )
            val publishedAt = obj["published_at"]?.jsonPrimitive?.contentOrNull?.let {
                runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
            }
            Entry(
                stableKey = stableKey,
                name = name,
                russianAlias = null,
                categoryId = categoryId,
                textureValue = textureValue,
                publishedAt = publishedAt
            )
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

    private fun resolveTextureValue(value: String?, url: String?): String? {
        val normalizedFromValue = value?.takeUnless { it.isBlank() }
        if (normalizedFromValue != null) return normalizedFromValue

        val normalizedUrl = normalizeTextureUrl(url ?: return null) ?: return null
        return encodeTextureValue(normalizedUrl)
    }

    private fun fallbackStableKey(url: String): String {
        val suffix = url.substringAfterLast('/').ifBlank { Integer.toHexString(url.hashCode()) }
        return "u:$suffix"
    }

    private fun normalizeTextureUrl(raw: String): String? {
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) return candidate

        val suffix = candidate.substringAfterLast('/').trim()
        if (!bareTextureHashRegex.matches(suffix)) return null
        return "$TEXTURE_HOST_PREFIX$suffix"
    }

    private fun encodeTextureValue(url: String): String {
        val payload = "{\"textures\":{\"SKIN\":{\"url\":\"$url\"}}}"
        return Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
    }
}
