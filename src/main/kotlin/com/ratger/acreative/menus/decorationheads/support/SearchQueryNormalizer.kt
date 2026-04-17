package com.ratger.acreative.menus.decorationheads.support

object SearchQueryNormalizer {
    fun normalize(rawQuery: String?): String? = rawQuery?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
}
