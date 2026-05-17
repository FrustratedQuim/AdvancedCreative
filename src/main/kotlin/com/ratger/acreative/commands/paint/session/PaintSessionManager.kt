package com.ratger.acreative.commands.paint.session

import com.ratger.acreative.commands.paint.model.PaintSession
import java.util.UUID

class PaintSessionManager(
    private val registry: PaintSessionRegistry
) {

    fun registerSession(session: PaintSession): PaintSession {
        registry.register(session)
        return session
    }

    fun getSession(ownerId: UUID): PaintSession? = registry.get(ownerId)

    fun hasSession(ownerId: UUID): Boolean = registry.contains(ownerId)

    fun removeSession(ownerId: UUID) {
        registry.unregister(ownerId)
    }

    fun getAllSessions(): Collection<PaintSession> = registry.getAll()
}
