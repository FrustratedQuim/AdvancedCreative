package com.ratger.acreative.menus.decorationheads.model

data class DecorationHeadPageResult(
    val entries: List<DecorationHeadEntry>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int
)
