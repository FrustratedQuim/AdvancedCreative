package com.ratger.acreative.decorationheads.model

enum class DecorationHeadMenuMode {
    CATEGORY,
    SEARCH,
    RECENT
}

data class DecorationHeadMenuState(
    val mode: DecorationHeadMenuMode,
    val categoryKey: String,
    val page: Int,
    val searchQuery: String? = null,
    val lastNonRecent: DecorationHeadSourcePage? = null
)
