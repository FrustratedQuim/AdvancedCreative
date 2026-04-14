package com.ratger.acreative.menus.decorationheads.category

data class CategoryDefinition(
    val key: String,
    val displayName: String,
    val mode: CategoryMode,
    val apiNames: List<String>
)
