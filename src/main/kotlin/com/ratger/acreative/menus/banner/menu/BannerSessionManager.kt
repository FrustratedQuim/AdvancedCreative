package com.ratger.acreative.menus.banner.menu

import com.ratger.acreative.menus.banner.model.BannerGalleryState
import com.ratger.acreative.menus.banner.model.BannerPostDraft
import com.ratger.acreative.menus.banner.model.MyBannersState
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BannerSessionManager {
    private val publicStateByPlayer = ConcurrentHashMap<UUID, BannerGalleryState>()
    private val myStateByPlayer = ConcurrentHashMap<UUID, MyBannersState>()
    private val postDraftByPlayer = ConcurrentHashMap<UUID, BannerPostDraft>()
    private val myOriginByPlayer = ConcurrentHashMap<UUID, BannerGalleryState>()
    private val lastBannerMenuByPlayer = ConcurrentHashMap<UUID, Boolean>()

    fun publicState(playerId: UUID): BannerGalleryState =
        publicStateByPlayer.computeIfAbsent(playerId) { BannerGalleryState() }

    fun myState(playerId: UUID): MyBannersState =
        myStateByPlayer.computeIfAbsent(playerId) { MyBannersState() }

    fun updatePublicState(playerId: UUID, state: BannerGalleryState) {
        publicStateByPlayer[playerId] = state
    }

    fun updateMyState(playerId: UUID, state: MyBannersState) {
        myStateByPlayer[playerId] = state
    }

    fun setPostDraft(playerId: UUID, draft: BannerPostDraft) {
        postDraftByPlayer[playerId] = draft.normalized()
    }

    fun getPostDraft(playerId: UUID): BannerPostDraft? = postDraftByPlayer[playerId]

    fun markLastMenuAsBanner(playerId: UUID) {
        lastBannerMenuByPlayer[playerId] = true
    }

    fun clearLastBannerMenuMarker(playerId: UUID) {
        lastBannerMenuByPlayer.remove(playerId)
    }

    fun wasLastMenuBanner(playerId: UUID): Boolean = lastBannerMenuByPlayer[playerId] == true

    fun clearTransient(playerId: UUID) {
        postDraftByPlayer.remove(playerId)
        myOriginByPlayer.remove(playerId)
        lastBannerMenuByPlayer.remove(playerId)
    }

    fun rememberMyOrigin(playerId: UUID, state: BannerGalleryState) {
        myOriginByPlayer[playerId] = state
    }

    fun consumeMyOrigin(playerId: UUID): BannerGalleryState? = myOriginByPlayer.remove(playerId)

    fun clear(playerId: UUID) {
        publicStateByPlayer.remove(playerId)
        myStateByPlayer.remove(playerId)
        postDraftByPlayer.remove(playerId)
        myOriginByPlayer.remove(playerId)
        lastBannerMenuByPlayer.remove(playerId)
    }

    fun totalEntriesCount(): Int =
        publicStateByPlayer.size + myStateByPlayer.size + postDraftByPlayer.size + myOriginByPlayer.size + lastBannerMenuByPlayer.size
}
