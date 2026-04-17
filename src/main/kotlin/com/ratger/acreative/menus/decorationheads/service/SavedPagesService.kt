package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.SavedPageEntry
import com.ratger.acreative.menus.decorationheads.model.SavedPageSourceMode
import com.ratger.acreative.menus.decorationheads.persistence.SavedPagesRepository
import com.ratger.acreative.menus.decorationheads.support.MapPreviewColorPalette
import com.ratger.acreative.menus.decorationheads.support.SearchQueryNormalizer
import java.util.UUID

class SavedPagesService(
    private val repository: SavedPagesRepository,
    private val maxSavedPages: Int = 45
) {
    enum class ToggleResult {
        SAVED,
        DELETED,
        LIMIT_REACHED,
        NOT_ALLOWED
    }

    fun canSaveState(state: DecorationHeadMenuState): Boolean = when (state.mode) {
        DecorationHeadMenuMode.CATEGORY,
        DecorationHeadMenuMode.SEARCH -> true
        DecorationHeadMenuMode.RECENT -> false
    }

    fun findByCurrentSource(playerId: UUID, state: DecorationHeadMenuState): SavedPageEntry? {
        val source = sourceFromState(state) ?: return null
        return repository.findBySource(playerId, source.sourceMode, source.categoryKey, source.sourcePage, source.searchQueryKey)
    }

    fun toggleForCurrentState(playerId: UUID, state: DecorationHeadMenuState): ToggleResult {
        val source = sourceFromState(state) ?: return ToggleResult.NOT_ALLOWED
        val existing = repository.findBySource(playerId, source.sourceMode, source.categoryKey, source.sourcePage, source.searchQueryKey)
        if (existing != null) {
            repository.deleteById(playerId, existing.id)
            return ToggleResult.DELETED
        }
        if (repository.countByPlayer(playerId) >= maxSavedPages) {
            return ToggleResult.LIMIT_REACHED
        }
        repository.insert(
            playerUuid = playerId,
            sourceMode = source.sourceMode,
            categoryKey = source.categoryKey,
            sourcePage = source.sourcePage,
            searchQuery = source.searchQuery,
            searchQueryKey = source.searchQueryKey,
            note = null,
            mapColorKey = null
        )
        return ToggleResult.SAVED
    }

    fun countByPlayer(playerId: UUID): Int = repository.countByPlayer(playerId)
    fun listByPlayer(playerId: UUID): List<SavedPageEntry> = repository.listByPlayer(playerId)
    fun listByPlayerAndCategory(playerId: UUID, categoryKey: String?): List<SavedPageEntry> = repository.listByPlayerAndCategory(playerId, categoryKey)
    fun findById(playerId: UUID, id: Long): SavedPageEntry? = repository.findById(playerId, id)

    fun updateNote(playerId: UUID, id: Long, note: String?): Boolean {
        val normalized = note?.trim()?.takeIf { it.isNotBlank() }
        return repository.updateNote(playerId, id, normalized)
    }

    fun updateSourcePage(playerId: UUID, id: Long, sourcePage: Int): Boolean {
        if (sourcePage < 1) return false
        return repository.updateSourcePage(playerId, id, sourcePage)
    }

    fun cycleMapColorKey(playerId: UUID, id: Long, forward: Boolean): SavedPageEntry? {
        val entry = repository.findById(playerId, id) ?: return null
        val next = if (forward) MapPreviewColorPalette.nextKey(entry.mapColorKey) else MapPreviewColorPalette.previousKey(entry.mapColorKey)
        val persisted = next.takeUnless { it == MapPreviewColorPalette.ORDINARY_KEY }
        repository.updateMapColorKey(playerId, id, persisted)
        return repository.findById(playerId, id)
    }

    fun delete(playerId: UUID, id: Long): Boolean = repository.deleteById(playerId, id)

    fun toMenuState(entry: SavedPageEntry): DecorationHeadMenuState = when (entry.sourceMode) {
        SavedPageSourceMode.CATEGORY -> DecorationHeadMenuState(
            mode = DecorationHeadMenuMode.CATEGORY,
            categoryKey = entry.categoryKey,
            page = entry.sourcePage,
            searchQuery = null,
            lastNonRecent = null
        )
        SavedPageSourceMode.SEARCH -> DecorationHeadMenuState(
            mode = DecorationHeadMenuMode.SEARCH,
            categoryKey = entry.categoryKey,
            page = entry.sourcePage,
            searchQuery = entry.searchQuery,
            lastNonRecent = null
        )
    }

    private data class SourceSnapshot(
        val sourceMode: SavedPageSourceMode,
        val categoryKey: String,
        val sourcePage: Int,
        val searchQuery: String?,
        val searchQueryKey: String
    )

    private fun sourceFromState(state: DecorationHeadMenuState): SourceSnapshot? {
        return when (state.mode) {
            DecorationHeadMenuMode.CATEGORY -> SourceSnapshot(
                sourceMode = SavedPageSourceMode.CATEGORY,
                categoryKey = state.categoryKey,
                sourcePage = state.page.coerceAtLeast(1),
                searchQuery = null,
                searchQueryKey = SearchQueryNormalizer.normalize(null) ?: ""
            )
            DecorationHeadMenuMode.SEARCH -> {
                val normalized = SearchQueryNormalizer.normalize(state.searchQuery) ?: return null
                SourceSnapshot(
                    sourceMode = SavedPageSourceMode.SEARCH,
                    categoryKey = state.categoryKey,
                    sourcePage = state.page.coerceAtLeast(1),
                    searchQuery = state.searchQuery,
                    searchQueryKey = normalized
                )
            }
            DecorationHeadMenuMode.RECENT -> null
        }
    }
}
