package com.ratger.acreative.menus.banner.persistence

import com.ratger.acreative.menus.banner.model.BannedUserEntry
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.menus.banner.model.BannerProfileSnapshot
import java.sql.ResultSet
import java.util.Locale
import java.util.UUID

class BannedUserRepository(
    private val database: BannerDatabase,
    private val pageSize: Int
) {
    fun find(playerUuid: UUID): BannedUserEntry? = database.connection().use { conn ->
        conn.prepareStatement("SELECT * FROM banner_banned_users WHERE player_uuid=? LIMIT 1").use { ps ->
            ps.setString(1, playerUuid.toString())
            ps.executeQuery().use { rs ->
                if (rs.next()) readCurrentEntry(rs) else null
            }
        }
    }

    fun isBanned(playerUuid: UUID): Boolean = find(playerUuid) != null

    fun save(entry: BannedUserEntry) {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO banner_banned_users(
                    player_uuid,
                    player_name,
                    player_name_lower,
                    reason,
                    skin_value,
                    skin_signature,
                    banned_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, entry.playerUuid.toString())
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

    fun delete(playerUuid: UUID): Boolean = database.connection().use { conn ->
        conn.prepareStatement("DELETE FROM banner_banned_users WHERE player_uuid=?").use { ps ->
            ps.setString(1, playerUuid.toString())
            ps.executeUpdate() > 0
        }
    }

    fun page(page: Int): BannerPageResult<BannedUserEntry> {
        val totalItems = count()
        val totalPages = if (totalItems == 0) 1 else ((totalItems + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(1, totalPages)
        val offset = (safePage - 1) * pageSize

        return BannerPageResult(
            entries = list(offset),
            page = safePage,
            totalPages = totalPages,
            totalItems = totalItems
        )
    }

    private fun count(): Int = database.connection().use { conn ->
        conn.prepareStatement("SELECT COUNT(*) FROM banner_banned_users").use { ps ->
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun list(offset: Int): List<BannedUserEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT *
            FROM banner_banned_users
            ORDER BY banned_at DESC, player_name_lower ASC
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, pageSize)
            ps.setInt(2, offset)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(readCurrentEntry(rs))
                    }
                }
            }
        }
    }

    private fun readCurrentEntry(rs: ResultSet): BannedUserEntry {
        return BannedUserEntry(
            playerUuid = UUID.fromString(rs.getString("player_uuid")),
            playerName = rs.getString("player_name"),
            reason = rs.getString("reason"),
            profileSnapshot = rs.getString("skin_value")?.let { BannerProfileSnapshot(it, rs.getString("skin_signature")) },
            bannedAtEpochMillis = rs.getLong("banned_at")
        )
    }
}
