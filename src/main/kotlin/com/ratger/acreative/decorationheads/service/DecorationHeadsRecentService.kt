package com.ratger.acreative.decorationheads.service

import com.ratger.acreative.decorationheads.model.DecorationHeadEntry
import com.ratger.acreative.decorationheads.persistence.DecorationHeadRecentRepository
import java.util.UUID

class DecorationHeadsRecentService(
    private val recentRepository: DecorationHeadRecentRepository
) {
    fun push(playerId: UUID, entry: DecorationHeadEntry) = recentRepository.push(playerId, entry)
    fun list(playerId: UUID): List<DecorationHeadEntry> = recentRepository.list(playerId)
}
