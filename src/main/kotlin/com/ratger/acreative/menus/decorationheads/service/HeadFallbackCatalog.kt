package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

object HeadFallbackCatalog {
    const val BASE_NAME = "heads-fallback"
    const val FILE_NAME = "$BASE_NAME.dat"

    fun resolveFile(dataFolder: File): File = File(dataFolder, FILE_NAME)
}

class HeadFallbackCatalogReader {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(file: File): List<Entry> {
        val rawJson = file.inputStream().use { input ->
            input.buffered().use { buffered ->
                GZIPInputStream(buffered).use { gzip ->
                    gzip.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                        reader.readText()
                    }
                }
            }
        }
        val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return emptyList()

        val fields = (root["f"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()

        if (fields.isEmpty()) return emptyList()

        val indexes = fields.withIndex().associate { it.value to it.index }
        val stableKeyIndex = indexes["stableKey"] ?: return emptyList()
        val displayNameIndex = indexes["displayName"] ?: return emptyList()
        val displayNameRuIndex = indexes["displayNameRu"]
        val sourceCategoryIdIndex = indexes["sourceCategoryId"] ?: return emptyList()
        val textureValueIndex = indexes["textureValue"] ?: return emptyList()

        val rows = root["h"] as? JsonArray ?: return emptyList()
        return rows.mapNotNull { row ->
            val values = row as? JsonArray ?: return@mapNotNull null

            val stableKey = values.stringAt(stableKeyIndex) ?: return@mapNotNull null
            val displayName = values.stringAt(displayNameIndex) ?: return@mapNotNull null
            val sourceCategoryId = values.intAt(sourceCategoryIdIndex) ?: return@mapNotNull null
            val textureValue = values.stringAt(textureValueIndex) ?: return@mapNotNull null
            val displayNameRu = displayNameRuIndex?.let { index -> values.stringAt(index) }

            Entry(
                stableKey = stableKey,
                name = displayName,
                russianAlias = displayNameRu,
                categoryId = sourceCategoryId,
                textureValue = textureValue
            )
        }
    }

    private fun JsonArray.stringAt(index: Int): String? = getOrNull(index)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonArray.intAt(index: Int): Int? = getOrNull(index)?.toIntOrNull()

    private fun JsonElement.toIntOrNull(): Int? = jsonPrimitive.contentOrNull?.toIntOrNull()
}
