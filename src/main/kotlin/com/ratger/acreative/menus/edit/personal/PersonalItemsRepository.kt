package com.ratger.acreative.menus.edit.personal

import com.ratger.acreative.core.CoreUserIdentityService
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import org.bukkit.inventory.ItemStack
import java.util.UUID

class PersonalItemsRepository(
    private val database: AdvancedCreativeDatabase,
    private val identityService: CoreUserIdentityService,
    private val limit: Int
) {
    data class StoredPersonalItem(
        val contentHash: String,
        val item: ItemStack,
        val lastUsedAtEpochSeconds: Long
    )

    fun list(playerId: UUID): List<StoredPersonalItem> {
        val userId = identityService.resolveUserId(playerId) ?: return emptyList()
        val sql = """
            SELECT content_hash, item_data, last_used_at
            FROM edit_personal_items
            WHERE player_id=?
            ORDER BY last_used_at DESC, content_hash ASC
        """.trimIndent()

        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val bytes = rs.getBytes("item_data") ?: continue
                            val stack = runCatching { ItemStack.deserializeBytes(bytes) }.getOrNull() ?: continue
                            if (stack.type.isAir || stack.amount <= 0) continue
                            add(
                                StoredPersonalItem(
                                    contentHash = rs.getString("content_hash"),
                                    item = stack,
                                    lastUsedAtEpochSeconds = rs.getLong("last_used_at")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun replaceAll(playerId: UUID, entries: List<StoredPersonalItem>) {
        val userId = requireNotNull(identityService.resolveUserId(playerId)) {
            "CoreApi user not found for playerUuid=$playerId"
        }
        val trimmed = entries.take(limit)
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM edit_personal_items WHERE player_id=?").use { ps ->
                ps.setLong(1, userId)
                ps.executeUpdate()
            }
            conn.prepareStatement(
                """
                INSERT INTO edit_personal_items (player_id, last_used_at, content_hash, item_data)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                trimmed.forEach { entry ->
                    ps.setLong(1, userId)
                    ps.setLong(2, entry.lastUsedAtEpochSeconds)
                    ps.setString(3, entry.contentHash)
                    ps.setBytes(4, entry.item.serializeAsBytes())
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }
}
