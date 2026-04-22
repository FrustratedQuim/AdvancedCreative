package com.ratger.acreative.menus.banner.persistence

import com.ratger.acreative.menus.banner.model.BannedPatternEntry
import com.ratger.acreative.menus.banner.model.BannerPageResult
import org.bukkit.inventory.ItemStack
import java.sql.ResultSet

class BannedPatternRepository(
    private val database: BannerDatabase,
    private val pageSize: Int
) {
    fun isBanned(patternSignature: String): Boolean = database.connection().use { conn ->
        conn.prepareStatement("SELECT 1 FROM banner_banned_patterns WHERE pattern_signature=? LIMIT 1").use { ps ->
            ps.setString(1, patternSignature)
            ps.executeQuery().use(ResultSet::next)
        }
    }

    fun save(patternSignature: String, bannerItem: ItemStack, bannedAtEpochMillis: Long) {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO banner_banned_patterns(pattern_signature, banner_data, banned_at)
                VALUES (?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, patternSignature)
                ps.setBytes(2, bannerItem.serializeAsBytes())
                ps.setLong(3, bannedAtEpochMillis)
                ps.executeUpdate()
            }
        }
    }

    fun delete(patternSignature: String): Boolean = database.connection().use { conn ->
        conn.prepareStatement("DELETE FROM banner_banned_patterns WHERE pattern_signature=?").use { ps ->
            ps.setString(1, patternSignature)
            ps.executeUpdate() > 0
        }
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
        conn.prepareStatement("SELECT COUNT(*) FROM banner_banned_patterns").use { ps ->
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun list(offset: Int): List<BannedPatternEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT *
            FROM banner_banned_patterns
            ORDER BY banned_at DESC, pattern_signature ASC
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, pageSize)
            ps.setInt(2, offset)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        val bytes = rs.getBytes("banner_data") ?: continue
                        val banner = runCatching { ItemStack.deserializeBytes(bytes) }.getOrNull() ?: continue
                        add(
                            BannedPatternEntry(
                                patternSignature = rs.getString("pattern_signature"),
                                bannerItem = banner,
                                bannedAtEpochMillis = rs.getLong("banned_at")
                            )
                        )
                    }
                }
            }
        }
    }
}
