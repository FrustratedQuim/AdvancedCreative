package com.ratger.acreative.menus.decorationheads.model

import java.time.LocalDate

data class DecorationHeadEntry(
    val apiId: Int?,
    val stableKey: String,
    val name: String,
    val categoryId: Int,
    val textureValue: String,
    val publishedAt: LocalDate?
)
