package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerPostDraft
import com.ratger.acreative.menus.banner.persistence.BannedPatternRepository
import com.ratger.acreative.menus.banner.persistence.PublishedBannerRepository
import org.bukkit.entity.Player
import java.time.LocalDate
import java.time.ZoneId

class BannerPublicationService(
    private val publishedBannerRepository: PublishedBannerRepository,
    private val bannedPatternRepository: BannedPatternRepository,
    private val authorCache: BannerAuthorCache,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    enum class PublishFailure {
        ALREADY_PUBLISHED_TODAY,
        ALREADY_PUBLISHED_BY_PLAYER,
        LIMIT_REACHED,
        BLOCKED_PATTERN
    }

    data class PublishSuccess(
        val activeCount: Int
    )

    fun publish(player: Player, draft: BannerPostDraft): Result {
        val normalizedBanner = BannerPatternSupport.normalizeForStorage(draft.bannerItem)
            ?: return Result.failure(PublishFailure.BLOCKED_PATTERN)
        val patternSignature = BannerPatternSupport.patternSignature(normalizedBanner)
            ?: return Result.failure(PublishFailure.BLOCKED_PATTERN)

        validationFailure(player, draft, patternSignature)?.let { return Result.failure(it) }

        val publishedAt = System.currentTimeMillis()
        publishedBannerRepository.savePublishedBanner(
            authorUuid = player.uniqueId,
            authorName = player.name,
            title = draft.title?.trim(),
            category = draft.category,
            bannerItem = normalizedBanner,
            patternSignature = patternSignature,
            publishedAtEpochMillis = publishedAt
        )
        authorCache.reload()

        return Result.success(
            PublishSuccess(
                activeCount = publishedBannerRepository.countByAuthor(player.uniqueId)
            )
        )
    }

    fun deletePublishedBanner(entryId: Long): Boolean {
        val deleted = publishedBannerRepository.deleteById(entryId)
        if (deleted) {
            authorCache.reload()
        }
        return deleted
    }

    fun removeBlockedPatternEverywhere(patternSignature: String): Int {
        val removed = publishedBannerRepository.deleteByPatternSignature(patternSignature)
        if (removed > 0) {
            authorCache.reload()
        }
        return removed
    }

    private fun validationFailure(
        player: Player,
        draft: BannerPostDraft,
        patternSignature: String
    ): PublishFailure? {
        if (bannedPatternRepository.isBanned(patternSignature)) {
            return PublishFailure.BLOCKED_PATTERN
        }

        val currentCount = publishedBannerRepository.countByAuthor(player.uniqueId)
        if (currentCount >= BannerPatternSupport.PUBLISH_LIMIT) {
            return PublishFailure.LIMIT_REACHED
        }

        val todayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        if (publishedBannerRepository.existsPublishedToday(patternSignature, draft.category, todayStart)) {
            return PublishFailure.ALREADY_PUBLISHED_TODAY
        }

        if (publishedBannerRepository.hasPublicationHistory(player.uniqueId, patternSignature, draft.category)) {
            return PublishFailure.ALREADY_PUBLISHED_BY_PLAYER
        }

        return null
    }

    sealed class Result {
        data class Success(val value: PublishSuccess) : Result()
        data class Failure(val reason: PublishFailure) : Result()

        companion object {
            fun success(value: PublishSuccess): Result = Success(value)
            fun failure(reason: PublishFailure): Result = Failure(reason)
        }
    }
}
