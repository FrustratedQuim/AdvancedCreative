package com.ratger.acreative.moderation.userban

data class UserProfileSnapshot(
    val value: String,
    val signature: String?
)

data class UserBanEntry(
    val playerId: Long,
    val playerName: String,
    val reason: String?,
    val profileSnapshot: UserProfileSnapshot?,
    val bannedAtEpochMillis: Long
)

data class UserBanPageResult<T>(
    val entries: List<T>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int
)
