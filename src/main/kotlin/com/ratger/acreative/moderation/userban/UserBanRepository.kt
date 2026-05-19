package com.ratger.acreative.moderation.userban

import com.ratger.acreative.core.CoreUserIdentityService
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import java.sql.ResultSet
import java.util.Locale
import java.util.UUID

interface UserBanStorage {
    fun find(playerId: Long): UserBanEntry?

    fun isBanned(playerId: Long): Boolean

    fun save(entry: UserBanEntry)

    fun delete(playerId: Long): Boolean

    fun page(page: Int): UserBanPageResult<UserBanEntry>
}

class UserBanRepository(
    private val database: AdvancedCreativeDatabase,
    private val identityService: CoreUserIdentityService,
    private val pageSize: Int,
    tableName: String
) : UserBanStorage {
    private val tableName: String = tableName.also(::validateTableName)

    fun find(playerUuid: UUID): UserBanEntry? {
        val playerId = identityService.resolveUserId(playerUuid) ?: return null
        return find(playerId)
    }

    override fun find(playerId: Long): UserBanEntry? = database.connection().use { conn ->
        conn.prepareStatement("SELECT * FROM $tableName WHERE player_id=? LIMIT 1").use { ps ->
            ps.setLong(1, playerId)
            ps.executeQuery().use { rs -> if (rs.next()) readCurrentEntry(rs) else null }
        }
    }

    fun isBanned(playerUuid: UUID): Boolean {
        val playerId = identityService.resolveUserId(playerUuid) ?: return false
        return isBanned(playerId)
    }

    override fun isBanned(playerId: Long): Boolean = find(playerId) != null

    override fun save(entry: UserBanEntry) {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO $tableName(
                    player_id,
                    player_name,
                    player_name_lower,
                    reason,
                    skin_value,
                    skin_signature,
                    blocked_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
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

    fun delete(playerUuid: UUID): Boolean {
        val playerId = identityService.resolveUserId(playerUuid) ?: return false
        return delete(playerId)
    }

    override fun delete(playerId: Long): Boolean = database.connection().use { conn ->
        conn.prepareStatement("DELETE FROM $tableName WHERE player_id=?").use { ps ->
            ps.setLong(1, playerId)
            ps.executeUpdate() > 0
        }
    }

    override fun page(page: Int): UserBanPageResult<UserBanEntry> {
        val totalItems = count()
        val totalPages = if (totalItems == 0) 1 else ((totalItems + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(1, totalPages)
        val offset = (safePage - 1) * pageSize

        return UserBanPageResult(
            entries = list(offset),
            page = safePage,
            totalPages = totalPages,
            totalItems = totalItems
        )
    }

    private fun count(): Int = database.connection().use { conn ->
        conn.prepareStatement("SELECT COUNT(*) FROM $tableName").use { ps ->
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun list(offset: Int): List<UserBanEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT *
            FROM $tableName
            ORDER BY blocked_at DESC, player_name_lower ASC
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
        reason = rs.getString("reason"),
        profileSnapshot = rs.getString("skin_value")?.let { UserProfileSnapshot(it, rs.getString("skin_signature")) },
        bannedAtEpochMillis = rs.getLong("blocked_at")
    )

    private fun validateTableName(tableName: String) {
        require(TABLE_NAME_PATTERN.matches(tableName)) { "Unsafe user ban table name: $tableName" }
    }

    private companion object {
        val TABLE_NAME_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
