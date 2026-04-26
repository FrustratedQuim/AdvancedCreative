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
    private val publicationHistoryCache: BannerPublicationHistoryCache,
    private val configResolver: BannerPublicationConfigResolver,
    private val permissionLimitResolver: BannerPermissionLimitResolver,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    enum class PublishFailure {
        ALREADY_PUBLISHED_TODAY,
        ALREADY_PUBLISHED_BY_PLAYER,
        LIMIT_REACHED,
        BLOCKED_PATTERN
    }

    data class PublishSuccess(
        val activeCount: Int,
        val limit: Int
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
        publicationHistoryCache.rememberPublication(player.uniqueId, patternSignature, draft.category)
        authorCache.reload()

        return Result.success(
            PublishSuccess(
                activeCount = publishedBannerRepository.countByAuthor(player.uniqueId),
                limit = limitFor(player)
            )
        )
    }

    fun limitFor(player: Player): Int {
        val config = configResolver.readConfig()
        return permissionLimitResolver.resolveLimit(player, config.defaultLimit, config.limitsByPermission)
    }

    fun deletePublishedBanner(entryId: Long): Boolean {
        val identity = publishedBannerRepository.publicationIdentity(entryId)
        val deleted = publishedBannerRepository.deleteById(entryId)
        if (deleted) {
            if (identity != null) {
                publicationHistoryCache.forgetPublication(
                    authorUuid = identity.authorUuid,
                    patternSignature = identity.patternSignature,
                    category = identity.category
                )
            } else {
                publicationHistoryCache.clear()
            }
            authorCache.reload()
        }
        return deleted
    }

    fun removeBlockedPatternEverywhere(patternSignature: String): Int {
        val removed = publishedBannerRepository.deleteByPatternSignature(patternSignature)
        if (removed > 0) {
            publicationHistoryCache.clear()
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
        val limit = limitFor(player)
        if (limit >= 0 && currentCount >= limit) {
            return PublishFailure.LIMIT_REACHED
        }

        val todayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        if (publishedBannerRepository.existsPublishedToday(patternSignature, draft.category, todayStart)) {
            return PublishFailure.ALREADY_PUBLISHED_TODAY
        }

        if (publicationHistoryCache.hasPublicationHistory(player.uniqueId, patternSignature, draft.category)) {
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
