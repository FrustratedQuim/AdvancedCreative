package com.ratger.acreative.menus.decorationheads.model

data class DecorationHeadSourcePage(
    val mode: DecorationHeadMenuMode,
    val categoryKey: String,
    val page: Int,
    val searchQuery: String?
)
