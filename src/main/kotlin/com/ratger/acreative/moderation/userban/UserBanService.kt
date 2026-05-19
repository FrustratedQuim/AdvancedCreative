package com.ratger.acreative.moderation.userban

import com.ratger.acreative.core.CoreUserIdentityService
import ru.violence.coreapi.common.api.user.User
import java.util.UUID
import java.util.concurrent.CompletableFuture

class UserBanService(
    private val repository: UserBanStorage,
    private val profileResolver: UserProfileResolver,
    private val identityService: CoreUserIdentityService
) {
    sealed class ToggleResult {
        data object Unbanned : ToggleResult()
        data class Banned(val entry: UserBanEntry) : ToggleResult()
    }

    fun toggle(user: User, reason: String?): CompletableFuture<ToggleResult> {
        val alreadyBanned = repository.find(identityService.resolveUserId(user))
        if (alreadyBanned != null) {
            repository.delete(identityService.resolveUserId(user))
            return CompletableFuture.completedFuture(ToggleResult.Unbanned)
        }

        return profileResolver.resolveSkinSnapshotAsync(user).thenApply { snapshot ->
            val entry = UserBanEntry(
                playerId = identityService.resolveUserId(user),
                playerName = user.getName(),
                reason = reason?.trim()?.takeIf { it.isNotBlank() },
                profileSnapshot = snapshot,
                bannedAtEpochMillis = System.currentTimeMillis()
            )
            repository.save(entry)
            ToggleResult.Banned(entry)
        }
    }

    fun isBanned(playerUuid: UUID): Boolean {
        val playerId = identityService.resolveUserId(playerUuid) ?: return false
        return repository.isBanned(playerId)
    }

    fun isBanned(playerId: Long): Boolean = repository.isBanned(playerId)

    fun page(page: Int): UserBanPageResult<UserBanEntry> = repository.page(page)

    fun unban(playerUuid: UUID): Boolean {
        val playerId = identityService.resolveUserId(playerUuid) ?: return false
        return repository.delete(playerId)
    }

    fun unban(playerId: Long): Boolean = repository.delete(playerId)
}

fun interface UserProfileResolver {
    fun resolveSkinSnapshotAsync(user: User): CompletableFuture<UserProfileSnapshot?>
}
