package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.core.CoreUserIdentityService
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import org.bukkit.inventory.ItemStack
import java.util.UUID

class BannerStorageRepository(
    private val database: AdvancedCreativeDatabase,
    private val identityService: CoreUserIdentityService
) {
    data class StoredBannerSlot(
        val slotIndex: Int,
        val item: ItemStack
    )

    fun load(playerId: UUID): List<StoredBannerSlot> {
        val userId = identityService.resolveUserId(playerId) ?: return emptyList()
        val sql = """
            SELECT slot_index, banner_item_data
            FROM banner_storage_items
            WHERE player_id=?
            ORDER BY slot_index ASC
        """.trimIndent()

        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val bytes = rs.getBytes("banner_item_data") ?: continue
                            val item = runCatching { ItemStack.deserializeBytes(bytes) }.getOrNull() ?: continue
                            if (!item.type.name.endsWith("_BANNER") || item.amount <= 0) continue
                            add(StoredBannerSlot(rs.getInt("slot_index"), item))
                        }
                    }
                }
            }
        }
    }

    fun replaceAll(playerId: UUID, slots: Map<Int, ItemStack>) {
        val userId = requireNotNull(identityService.resolveUserId(playerId)) {
            "CoreApi user not found for playerUuid=$playerId"
        }
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM banner_storage_items WHERE player_id=?").use { ps ->
                ps.setLong(1, userId)
                ps.executeUpdate()
            }
            conn.prepareStatement(
                """
                INSERT INTO banner_storage_items(player_id, slot_index, banner_item_data)
                VALUES (?, ?, ?)
                ON CONFLICT(player_id, slot_index) DO UPDATE SET
                banner_item_data=excluded.banner_item_data
                """.trimIndent()
            ).use { ps ->
                slots.toSortedMap().forEach { (slot, item) ->
                    ps.setLong(1, userId)
                    ps.setInt(2, slot)
                    ps.setBytes(3, item.serializeAsBytes())
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }
}
