package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.cache.DecorationHeadCache
import com.ratger.acreative.menus.decorationheads.category.DecorationHeadCategoryMode
import com.ratger.acreative.menus.decorationheads.category.DecorationHeadCategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.DecorationHeadCategoryResolver
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadEntry
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadPageResult
import com.ratger.acreative.menus.decorationheads.persistence.DecorationHeadCatalogRepository

class DecorationHeadsCatalogService(
    private val cache: DecorationHeadCache,
    private val categoryRegistry: DecorationHeadCategoryRegistry,
    private val categoryResolver: DecorationHeadCategoryResolver,
    private val catalogRepository: DecorationHeadCatalogRepository,
    private val menuPageSize: Int
) {
    fun page(state: DecorationHeadMenuState): DecorationHeadPageResult {
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
        return DecorationHeadPageResult(slice, page, totalPages, totalItems)
    }

    private fun loadCategoryEntries(categoryKey: String): List<DecorationHeadEntry> {
        val definition = categoryRegistry.byKey(categoryKey) ?: return emptyList()
        return if (definition.mode == DecorationHeadCategoryMode.NEW) {
            catalogRepository.findRecentPublished(10_000)
        } else {
            catalogRepository.findByCategoryIds(categoryResolver.resolveUiCategoryToApiIds(categoryKey))
                .sortedBy { it.name.lowercase() }
        }
    }

    private fun loadSearchEntries(rawQuery: String): List<DecorationHeadEntry> {
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
