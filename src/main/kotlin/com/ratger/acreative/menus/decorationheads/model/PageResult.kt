package com.ratger.acreative.menus.decorationheads.model

data class PageResult(
    val entries: List<Entry>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int
)
