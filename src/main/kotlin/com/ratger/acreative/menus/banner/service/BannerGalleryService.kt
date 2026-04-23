package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerGalleryState
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.menus.banner.model.MyBannersState
import com.ratger.acreative.menus.banner.model.PublishedBannerEntry
import com.ratger.acreative.menus.banner.persistence.PublishedBannerRepository
import org.bukkit.entity.Player

class BannerGalleryService(
    private val publishedBannerRepository: PublishedBannerRepository,
    private val takeCooldownService: BannerTakeCooldownService
) {
    fun publicPage(state: BannerGalleryState): BannerPageResult<PublishedBannerEntry> =
        publishedBannerRepository.page(state)

    fun myPage(player: Player, state: MyBannersState): BannerPageResult<PublishedBannerEntry> {
        return publishedBannerRepository.page(
            BannerGalleryState(
                page = state.page,
                category = state.category,
                sort = state.sort,
                authorFilterUuid = player.uniqueId
            )
        )
    }

    fun myCount(player: Player): Int = publishedBannerRepository.countByAuthor(player.uniqueId)

    fun countByAuthorName(authorName: String): Int = publishedBannerRepository.countByAuthorName(authorName)

    fun recordTakeIfNeeded(entry: PublishedBannerEntry, player: Player) {
        if (takeCooldownService.shouldCountTake(entry.id, player.uniqueId)) {
            publishedBannerRepository.incrementTakes(entry.id)
        }
    }
}
