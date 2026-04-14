package com.ratger.acreative.menus.decorationheads.model

data class SourcePage(
    val mode: DecorationHeadMenuMode,
    val categoryKey: String,
    val page: Int,
    val searchQuery: String?
)
