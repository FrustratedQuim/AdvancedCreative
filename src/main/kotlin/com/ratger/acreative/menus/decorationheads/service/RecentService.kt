package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.persistence.RecentRepository
import java.util.UUID

class RecentService(
    private val recentRepository: RecentRepository
) {
    fun push(playerId: UUID, entry: Entry) = recentRepository.push(playerId, entry)
    fun list(playerId: UUID): List<Entry> = recentRepository.list(playerId)
}
