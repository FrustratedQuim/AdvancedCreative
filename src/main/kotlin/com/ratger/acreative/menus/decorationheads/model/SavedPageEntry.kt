package com.ratger.acreative.menus.decorationheads.model

enum class SavedPageSourceMode {
    CATEGORY,
    SEARCH
}

data class SavedPageEntry(
    val id: Long,
    val playerId: Long,
    val sourceMode: SavedPageSourceMode,
    val categoryKey: String,
    val sourcePage: Int,
    val searchQuery: String?,
    val searchQueryKey: String,
    val note: String?,
    val mapColorKey: String?,
    val createdAt: Long,
    val updatedAt: Long
)
