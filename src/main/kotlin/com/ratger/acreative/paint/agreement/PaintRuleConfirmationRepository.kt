package com.ratger.acreative.paint.agreement

import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import java.util.UUID

class PaintRuleConfirmationRepository(
    private val database: AdvancedCreativeDatabase
) {
    fun hasConfirmed(playerId: UUID): Boolean = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT 1
            FROM paint_rule_confirmations
            WHERE player_uuid=?
            LIMIT 1
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, playerId.toString())
            ps.executeQuery().use { rs -> rs.next() }
        }
    }

    fun saveConfirmed(playerId: UUID, confirmedAtEpochMillis: Long = System.currentTimeMillis()) {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO paint_rule_confirmations(player_uuid, confirmed_at)
                VALUES (?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, playerId.toString())
                ps.setLong(2, confirmedAtEpochMillis)
                ps.executeUpdate()
            }
        }
    }
}
