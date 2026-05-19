package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.core.CoreUserIdentityService
import com.ratger.acreative.menus.banner.model.BannerCategory
import com.ratger.acreative.menus.banner.persistence.PublishedBannerRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BannerPublicationHistoryCache(
    private val identityService: CoreUserIdentityService,
    private val repository: PublishedBannerRepository
) {
    private val history = ConcurrentHashMap<HistoryKey, Boolean>()

    fun hasPublicationHistory(authorUuid: UUID, patternSignature: String, category: BannerCategory): Boolean {
        val authorId = identityService.resolveUserId(authorUuid) ?: return false
        val key = HistoryKey(authorId, patternSignature, category.key)
        return history.computeIfAbsent(key) {
            repository.hasPublicationHistory(authorUuid, patternSignature, category)
        }
    }

    fun rememberPublication(authorUuid: UUID, patternSignature: String, category: BannerCategory) {
        val authorId = identityService.resolveUserId(authorUuid) ?: return
        history[HistoryKey(authorId, patternSignature, category.key)] = true
    }

    fun forgetPublication(authorUuid: UUID, patternSignature: String, category: BannerCategory) {
        val authorId = identityService.resolveUserId(authorUuid) ?: return
        forgetPublication(authorId, patternSignature, category)
    }

    fun forgetPublication(authorId: Long, patternSignature: String, category: BannerCategory) {
        history.remove(HistoryKey(authorId, patternSignature, category.key))
    }

    fun clear() {
        history.clear()
    }

    fun size(): Int = history.size

    fun snapshotKeys(): List<String> = history.keys.map { "${it.authorId}:${it.categoryKey}:${it.patternSignature}" }

    private data class HistoryKey(
        val authorId: Long,
        val patternSignature: String,
        val categoryKey: String
    )
}
