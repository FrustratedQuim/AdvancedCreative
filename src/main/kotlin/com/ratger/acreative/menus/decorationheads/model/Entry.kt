package com.ratger.acreative.menus.decorationheads.model

import java.time.LocalDate

data class Entry(
    val stableKey: String,
    val name: String,
    val russianAlias: String?,
    val categoryId: Int,
    val textureValue: String,
    val publishedAt: LocalDate?
)
