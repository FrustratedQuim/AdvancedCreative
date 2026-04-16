package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.persistence.RecentRepository
import java.util.UUID
import java.util.concurrent.ExecutorService

class RecentService(
    private val recentRepository: RecentRepository,
    private val executor: ExecutorService
) {
    fun push(playerId: UUID, entry: Entry, onCountUpdated: ((Int) -> Unit)? = null) {
        executor.submit {
            recentRepository.push(playerId, entry)
            onCountUpdated?.invoke(recentRepository.list(playerId).size)
        }
    }

    fun list(playerId: UUID): List<Entry> = recentRepository.list(playerId)
}
