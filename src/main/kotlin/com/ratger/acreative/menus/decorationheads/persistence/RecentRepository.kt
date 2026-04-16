package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.menus.decorationheads.model.Entry
import java.util.UUID

class RecentRepository(
    private val database: Database,
    private val limit: Int
) {
    fun push(playerId: UUID, entry: Entry) {
        val player = playerId.toString()
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM decoration_head_recent WHERE player_uuid=? AND stable_key=?").use { ps ->
                ps.setString(1, player)
                ps.setString(2, entry.stableKey)
                ps.executeUpdate()
            }
            conn.prepareStatement("UPDATE decoration_head_recent SET position=position+1 WHERE player_uuid=?").use { ps ->
                ps.setString(1, player)
                ps.executeUpdate()
            }
            conn.prepareStatement(
                """
                INSERT INTO decoration_head_recent (player_uuid, position, last_used_at, stable_key, head_name, category_id, texture_value)
                VALUES (?, 0, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, player)
                ps.setLong(2, System.currentTimeMillis())
                ps.setString(3, entry.stableKey)
                ps.setString(4, entry.name)
                ps.setInt(5, entry.categoryId)
                ps.setString(6, entry.textureValue)
                ps.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM decoration_head_recent WHERE player_uuid=? AND position>=?").use { ps ->
                ps.setString(1, player)
                ps.setInt(2, limit)
                ps.executeUpdate()
            }
            conn.commit()
        }
    }

    fun list(playerId: UUID): List<Entry> {
        val sql = "SELECT stable_key, head_name, category_id, texture_value FROM decoration_head_recent WHERE player_uuid=? ORDER BY position ASC"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerId.toString())
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                Entry(
                                    stableKey = rs.getString("stable_key"),
                                    name = rs.getString("head_name"),
                                    russianAlias = null,
                                    categoryId = rs.getInt("category_id"),
                                    textureValue = rs.getString("texture_value"),
                                    publishedAt = null
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
