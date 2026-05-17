package com.ratger.acreative.commands.paint.session

import com.ratger.acreative.commands.paint.model.PaintSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PaintSessionRegistry {

    private val sessions = ConcurrentHashMap<UUID, PaintSession>()

    fun register(session: PaintSession) {
        sessions[session.ownerId] = session
    }

    fun unregister(ownerId: UUID) {
        sessions.remove(ownerId)
    }

    fun get(ownerId: UUID): PaintSession? = sessions[ownerId]

    fun contains(ownerId: UUID): Boolean = sessions.containsKey(ownerId)

    fun getAll(): Collection<PaintSession> = sessions.values

    fun clear() = sessions.clear()
}