package com.ratger.acreative.moderation.userban

import ru.violence.coreapi.common.api.user.User
import java.util.UUID
import java.util.concurrent.CompletableFuture

class UserBanService(
    private val repository: UserBanStorage,
    private val profileResolver: UserProfileResolver
) {
    sealed class ToggleResult {
        data object Unbanned : ToggleResult()
        data class Banned(val entry: UserBanEntry) : ToggleResult()
    }

    fun toggle(user: User, reason: String?): CompletableFuture<ToggleResult> {
        val alreadyBanned = repository.find(user.getUniqueId())
        if (alreadyBanned != null) {
            repository.delete(user.getUniqueId())
            return CompletableFuture.completedFuture(ToggleResult.Unbanned)
        }

        return profileResolver.resolveSkinSnapshotAsync(user).thenApply { snapshot ->
            val entry = UserBanEntry(
                playerUuid = user.getUniqueId(),
                playerName = user.getName(),
                reason = reason?.trim()?.takeIf { it.isNotBlank() },
                profileSnapshot = snapshot,
                bannedAtEpochMillis = System.currentTimeMillis()
            )
            repository.save(entry)
            ToggleResult.Banned(entry)
        }
    }

    fun isBanned(playerUuid: UUID): Boolean = repository.isBanned(playerUuid)

    fun page(page: Int): UserBanPageResult<UserBanEntry> = repository.page(page)

    fun unban(playerUuid: UUID): Boolean = repository.delete(playerUuid)
}

fun interface UserProfileResolver {
    fun resolveSkinSnapshotAsync(user: User): CompletableFuture<UserProfileSnapshot?>
}
