package com.ratger.acreative.decorationheads.category

data class DecorationHeadCategoryDefinition(
    val key: String,
    val displayName: String,
    val mode: DecorationHeadCategoryMode,
    val apiNames: List<String>
)
