package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.core.CoreUserIdentityService
import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import java.util.UUID

class RecentRepository(
    private val database: AdvancedCreativeDatabase,
    private val identityService: CoreUserIdentityService,
    private val limit: Int
) {
    data class StoredRecentEntry(
        val entry: Entry,
        val savedAtEpochSeconds: Long
    )

    fun list(playerId: UUID): List<Entry> = listStored(playerId).map { it.entry }

    fun listStored(playerId: UUID): List<StoredRecentEntry> {
        val userId = identityService.resolveUserId(playerId) ?: return emptyList()
        val sql = """
            SELECT
                recent.stable_key,
                catalog.display_name,
                catalog.source_category_id,
                catalog.texture_value,
                recent.last_used_at
            FROM head_recent_entries recent
            JOIN head_catalog_entries catalog ON catalog.stable_key = recent.stable_key
            WHERE recent.player_id=?
            ORDER BY recent.position ASC
        """.trimIndent()

        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                StoredRecentEntry(
                                    entry = Entry(
                                        stableKey = rs.getString("stable_key"),
                                        name = rs.getString("display_name"),
                                        russianAlias = null,
                                        categoryId = rs.getInt("source_category_id"),
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
        val userId = requireNotNull(identityService.resolveUserId(playerId)) {
            "CoreApi user not found for playerUuid=$playerId"
        }
        val trimmed = entries.take(limit)
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM head_recent_entries WHERE player_id=?").use { ps ->
                ps.setLong(1, userId)
                ps.executeUpdate()
            }

            conn.prepareStatement(
                """
                INSERT INTO head_recent_entries (player_id, position, stable_key, last_used_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                trimmed.forEachIndexed { index, stored ->
                    ps.setLong(1, userId)
                    ps.setInt(2, index)
                    ps.setString(3, stored.entry.stableKey)
                    ps.setLong(4, stored.savedAtEpochSeconds)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }

}
