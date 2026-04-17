package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.menus.decorationheads.model.SavedPageEntry
import com.ratger.acreative.menus.decorationheads.model.SavedPageSourceMode
import java.util.UUID

class SavedPagesRepository(
    private val database: Database
) {
    fun insert(
        playerUuid: UUID,
        sourceMode: SavedPageSourceMode,
        categoryKey: String,
        sourcePage: Int,
        searchQuery: String?,
        searchQueryKey: String,
        note: String?,
        mapColorKey: String?
    ): SavedPageEntry {
        val now = System.currentTimeMillis() / 1000L
        val sql = """
            INSERT INTO decoration_head_saved_pages
            (player_uuid, source_mode, category_key, source_page, search_query, search_query_key, note, map_color_key, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return database.connection().use { conn ->
            conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { ps ->
                ps.setString(1, playerUuid.toString())
                ps.setString(2, sourceMode.name)
                ps.setString(3, categoryKey)
                ps.setInt(4, sourcePage)
                ps.setString(5, searchQuery)
                ps.setString(6, searchQueryKey)
                ps.setString(7, note)
                ps.setString(8, mapColorKey)
                ps.setLong(9, now)
                ps.setLong(10, now)
                ps.executeUpdate()
                ps.generatedKeys.use { keys ->
                    check(keys.next()) { "Failed to insert saved page" }
                    val id = keys.getLong(1)
                    return SavedPageEntry(id, playerUuid, sourceMode, categoryKey, sourcePage, searchQuery, searchQueryKey, note, mapColorKey, now, now)
                }
            }
        }
    }

    fun deleteById(playerUuid: UUID, id: Long): Boolean {
        val sql = "DELETE FROM decoration_head_saved_pages WHERE player_uuid=? AND id=?"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerUuid.toString())
                ps.setLong(2, id)
                ps.executeUpdate() > 0
            }
        }
    }

    fun findById(playerUuid: UUID, id: Long): SavedPageEntry? {
        val sql = "SELECT * FROM decoration_head_saved_pages WHERE player_uuid=? AND id=?"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerUuid.toString())
                ps.setLong(2, id)
                ps.executeQuery().use { rs -> if (rs.next()) row(rs) else null }
            }
        }
    }

    fun findBySource(playerUuid: UUID, sourceMode: SavedPageSourceMode, categoryKey: String, sourcePage: Int, searchQueryKey: String): SavedPageEntry? {
        val sql = """
            SELECT * FROM decoration_head_saved_pages
            WHERE player_uuid=? AND source_mode=? AND category_key=? AND source_page=? AND search_query_key=?
            LIMIT 1
        """.trimIndent()
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerUuid.toString())
                ps.setString(2, sourceMode.name)
                ps.setString(3, categoryKey)
                ps.setInt(4, sourcePage)
                ps.setString(5, searchQueryKey)
                ps.executeQuery().use { rs -> if (rs.next()) row(rs) else null }
            }
        }
    }

    fun countByPlayer(playerUuid: UUID): Int {
        val sql = "SELECT COUNT(*) FROM decoration_head_saved_pages WHERE player_uuid=?"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerUuid.toString())
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }
    }

    fun listByPlayer(playerUuid: UUID): List<SavedPageEntry> = listByPlayerAndCategory(playerUuid, null)

    fun listByPlayerAndCategory(playerUuid: UUID, categoryKey: String?): List<SavedPageEntry> {
        val sql = if (categoryKey == null) {
            "SELECT * FROM decoration_head_saved_pages WHERE player_uuid=? ORDER BY id ASC"
        } else {
            "SELECT * FROM decoration_head_saved_pages WHERE player_uuid=? AND category_key=? ORDER BY id ASC"
        }
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerUuid.toString())
                if (categoryKey != null) ps.setString(2, categoryKey)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(row(rs))
                    }
                }
            }
        }
    }

    fun updateNote(playerUuid: UUID, id: Long, note: String?): Boolean = updateField(playerUuid, id, "note", note)

    fun updateSourcePage(playerUuid: UUID, id: Long, sourcePage: Int): Boolean {
        val sql = "UPDATE decoration_head_saved_pages SET source_page=?, updated_at=? WHERE player_uuid=? AND id=?"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setInt(1, sourcePage)
                ps.setLong(2, System.currentTimeMillis() / 1000L)
                ps.setString(3, playerUuid.toString())
                ps.setLong(4, id)
                ps.executeUpdate() > 0
            }
        }
    }

    fun updateMapColorKey(playerUuid: UUID, id: Long, mapColorKey: String?): Boolean = updateField(playerUuid, id, "map_color_key", mapColorKey)

    private fun updateField(playerUuid: UUID, id: Long, field: String, value: String?): Boolean {
        val sql = "UPDATE decoration_head_saved_pages SET $field=?, updated_at=? WHERE player_uuid=? AND id=?"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, value)
                ps.setLong(2, System.currentTimeMillis() / 1000L)
                ps.setString(3, playerUuid.toString())
                ps.setLong(4, id)
                ps.executeUpdate() > 0
            }
        }
    }

    private fun row(rs: java.sql.ResultSet): SavedPageEntry = SavedPageEntry(
        id = rs.getLong("id"),
        playerUuid = UUID.fromString(rs.getString("player_uuid")),
        sourceMode = SavedPageSourceMode.valueOf(rs.getString("source_mode")),
        categoryKey = rs.getString("category_key"),
        sourcePage = rs.getInt("source_page"),
        searchQuery = rs.getString("search_query"),
        searchQueryKey = rs.getString("search_query_key"),
        note = rs.getString("note"),
        mapColorKey = rs.getString("map_color_key"),
        createdAt = rs.getLong("created_at"),
        updatedAt = rs.getLong("updated_at")
    )
}
