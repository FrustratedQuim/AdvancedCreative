package com.ratger.acreative.commands.disguise.model

data class DisguiseRequest(
    val type: String?,
    val playerName: String? = null,
    val flags: List<String> = emptyList(),
    val textDisplayText: String? = null,
    val slimeSize: Int? = null
) {
    companion object {
        val SLIME_SIZE_RANGE: IntRange = 1..10
    }
}
