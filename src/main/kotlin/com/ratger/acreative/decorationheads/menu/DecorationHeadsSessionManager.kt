package com.ratger.acreative.decorationheads.menu

import com.ratger.acreative.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.decorationheads.model.DecorationHeadSourcePage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DecorationHeadsSessionManager(
    private val defaultCategory: String
) {
    private val byPlayer = ConcurrentHashMap<UUID, DecorationHeadMenuState>()

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
            lastNonRecent = DecorationHeadSourcePage(old.mode, old.categoryKey, old.page, old.searchQuery)
        )
    }

    fun backFromRecent(playerId: UUID): DecorationHeadMenuState {
        val old = getOrCreate(playerId)
        val source = old.lastNonRecent ?: DecorationHeadSourcePage(DecorationHeadMenuMode.CATEGORY, defaultCategory, 1, null)
        val restored = DecorationHeadMenuState(source.mode, source.categoryKey, source.page, source.searchQuery, null)
        byPlayer[playerId] = restored
        return restored
    }

    fun clear(playerId: UUID) {
        byPlayer.remove(playerId)
    }
}
