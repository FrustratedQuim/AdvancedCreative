package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsHttpClient
import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsResponseMapper
import com.ratger.acreative.menus.decorationheads.cache.Cache
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.persistence.CatalogRepository
import com.ratger.acreative.menus.decorationheads.persistence.SyncStateRepository
import java.util.concurrent.ExecutorService

class SyncService(
    private val client: MinecraftHeadsHttpClient,
    private val mapper: MinecraftHeadsResponseMapper,
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val cache: Cache,
    private val catalogRepository: CatalogRepository,
    private val syncStateRepository: SyncStateRepository,
    private val executor: ExecutorService,
    private val logger: java.util.logging.Logger,
    private val warmPages: Int
) {
    fun start() {
        executor.submit {
            val warmupContext = runCatching { syncCategoriesAndWarmCache() }
                .onFailure { logger.warning("Decoration heads warmup failed: ${it.message}") }
                .getOrNull()

            val shouldRunInitialFullSync = runCatching {
                catalogRepository.isCatalogEmpty() || !syncStateRepository.isInitialSyncCompleted()
            }.onFailure {
                logger.warning("Decoration heads sync-state check failed: ${it.message}. Skipping full sync for this startup")
            }.getOrDefault(false)

            if (!shouldRunInitialFullSync) {
                logger.info("Decoration heads: skipping full catalog sync (already initialized)")
                return@submit
            }

            runCatching {
                fullSyncAllConfiguredCategories(startPageByCategoryId = warmupContext?.warmedPagesByCategory.orEmpty())
                syncStateRepository.markInitialSyncCompleted()
                logger.info("Decoration heads: initial full catalog sync completed")
            }.onFailure {
                logger.warning("Decoration heads initial full sync failed: ${it.message}")
            }
        }
    }

    private fun syncCategoriesAndWarmCache(): WarmupContext {
        cache.clearIndexes()
        val categories = client.fetchCategories().associate { it.name to it.id }
        categoryResolver.applyApiCategories(categories, categoryRegistry.definitions)
            .forEach(logger::warning)

        val pagesToWarm = warmPages.coerceAtLeast(1)
        val warmedPagesByCategory = mutableMapOf<Int, Int>()
        categoryRegistry.definitions
            .filter { it.mode == CategoryMode.CATEGORY_GROUP }
            .forEach { definition ->
                categoryResolver.resolveUiCategoryToApiIds(definition.key).forEach { categoryId ->
                    warmCategory(categoryId = categoryId, pagesToWarm = pagesToWarm)
                    warmedPagesByCategory[categoryId] = pagesToWarm
                }
            }

        return WarmupContext(warmedPagesByCategory)
    }

    private fun warmCategory(categoryId: Int, pagesToWarm: Int) {
        for (page in 1..pagesToWarm) {
            val mapped = runCatching {
                val response = client.fetchCustomHeads(page = page, categoryId = categoryId)
                mapper.mapHeads(response)
            }.getOrElse {
                logger.warning("Decoration heads warmup failed for categoryId=$categoryId page=$page: ${it.message}")
                return
            }

            if (mapped.isEmpty()) return

            catalogRepository.upsert(mapped)
            if (page == 1) {
                cache.putAll(mapped)
            }
        }
    }

    fun fullSyncAllConfiguredCategories(startPageByCategoryId: Map<Int, Int> = emptyMap()) {
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
                        catalogRepository.upsert(mapped)
                        page++
                    }
                }
            }
    }

    private data class WarmupContext(
        val warmedPagesByCategory: Map<Int, Int>
    )
}
