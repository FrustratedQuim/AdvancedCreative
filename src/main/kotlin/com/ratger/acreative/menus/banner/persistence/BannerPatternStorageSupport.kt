package com.ratger.acreative.menus.banner.persistence

import org.bukkit.inventory.ItemStack
import java.sql.Connection

internal object BannerPatternStorageSupport {
    fun upsertPattern(conn: Connection, patternSignature: String, bannerItem: ItemStack) {
        conn.prepareStatement(
            """
            INSERT INTO banner_patterns(pattern_signature, banner_item_data)
            VALUES (?, ?)
            ON CONFLICT(pattern_signature) DO UPDATE SET
            banner_item_data=excluded.banner_item_data
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, patternSignature)
            ps.setBytes(2, bannerItem.serializeAsBytes())
            ps.executeUpdate()
        }
    }

    fun deletePatternIfUnused(conn: Connection, patternSignature: String) {
        conn.prepareStatement(
            """
            DELETE FROM banner_patterns
            WHERE pattern_signature=?
              AND NOT EXISTS (
                  SELECT 1
                  FROM banner_publications
                  WHERE pattern_signature=banner_patterns.pattern_signature
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM banner_blocked_patterns
                  WHERE pattern_signature=banner_patterns.pattern_signature
              )
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, patternSignature)
            ps.executeUpdate()
        }
    }
}
