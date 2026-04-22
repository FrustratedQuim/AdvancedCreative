package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannedPatternEntry
import com.ratger.acreative.menus.banner.model.BannedUserEntry
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.menus.banner.persistence.BannedPatternRepository
import com.ratger.acreative.menus.banner.persistence.BannedUserRepository
import org.bukkit.entity.Player
import ru.violence.coreapi.common.api.user.User
import java.util.UUID
import java.util.concurrent.CompletableFuture

class BannerModerationService(
    private val bannedPatternRepository: BannedPatternRepository,
    private val bannedUserRepository: BannedUserRepository,
    private val publicationService: BannerPublicationService,
    private val playerLookupService: BannerPlayerLookupService
) {
    enum class PatternToggleResult {
        BANNED,
        UNBANNED,
        INVALID
    }

    sealed class UserToggleResult {
        data object Unbanned : UserToggleResult()
        data class Banned(val entry: BannedUserEntry) : UserToggleResult()
    }

    fun togglePattern(player: Player): PatternToggleResult {
        val handBanner = BannerPatternSupport.normalizeForStorage(player.inventory.itemInMainHand) ?: return PatternToggleResult.INVALID
        val patternSignature = BannerPatternSupport.patternSignature(handBanner) ?: return PatternToggleResult.INVALID

        return if (bannedPatternRepository.isBanned(patternSignature)) {
            bannedPatternRepository.delete(patternSignature)
            PatternToggleResult.UNBANNED
        } else {
            bannedPatternRepository.save(patternSignature, handBanner, System.currentTimeMillis())
            publicationService.removeBlockedPatternEverywhere(patternSignature)
            PatternToggleResult.BANNED
        }
    }

    fun toggleUserBan(user: User, reason: String?): CompletableFuture<UserToggleResult> {
        val alreadyBanned = bannedUserRepository.find(user.getUniqueId())
        if (alreadyBanned != null) {
            bannedUserRepository.delete(user.getUniqueId())
            return CompletableFuture.completedFuture(UserToggleResult.Unbanned)
        }

        return playerLookupService.resolveSkinSnapshotAsync(user).thenApply { snapshot ->
            val entry = BannedUserEntry(
                playerUuid = user.getUniqueId(),
                playerName = user.getName(),
                reason = reason?.trim()?.takeIf { it.isNotBlank() },
                profileSnapshot = snapshot,
                bannedAtEpochMillis = System.currentTimeMillis()
            )
            bannedUserRepository.save(entry)
            UserToggleResult.Banned(entry)
        }
    }

    fun isUserBanned(playerUuid: UUID): Boolean = bannedUserRepository.isBanned(playerUuid)

    fun bannedPatternsPage(page: Int): BannerPageResult<BannedPatternEntry> = bannedPatternRepository.page(page)

    fun bannedUsersPage(page: Int): BannerPageResult<BannedUserEntry> = bannedUserRepository.page(page)

    fun unbanPattern(patternSignature: String): Boolean = bannedPatternRepository.delete(patternSignature)

    fun unbanUser(playerUuid: UUID): Boolean = bannedUserRepository.delete(playerUuid)
}
