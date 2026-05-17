package com.ratger.acreative.commands.paint.interaction

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PaintDirectUseSuppression {
    private val suppressedDirectUseUntilMillis = ConcurrentHashMap<UUID, Long>()

    fun isSuppressed(playerId: UUID): Boolean {
        val untilMillis = suppressedDirectUseUntilMillis[playerId] ?: return false
        val now = System.currentTimeMillis()
        if (now > untilMillis) {
            suppressedDirectUseUntilMillis.remove(playerId, untilMillis)
            return false
        }
        return true
    }

    fun suppress(playerId: UUID, durationMillis: Long) {
        suppressedDirectUseUntilMillis[playerId] = System.currentTimeMillis() + durationMillis
    }
}
