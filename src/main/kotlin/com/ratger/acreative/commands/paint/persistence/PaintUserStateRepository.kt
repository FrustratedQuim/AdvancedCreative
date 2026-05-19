package com.ratger.acreative.commands.paint.persistence

import com.ratger.acreative.commands.paint.agreement.PaintRuleConfirmationRepository
import com.ratger.acreative.core.CoreUserIdentityService
import com.ratger.acreative.moderation.userban.UserBanEntry
import com.ratger.acreative.moderation.userban.UserBanPageResult
import com.ratger.acreative.moderation.userban.UserBanStorage
import com.ratger.acreative.moderation.userban.UserProfileSnapshot
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import java.sql.ResultSet
import java.util.Locale
import java.util.UUID

class PaintUserStateRepository(
    private val database: AdvancedCreativeDatabase,
    private val identityService: CoreUserIdentityService,
    private val pageSize: Int
) : PaintRuleConfirmationRepository, UserBanStorage {
    override fun hasConfirmed(playerId: UUID): Boolean {
        val userId = identityService.resolveUserId(playerId) ?: return false
        return database.connection().use { conn ->
            conn.prepareStatement(
                """
                SELECT 1
                FROM paint_users
                WHERE player_id=? AND rules_confirmed=1
                LIMIT 1
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs -> rs.next() }
            }
        }
    }

    override fun saveConfirmed(playerId: UUID) {
        val userId = requireNotNull(identityService.resolveUserId(playerId)) {
            "CoreApi user not found for playerUuid=$playerId"
        }
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO paint_users(player_id, rules_confirmed)
                VALUES (?, 1)
                ON CONFLICT(player_id) DO UPDATE SET
                    rules_confirmed=1
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeUpdate()
            }
        }
    }

    override fun find(playerId: Long): UserBanEntry? = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT *
            FROM paint_users
            WHERE player_id=? AND paint_banned=1
            LIMIT 1
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, playerId)
            ps.executeQuery().use { rs -> if (rs.next()) readCurrentEntry(rs) else null }
        }
    }

    override fun isBanned(playerId: Long): Boolean = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT 1
            FROM paint_users
            WHERE player_id=? AND paint_banned=1
            LIMIT 1
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, playerId)
            ps.executeQuery().use { rs -> rs.next() }
        }
    }

    override fun save(entry: UserBanEntry) {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO paint_users(
                    player_id,
                    player_name,
                    player_name_lower,
                    ban_reason,
                    skin_value,
                    skin_signature,
                    paint_banned,
                    banned_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 1, ?)
                ON CONFLICT(player_id) DO UPDATE SET
                    player_name=excluded.player_name,
                    player_name_lower=excluded.player_name_lower,
                    ban_reason=excluded.ban_reason,
                    skin_value=excluded.skin_value,
                    skin_signature=excluded.skin_signature,
                    paint_banned=1,
                    banned_at=excluded.banned_at
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, entry.playerId)
                ps.setString(2, entry.playerName)
                ps.setString(3, entry.playerName.lowercase(Locale.ROOT))
                ps.setString(4, entry.reason)
                ps.setString(5, entry.profileSnapshot?.value)
                ps.setString(6, entry.profileSnapshot?.signature)
                ps.setLong(7, entry.bannedAtEpochMillis)
                ps.executeUpdate()
            }
        }
    }

    override fun delete(playerId: Long): Boolean = database.connection().use { conn ->
        conn.prepareStatement(
            """
            UPDATE paint_users
            SET paint_banned=0,
                ban_reason=NULL,
                banned_at=NULL
            WHERE player_id=? AND paint_banned=1
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, playerId)
            ps.executeUpdate() > 0
        }
    }

    override fun page(page: Int): UserBanPageResult<UserBanEntry> {
        val totalItems = countBanned()
        val totalPages = if (totalItems == 0) 1 else ((totalItems + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(1, totalPages)
        val offset = (safePage - 1) * pageSize

        return UserBanPageResult(
            entries = listBanned(offset),
            page = safePage,
            totalPages = totalPages,
            totalItems = totalItems
        )
    }

    private fun countBanned(): Int = database.connection().use { conn ->
        conn.prepareStatement("SELECT COUNT(*) FROM paint_users WHERE paint_banned=1").use { ps ->
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun listBanned(offset: Int): List<UserBanEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT *
            FROM paint_users
            WHERE paint_banned=1
            ORDER BY banned_at DESC, player_name_lower ASC
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, pageSize)
            ps.setInt(2, offset)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(readCurrentEntry(rs))
                }
            }
        }
    }

    private fun readCurrentEntry(rs: ResultSet): UserBanEntry = UserBanEntry(
        playerId = rs.getLong("player_id"),
        playerName = rs.getString("player_name"),
        reason = rs.getString("ban_reason"),
        profileSnapshot = rs.getString("skin_value")?.let { UserProfileSnapshot(it, rs.getString("skin_signature")) },
        bannedAtEpochMillis = rs.getLong("banned_at")
    )
}
