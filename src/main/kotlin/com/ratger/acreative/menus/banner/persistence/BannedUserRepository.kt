package com.ratger.acreative.menus.banner.persistence

import com.ratger.acreative.menus.banner.model.BannedUserEntry
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.moderation.userban.UserBanRepository
import com.ratger.acreative.moderation.userban.UserBanStorage
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import java.util.UUID

class BannedUserRepository(
    database: AdvancedCreativeDatabase,
    pageSize: Int
) {
    private val repository = UserBanRepository(
        database = database,
        pageSize = pageSize,
        tableName = TABLE_NAME
    )

    fun sharedRepository(): UserBanStorage = repository

    fun isBanned(playerUuid: UUID): Boolean = repository.isBanned(playerUuid)

    fun delete(playerUuid: UUID): Boolean = repository.delete(playerUuid)

    fun page(page: Int): BannerPageResult<BannedUserEntry> {
        val userPage = repository.page(page)
        return BannerPageResult(
            entries = userPage.entries,
            page = userPage.page,
            totalPages = userPage.totalPages,
            totalItems = userPage.totalItems
        )
    }

    private companion object {
        const val TABLE_NAME = "banner_blocked_players"
    }
}
