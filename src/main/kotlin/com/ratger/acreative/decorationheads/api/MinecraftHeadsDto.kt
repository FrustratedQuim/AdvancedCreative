package com.ratger.acreative.decorationheads.api

import kotlinx.serialization.json.JsonObject

data class MinecraftHeadsCategoryDto(
    val id: Int,
    val name: String
)

data class MinecraftHeadsHeadDto(
    val json: JsonObject
)
