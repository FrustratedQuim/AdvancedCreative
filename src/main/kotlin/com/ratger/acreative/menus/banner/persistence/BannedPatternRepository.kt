package com.ratger.acreative.menus.banner.persistence

import com.ratger.acreative.menus.banner.model.BannedPatternEntry
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import org.bukkit.inventory.ItemStack
import java.sql.ResultSet

class BannedPatternRepository(
    private val database: AdvancedCreativeDatabase,
    private val pageSize: Int
) {
    fun isBanned(patternSignature: String): Boolean = database.connection().use { conn ->
        conn.prepareStatement("SELECT 1 FROM banner_blocked_patterns WHERE pattern_signature=? LIMIT 1").use { ps ->
            ps.setString(1, patternSignature)
            ps.executeQuery().use(ResultSet::next)
        }
    }

    fun save(patternSignature: String, bannerItem: ItemStack, bannedAtEpochMillis: Long) {
        database.connection().use { conn ->
            conn.autoCommit = false
            BannerPatternStorageSupport.upsertPattern(conn, patternSignature, bannerItem)
            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO banner_blocked_patterns(pattern_signature, blocked_at)
                VALUES (?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, patternSignature)
                ps.setLong(2, bannedAtEpochMillis)
                ps.executeUpdate()
            }
            conn.commit()
        }
    }

    fun delete(patternSignature: String): Boolean = database.connection().use { conn ->
        conn.autoCommit = false
        val deleted = conn.prepareStatement("DELETE FROM banner_blocked_patterns WHERE pattern_signature=?").use { ps ->
            ps.setString(1, patternSignature)
            ps.executeUpdate() > 0
        }
        if (deleted) {
            BannerPatternStorageSupport.deletePatternIfUnused(conn, patternSignature)
        }
        conn.commit()
        deleted
    }

    fun page(page: Int): BannerPageResult<BannedPatternEntry> {
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
        conn.prepareStatement("SELECT COUNT(*) FROM banner_blocked_patterns").use { ps ->
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun list(offset: Int): List<BannedPatternEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT blocked.pattern_signature, blocked.blocked_at, pattern.banner_item_data
            FROM banner_blocked_patterns blocked
            JOIN banner_patterns pattern ON pattern.pattern_signature = blocked.pattern_signature
            ORDER BY blocked.blocked_at DESC, blocked.pattern_signature ASC
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, pageSize)
            ps.setInt(2, offset)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val bytes = rs.getBytes("banner_item_data") ?: continue
                        val banner = runCatching { ItemStack.deserializeBytes(bytes) }.getOrNull() ?: continue
                        add(
                            BannedPatternEntry(
                                patternSignature = rs.getString("pattern_signature"),
                                bannerItem = banner,
                                bannedAtEpochMillis = rs.getLong("blocked_at")
                            )
                        )
                    }
                }
            }
        }
    }
}
