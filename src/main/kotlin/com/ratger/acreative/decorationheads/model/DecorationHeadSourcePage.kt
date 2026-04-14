package com.ratger.acreative.decorationheads.model

data class DecorationHeadSourcePage(
    val mode: DecorationHeadMenuMode,
    val categoryKey: String,
    val page: Int,
    val searchQuery: String?
)
