package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsHttpClient
import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsResponseMapper
import com.ratger.acreative.menus.decorationheads.cache.DecorationHeadCache
import com.ratger.acreative.menus.decorationheads.category.DecorationHeadCategoryMode
import com.ratger.acreative.menus.decorationheads.category.DecorationHeadCategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.DecorationHeadCategoryResolver
import com.ratger.acreative.menus.decorationheads.persistence.DecorationHeadCatalogRepository
import java.util.concurrent.ExecutorService

class DecorationHeadsSyncService(
    private val client: MinecraftHeadsHttpClient,
    private val mapper: MinecraftHeadsResponseMapper,
    private val categoryRegistry: DecorationHeadCategoryRegistry,
    private val categoryResolver: DecorationHeadCategoryResolver,
    private val cache: DecorationHeadCache,
    private val catalogRepository: DecorationHeadCatalogRepository,
    private val executor: ExecutorService,
    private val logger: java.util.logging.Logger,
    private val warmPages: Int,
    private val menuPageSize: Int
) {
    fun start() {
        executor.submit {
            runCatching { syncCategoriesAndWarmCache() }
                .onFailure { logger.warning("Decoration heads warmup failed: ${it.message}") }
            runCatching { fullSyncAllConfiguredCategories() }
                .onFailure { logger.warning("Decoration heads full sync failed: ${it.message}") }
        }
    }

    private fun syncCategoriesAndWarmCache() {
        val categories = client.fetchCategories().associate { it.name to it.id }
        categoryResolver.applyApiCategories(categories, categoryRegistry.definitions)
            .forEach(logger::warning)

        val warmLimit = (warmPages.coerceAtLeast(1)) * menuPageSize
        categoryRegistry.definitions
            .filter { it.mode == DecorationHeadCategoryMode.CATEGORY_GROUP }
            .forEach { definition ->
                val aggregated = mutableListOf<com.ratger.acreative.menus.decorationheads.model.DecorationHeadEntry>()
                categoryResolver.resolveUiCategoryToApiIds(definition.key).forEach { categoryId ->
                    val response = client.fetchCustomHeads(page = 1, categoryId = categoryId)
                    aggregated += mapper.mapHeads(response)
                }
                val warm = aggregated.take(warmLimit)
                cache.putAll(warm, pinned = true)
                catalogRepository.upsert(aggregated)
            }
    }

    fun fullSyncAllConfiguredCategories() {
        categoryRegistry.definitions
            .filter { it.mode == DecorationHeadCategoryMode.CATEGORY_GROUP }
            .forEach { definition ->
                categoryResolver.resolveUiCategoryToApiIds(definition.key).forEach { apiCategoryId ->
                    var page = 1
                    var fetched = 0
                    var total = Int.MAX_VALUE
                    while (fetched < total) {
                        val response = client.fetchCustomHeads(page = page, categoryId = apiCategoryId)
                        val mapped = mapper.mapHeads(response)
                        if (mapped.isEmpty()) break
                        fetched += mapped.size
                        total = mapper.readRecords(response, fetched)
                        catalogRepository.upsert(mapped)
                        cache.putAll(mapped, pinned = false)
                        page++
                    }
                }
            }
    }
}
