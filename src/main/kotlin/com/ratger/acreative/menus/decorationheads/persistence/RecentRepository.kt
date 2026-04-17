package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.menus.decorationheads.model.Entry
import java.util.UUID

class RecentRepository(
    private val database: Database,
    private val limit: Int
) {
    data class StoredRecentEntry(
        val entry: Entry,
        val savedAtEpochSeconds: Long
    )

    fun list(playerId: UUID): List<Entry> = listStored(playerId).map { it.entry }

    fun listStored(playerId: UUID): List<StoredRecentEntry> {
        val sql = """
            SELECT stable_key, head_name, category_id, texture_value, last_used_at
            FROM decoration_head_recent
            WHERE player_uuid=?
            ORDER BY position ASC
        """.trimIndent()

        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerId.toString())
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                StoredRecentEntry(
                                    entry = Entry(
                                        stableKey = rs.getString("stable_key"),
                                        name = rs.getString("head_name"),
                                        russianAlias = null,
                                        categoryId = rs.getInt("category_id"),
                                        textureValue = rs.getString("texture_value")
                                    ),
                                    savedAtEpochSeconds = rs.getLong("last_used_at")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun replaceAll(playerId: UUID, entries: List<StoredRecentEntry>) {
        val player = playerId.toString()
        val trimmed = entries.take(limit)
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM decoration_head_recent WHERE player_uuid=?").use { ps ->
                ps.setString(1, player)
                ps.executeUpdate()
            }

            conn.prepareStatement(
                """
                INSERT INTO decoration_head_recent (player_uuid, position, last_used_at, stable_key, head_name, category_id, texture_value)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                trimmed.forEachIndexed { index, stored ->
                    ps.setString(1, player)
                    ps.setInt(2, index)
                    ps.setLong(3, stored.savedAtEpochSeconds)
                    ps.setString(4, stored.entry.stableKey)
                    ps.setString(5, stored.entry.name)
                    ps.setInt(6, stored.entry.categoryId)
                    ps.setString(7, stored.entry.textureValue)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }

    fun migrateTimestampFormatToEpochSeconds() {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                UPDATE decoration_head_recent
                SET last_used_at = CAST(last_used_at / 1000 AS INTEGER)
                WHERE last_used_at > 9999999999
                """.trimIndent()
            ).use { ps ->
                ps.executeUpdate()
            }
        }
    }
}
