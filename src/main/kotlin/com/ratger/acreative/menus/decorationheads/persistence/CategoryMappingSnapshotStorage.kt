package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.core.PluginCacheDirectory
import com.ratger.acreative.menus.decorationheads.category.ApiCategoryMapping
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Logger

class CategoryMappingSnapshotStorage(
    dataFolder: File,
    private val logger: Logger
) {
    private val cacheDirectory = PluginCacheDirectory.ensure(dataFolder.toPath())
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): List<ApiCategoryMapping> {
        val file = resolveFile()
        if (!file.isFile) return emptyList()

        return runCatching { read(file) }
            .onFailure { logger.warning("Failed to load decoration heads category snapshot ${file.name}: ${it.message}") }
            .getOrDefault(emptyList())
    }

    fun save(entries: Collection<ApiCategoryMapping>) {
        val normalized = entries
            .filter { it.name.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        if (normalized.isEmpty()) return

        val targetPath = resolveFile().toPath()
        val tempPath = Files.createTempFile(cacheDirectory, "head-categories.", ".json.tmp")
        val payload = json.encodeToString(JsonObject.serializer(), toJson(normalized))

        try {
            Files.writeString(tempPath, payload, Charsets.UTF_8)
            try {
                Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (error: Exception) {
            logger.warning("Failed to save decoration heads category snapshot ${targetPath.fileName}: ${error.message}")
        } finally {
            runCatching { Files.deleteIfExists(tempPath) }
        }
    }

    private fun resolveFile(): File = cacheDirectory.resolve(FILE_NAME).toFile()

    private fun read(file: File): List<ApiCategoryMapping> {
        val root = json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
        val categories = root["categories"]?.jsonArray ?: JsonArray(emptyList())
        return categories.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ApiCategoryMapping(id = id, name = name)
        }
    }

    private fun toJson(entries: Collection<ApiCategoryMapping>): JsonObject = buildJsonObject {
        put("schemaVersion", JsonPrimitive(SCHEMA_VERSION))
        put("savedAt", JsonPrimitive(System.currentTimeMillis()))
        put("categories", buildJsonArray {
            entries.forEach { entry ->
                add(
                    buildJsonObject {
                        put("id", JsonPrimitive(entry.id))
                        put("name", JsonPrimitive(entry.name))
                    }
                )
            }
        })
    }

    private companion object {
        const val FILE_NAME = "heads-categories.json"
        const val SCHEMA_VERSION = 1
    }
}
