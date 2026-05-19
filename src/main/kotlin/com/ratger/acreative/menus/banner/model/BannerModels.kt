package com.ratger.acreative.menus.banner.model

import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.PatternType
import org.bukkit.inventory.ItemStack
import com.ratger.acreative.moderation.userban.UserBanEntry
import com.ratger.acreative.moderation.userban.UserProfileSnapshot
import java.util.UUID

enum class BannerCategory(
    val key: String,
    val displayName: String
) {
    ALL("all", "Все"),
    MISCELLANEOUS("miscellaneous", "Разное"),
    SYMBOLS("symbols", "Символы"),
    DECORATION("decoration", "Декорации");

    companion object {
        fun fromKey(key: String?): BannerCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: MISCELLANEOUS
    }
}

enum class BannerSort(
    val key: String,
    val displayName: String
) {
    POPULAR("popular", "Популярные"),
    NEW("new", "Новые"),
    OLD("old", "Старые");
}

data class BannerColorDescriptor(
    val dyeColor: DyeColor,
    val bannerMaterial: Material,
    val dyeMaterial: Material,
    val displayName: String
)

data class BannerPatternDescriptor(
    val patternType: PatternType,
    val displayName: String
)

data class PublishedBannerEntry(
    val id: Long,
    val authorId: Long,
    val authorName: String,
    val title: String?,
    val category: BannerCategory,
    val bannerItem: ItemStack,
    val patternSignature: String,
    val takes: Int,
    val publishedAtEpochMillis: Long
)

data class BannedPatternEntry(
    val patternSignature: String,
    val bannerItem: ItemStack,
    val bannedAtEpochMillis: Long
)

typealias BannerProfileSnapshot = UserProfileSnapshot

typealias BannedUserEntry = UserBanEntry

data class BannerPageResult<T>(
    val entries: List<T>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int
)

data class BannerGalleryState(
    val page: Int = 1,
    val category: BannerCategory = BannerCategory.ALL,
    val sort: BannerSort = BannerSort.POPULAR,
    val searchQuery: String? = null,
    val authorFilterUuid: UUID? = null,
    val authorFilterName: String? = null,
    val moderatorMode: Boolean = false,
    val openedFromMainMenu: Boolean = false
)

data class MyBannersState(
    val page: Int = 1,
    val category: BannerCategory = BannerCategory.ALL,
    val sort: BannerSort = BannerSort.POPULAR
)

data class BannerPostDraft(
    val bannerItem: ItemStack,
    val title: String? = null,
    val category: BannerCategory = BannerCategory.MISCELLANEOUS
) {
    fun normalized(): BannerPostDraft = copy(title = normalizeTitle(title))

    companion object {
        const val TITLE_MAX_LENGTH: Int = 64

        fun normalizeTitle(raw: String?): String? {
            return raw
                ?.trim()
                ?.take(TITLE_MAX_LENGTH)
                ?.takeIf(String::isNotBlank)
        }
    }
}
