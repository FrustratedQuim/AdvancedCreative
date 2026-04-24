package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsHttpClient
import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsResponseMapper
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.model.Entry

class SyncService(
    private val client: MinecraftHeadsHttpClient,
    private val mapper: MinecraftHeadsResponseMapper,
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val logger: java.util.logging.Logger
) {
    fun hasApiKey(): Boolean = client.hasApiKey()

    fun refreshCategoryMappings() {
        runCatching {
            syncCategoryMappings().forEach(logger::warning)
        }.onFailure {
            logger.warning("Decoration heads category refresh failed: ${it.message}")
        }
    }

    fun fetchAllConfiguredHeads(startPageByCategoryId: Map<Int, Int> = emptyMap()): List<Entry> {
        syncCategoryMappings().forEach(logger::warning)

        val collected = LinkedHashMap<String, Entry>()
        categoryRegistry.definitions
            .filter { it.mode == CategoryMode.CATEGORY_GROUP }
            .forEach { definition ->
                categoryResolver.resolveUiCategoryToApiIds(definition.key).forEach { apiCategoryId ->
                    val startPage = (startPageByCategoryId[apiCategoryId] ?: 0) + 1
                    var page = startPage.coerceAtLeast(1)
                    var fetched = 0
                    var total = Int.MAX_VALUE
                    while (fetched < total) {
                        val response = client.fetchCustomHeads(page = page, categoryId = apiCategoryId)
                        val mapped = mapper.mapHeads(response)
                        if (mapped.isEmpty()) break
                        fetched += mapped.size
                        total = mapper.readRecords(response, fetched)
                        mapped.forEach { collected[it.stableKey] = it }
                        page++
                    }
                }
            }
        return collected.values.toList()
    }

    private fun syncCategoryMappings(): List<String> {
        val categories = client.fetchCategories().associate { it.name to it.id }
        return categoryResolver.applyApiCategories(categories, categoryRegistry.definitions)
    }
}
