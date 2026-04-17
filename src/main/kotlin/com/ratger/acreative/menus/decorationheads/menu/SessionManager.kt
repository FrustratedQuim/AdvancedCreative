package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.SourcePage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SessionManager(
    private val defaultCategory: String
) {
    private val byPlayer = ConcurrentHashMap<UUID, DecorationHeadMenuState>()
    private val recentCategoryByPlayer = ConcurrentHashMap<UUID, String>()

    fun getOrCreate(playerId: UUID): DecorationHeadMenuState = byPlayer.computeIfAbsent(playerId) {
        DecorationHeadMenuState(DecorationHeadMenuMode.CATEGORY, defaultCategory, 1, null, null)
    }

    fun update(playerId: UUID, state: DecorationHeadMenuState) {
        byPlayer[playerId] = state
    }

    fun setRecentMode(playerId: UUID) {
        val old = getOrCreate(playerId)
        byPlayer[playerId] = DecorationHeadMenuState(
            mode = DecorationHeadMenuMode.RECENT,
            categoryKey = old.categoryKey,
            page = 1,
            searchQuery = old.searchQuery,
            lastNonRecent = SourcePage(old.mode, old.categoryKey, old.page, old.searchQuery)
        )
    }

    fun getRecentCategory(playerId: UUID): String = recentCategoryByPlayer[playerId] ?: defaultCategory

    fun setRecentCategory(playerId: UUID, categoryKey: String) {
        recentCategoryByPlayer[playerId] = categoryKey
    }

    fun backFromRecent(playerId: UUID): DecorationHeadMenuState {
        val old = getOrCreate(playerId)
        val source = old.lastNonRecent ?: SourcePage(DecorationHeadMenuMode.CATEGORY, defaultCategory, 1, null)
        val restored = DecorationHeadMenuState(source.mode, source.categoryKey, source.page, source.searchQuery, null)
        byPlayer[playerId] = restored
        return restored
    }

    fun clear(playerId: UUID) {
        byPlayer.remove(playerId)
        recentCategoryByPlayer.remove(playerId)
    }
}
