package com.ratger.acreative.commands.paint.agreement

import java.util.UUID

interface PaintRuleConfirmationRepository {
    fun hasConfirmed(playerId: UUID): Boolean

    fun saveConfirmed(playerId: UUID)
}
