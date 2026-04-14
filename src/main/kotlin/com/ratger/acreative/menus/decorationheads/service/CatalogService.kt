package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.cache.Cache
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.PageResult
import com.ratger.acreative.menus.decorationheads.persistence.CatalogRepository

class CatalogService(
    private val cache: Cache,
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val catalogRepository: CatalogRepository,
    private val menuPageSize: Int
) {
    fun page(state: DecorationHeadMenuState): PageResult {
        val all = when (state.mode) {
            DecorationHeadMenuMode.CATEGORY -> loadCategoryEntries(state.categoryKey)
            DecorationHeadMenuMode.SEARCH -> loadSearchEntries(state.searchQuery.orEmpty())
            DecorationHeadMenuMode.RECENT -> emptyList()
        }
        val totalItems = all.size
        val totalPages = if (totalItems == 0) 1 else ((totalItems + menuPageSize - 1) / menuPageSize)
        val page = state.page.coerceIn(1, totalPages)
        val from = (page - 1) * menuPageSize
        val slice = all.drop(from).take(menuPageSize)
        return PageResult(slice, page, totalPages, totalItems)
    }

    private fun loadCategoryEntries(categoryKey: String): List<Entry> {
        val definition = categoryRegistry.byKey(categoryKey) ?: return emptyList()
        return if (definition.mode == CategoryMode.NEW) {
            catalogRepository.findRecentPublished(10_000)
        } else {
            catalogRepository.findByCategoryIds(categoryResolver.resolveUiCategoryToApiIds(categoryKey))
                .sortedBy { it.name.lowercase() }
        }
    }

    private fun loadSearchEntries(rawQuery: String): List<Entry> {
        val normalized = rawQuery.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        val cachedKeys = cache.searchIndex.get(normalized)
        if (cachedKeys != null) {
            return cachedKeys.mapNotNull { cache.get(it) }
        }

        val persisted = catalogRepository.findBySearch(normalized, 10_000)
        cache.putAll(persisted)
        cache.searchIndex.put(normalized, persisted.map { it.stableKey })
        return persisted
    }
}
