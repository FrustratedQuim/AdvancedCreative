package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerCategory
import com.ratger.acreative.menus.banner.persistence.PublishedBannerRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BannerPublicationHistoryCache(
    private val repository: PublishedBannerRepository
) {
    private val history = ConcurrentHashMap<HistoryKey, Boolean>()

    fun hasPublicationHistory(authorUuid: UUID, patternSignature: String, category: BannerCategory): Boolean {
        val key = HistoryKey(authorUuid, patternSignature, category.key)
        return history.computeIfAbsent(key) {
            repository.hasPublicationHistory(authorUuid, patternSignature, category)
        }
    }

    fun rememberPublication(authorUuid: UUID, patternSignature: String, category: BannerCategory) {
        history[HistoryKey(authorUuid, patternSignature, category.key)] = true
    }

    private data class HistoryKey(
        val authorUuid: UUID,
        val patternSignature: String,
        val categoryKey: String
    )
}
