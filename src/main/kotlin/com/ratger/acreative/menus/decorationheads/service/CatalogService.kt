package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.cache.Cache
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.PageResult
import com.ratger.acreative.menus.decorationheads.persistence.CatalogRepository
import com.ratger.acreative.menus.decorationheads.support.SearchQueryNormalizer

class CatalogService(
    private val cache: Cache,
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val catalogRepository: CatalogRepository,
    private val menuPageSize: Int
) {
    data class WarmupSnapshot(
        val requestedPages: Int,
        val loadedEntries: Int,
        val cachedEntriesAfterWarmup: Int
    )

    private companion object {
        val cyrillicRegex = Regex("\\p{IsCyrillic}")
    }

    fun page(state: DecorationHeadMenuState): PageResult {
        if (state.mode == DecorationHeadMenuMode.RECENT) {
            return PageResult(entries = emptyList(), page = 1, totalPages = 1, totalItems = 0)
        }

        val initialPage = state.page.coerceAtLeast(1)
        val totalItems = count(state)
        val totalPages = if (totalItems == 0) 1 else ((totalItems + menuPageSize - 1) / menuPageSize)
        val page = initialPage.coerceIn(1, totalPages)
        val offset = (page - 1) * menuPageSize
        val entries = findPage(state, page, offset)

        return PageResult(entries = entries, page = page, totalPages = totalPages, totalItems = totalItems)
    }

    fun warmRecentPublishedPages(pageCount: Int): WarmupSnapshot {
        val normalizedPageCount = pageCount.coerceAtLeast(0)
        if (normalizedPageCount == 0) {
            return WarmupSnapshot(
                requestedPages = 0,
                loadedEntries = 0,
                cachedEntriesAfterWarmup = cache.dynamicSize()
            )
        }

        var loadedEntries = 0
        repeat(normalizedPageCount) { pageIndex ->
            val offset = pageIndex * menuPageSize
            val entries = catalogRepository.findRecentPublishedPage(menuPageSize, offset)
            if (entries.isEmpty()) {
                return@repeat
            }
            cache.putAll(entries)
            loadedEntries += entries.size
        }

        return WarmupSnapshot(
            requestedPages = normalizedPageCount,
            loadedEntries = loadedEntries,
            cachedEntriesAfterWarmup = cache.dynamicSize()
        )
    }

    private fun count(state: DecorationHeadMenuState): Int = when (state.mode) {
        DecorationHeadMenuMode.CATEGORY -> countCategory(state.categoryKey)
        DecorationHeadMenuMode.SEARCH -> countSearch(state.searchQuery.orEmpty())
        DecorationHeadMenuMode.RECENT -> 0
    }

    private fun findPage(state: DecorationHeadMenuState, page: Int, offset: Int) = when (state.mode) {
        DecorationHeadMenuMode.CATEGORY -> findCategoryPage(state.categoryKey, offset)
        DecorationHeadMenuMode.SEARCH -> findSearchPage(state.searchQuery.orEmpty(), page, offset)
        DecorationHeadMenuMode.RECENT -> emptyList()
    }

    private fun countCategory(categoryKey: String): Int {
        val definition = categoryRegistry.byKey(categoryKey) ?: return 0
        return if (definition.mode == CategoryMode.NEW) {
            catalogRepository.countRecentPublished()
        } else {
            catalogRepository.countByCategoryIds(categoryResolver.resolveUiCategoryToApiIds(categoryKey))
        }
    }

    private fun findCategoryPage(categoryKey: String, offset: Int) =
        when (categoryRegistry.byKey(categoryKey)?.mode) {
            CategoryMode.NEW -> catalogRepository.findRecentPublishedPage(menuPageSize, offset)
            CategoryMode.CATEGORY_GROUP -> {
                val ids = categoryResolver.resolveUiCategoryToApiIds(categoryKey)
                catalogRepository.findPageByCategoryIds(ids, menuPageSize, offset)
            }
            null -> emptyList()
        }

    private fun countSearch(rawQuery: String): Int {
        val normalized = normalizeQuery(rawQuery) ?: return 0
        return catalogRepository.countBySearch(normalized, containsCyrillic(rawQuery))
    }

    private fun findSearchPage(rawQuery: String, page: Int, offset: Int): List<Entry> {
        val normalized = normalizeQuery(rawQuery) ?: return emptyList()
        val searchByRussianAlias = containsCyrillic(rawQuery)
        cache.searchIndex.get(normalized, page, menuPageSize)?.let { cached ->
            cache.putAll(cached)
            return cached
        }

        val entries = catalogRepository.findSearchPage(normalized, menuPageSize, offset, searchByRussianAlias)
        cache.putAll(entries)
        cache.searchIndex.put(normalized, page, menuPageSize, entries)
        return entries
    }

    private fun normalizeQuery(rawQuery: String): String? = SearchQueryNormalizer.normalize(rawQuery)

    private fun containsCyrillic(rawQuery: String): Boolean = cyrillicRegex.containsMatchIn(rawQuery)
}
