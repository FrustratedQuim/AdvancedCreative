package com.ratger.acreative.menus.banner.storage

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BannerStorageSessionManager {
    private val sessions = ConcurrentHashMap<UUID, BannerStorageSession>()

    fun get(playerId: UUID): BannerStorageSession? = sessions[playerId]

    fun upsert(session: BannerStorageSession): BannerStorageSession {
        sessions[session.playerId] = session
        return session
    }

    fun remove(playerId: UUID): BannerStorageSession? = sessions.remove(playerId)

    fun playerIdsSnapshot(): Set<UUID> = sessions.keys.toSet()

    fun totalSessions(): Int = sessions.size
}
