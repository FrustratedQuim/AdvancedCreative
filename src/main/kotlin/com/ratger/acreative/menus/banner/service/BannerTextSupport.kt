package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object BannerTextSupport {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd.yyyy", Locale.ROOT)

    fun escapeMiniMessage(input: String): String {
        return input
            .replace("§", "§\u200B")
            .replace("<", "\\<")
            .replace(">", "\\>")
    }

    fun titleWithPages(baseTitle: String, page: Int, totalPages: Int): String {
        return if (totalPages >= 2) {
            "$baseTitle [$page/$totalPages]"
        } else {
            baseTitle
        }
    }

    fun formatTakes(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        val suffix = if (mod100 in 11..14) {
            "раз"
        } else {
            when (mod10) {
                1 -> "раз"
                2, 3, 4 -> "раза"
                else -> "раз"
            }
        }
        return "$count $suffix"
    }

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(dateFormatter)

    fun galleryBaseTitle(authorFilterName: String?, category: BannerCategory): String {
        val scope = if (authorFilterName.isNullOrBlank()) {
            if (category == BannerCategory.ALL) "Все" else category.displayName
        } else {
            escapeMiniMessage(authorFilterName)
        }

        return if (authorFilterName.isNullOrBlank() || category == BannerCategory.ALL) {
            "▍ Флаги → $scope"
        } else {
            "▍ Флаги → $scope → ${category.displayName}"
        }
    }

    fun myFlagsBaseTitle(category: BannerCategory): String =
        "▍ Флаги → Мои → ${category.displayName}"
}
