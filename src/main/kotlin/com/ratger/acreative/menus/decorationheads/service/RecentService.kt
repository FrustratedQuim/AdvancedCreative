package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.persistence.RecentRepository
import java.util.UUID
import java.util.concurrent.ExecutorService

class RecentService(
    private val recentRepository: RecentRepository,
    private val executor: ExecutorService
) {
    fun push(playerId: UUID, entry: Entry) {
        executor.submit {
            recentRepository.push(playerId, entry)
        }
    }

    fun list(playerId: UUID): List<Entry> = recentRepository.list(playerId)
}
