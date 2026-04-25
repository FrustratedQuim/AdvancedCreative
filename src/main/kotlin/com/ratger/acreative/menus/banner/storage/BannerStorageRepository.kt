package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import org.bukkit.inventory.ItemStack
import java.util.UUID

class BannerStorageRepository(
    private val database: AdvancedCreativeDatabase
) {
    data class StoredBannerSlot(
        val slotIndex: Int,
        val item: ItemStack
    )

    fun load(playerId: UUID): List<StoredBannerSlot> {
        val sql = """
            SELECT slot_index, banner_item_data
            FROM banner_storage_items
            WHERE player_uuid=?
            ORDER BY slot_index ASC
        """.trimIndent()

        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, playerId.toString())
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
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM banner_storage_items WHERE player_uuid=?").use { ps ->
                ps.setString(1, playerId.toString())
                ps.executeUpdate()
            }
            conn.prepareStatement(
                """
                INSERT INTO banner_storage_items(player_uuid, slot_index, banner_item_data)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid, slot_index) DO UPDATE SET
                banner_item_data=excluded.banner_item_data
                """.trimIndent()
            ).use { ps ->
                slots.toSortedMap().forEach { (slot, item) ->
                    ps.setString(1, playerId.toString())
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
