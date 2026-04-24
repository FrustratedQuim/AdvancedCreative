package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.persistence.AdvancedCreativeDatabase
import java.sql.ResultSet

class CatalogRepository(
    private val database: AdvancedCreativeDatabase
) {
    fun upsert(entries: Collection<Entry>) {
        if (entries.isEmpty()) return
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement(
                """
                INSERT INTO head_catalog_entries (stable_key, display_name, display_name_ru, source_category_id, texture_value, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(stable_key) DO UPDATE SET
                display_name=excluded.display_name,
                display_name_ru=COALESCE(excluded.display_name_ru, head_catalog_entries.display_name_ru),
                source_category_id=excluded.source_category_id,
                texture_value=excluded.texture_value,
                updated_at=excluded.updated_at
                """.trimIndent()
            ).use { ps ->
                entries.forEach { e ->
                    ps.setString(1, e.stableKey)
                    ps.setString(2, e.name)
                    ps.setString(3, e.russianAlias)
                    ps.setInt(4, e.categoryId)
                    ps.setString(5, e.textureValue)
                    ps.setLong(6, System.currentTimeMillis())
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }

    fun isCatalogEmpty(): Boolean = database.connection().use { conn ->
        conn.prepareStatement("SELECT 1 FROM head_catalog_entries LIMIT 1").use { ps ->
            ps.executeQuery().use { rs -> !rs.next() }
        }
    }

    fun countByCategoryIds(categoryIds: Set<Int>): Int {
        if (categoryIds.isEmpty()) return 0
        val placeholders = categoryIds.joinToString(",") { "?" }
        return database.connection().use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM head_catalog_entries WHERE source_category_id IN ($placeholders)").use { ps ->
                categoryIds.bind(ps)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }
    }

    fun findPageByCategoryIds(categoryIds: Set<Int>, limit: Int, offset: Int): List<Entry> {
        if (categoryIds.isEmpty()) return emptyList()
        val placeholders = categoryIds.joinToString(",") { "?" }
        return database.connection().use { conn ->
            conn.prepareStatement(
                """
                SELECT * FROM head_catalog_entries
                WHERE source_category_id IN ($placeholders)
                ORDER BY display_name COLLATE NOCASE ASC
                LIMIT ? OFFSET ?
                """.trimIndent()
            ).use { ps ->
                var index = categoryIds.bind(ps)
                ps.setInt(index++, limit)
                ps.setInt(index, offset)
                ps.executeQuery().use(::readEntries)
            }
        }
    }

    fun countRecentPublished(): Int = database.connection().use { conn ->
        conn.prepareStatement("SELECT COUNT(*) FROM head_catalog_entries").use { ps ->
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    fun replaceAll(entries: Collection<Entry>) {
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM head_recent_entries").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM head_catalog_entries").use { it.executeUpdate() }
            if (entries.isNotEmpty()) {
                conn.prepareStatement(
                    """
                    INSERT INTO head_catalog_entries (stable_key, display_name, display_name_ru, source_category_id, texture_value, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    entries.forEach { e ->
                        ps.setString(1, e.stableKey)
                        ps.setString(2, e.name)
                        ps.setString(3, e.russianAlias)
                        ps.setInt(4, e.categoryId)
                        ps.setString(5, e.textureValue)
                        ps.setLong(6, System.currentTimeMillis())
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
            conn.commit()
        }
    }

    fun findRecentPublishedPage(limit: Int, offset: Int): List<Entry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT * FROM head_catalog_entries
            ORDER BY display_name COLLATE NOCASE ASC
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, limit)
            ps.setInt(2, offset)
            ps.executeQuery().use(::readEntries)
        }
    }

    fun countBySearch(query: String, searchByRussianAlias: Boolean): Int = database.connection().use { conn ->
        val column = if (searchByRussianAlias) "display_name_ru" else "display_name"
        conn.prepareStatement("SELECT COUNT(*) FROM head_catalog_entries WHERE lower(COALESCE($column, '')) LIKE ?").use { ps ->
            ps.setString(1, "%${query.lowercase()}%")
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    fun findSearchPage(query: String, limit: Int, offset: Int, searchByRussianAlias: Boolean): List<Entry> = database.connection().use { conn ->
        val column = if (searchByRussianAlias) "display_name_ru" else "display_name"
        conn.prepareStatement(
            """
            SELECT * FROM head_catalog_entries
            WHERE lower(COALESCE($column, '')) LIKE ?
            ORDER BY display_name COLLATE NOCASE ASC
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, "%${query.lowercase()}%")
            ps.setInt(2, limit)
            ps.setInt(3, offset)
            ps.executeQuery().use(::readEntries)
        }
    }

    private fun Set<Int>.bind(ps: java.sql.PreparedStatement): Int {
        var index = 1
        forEach { value ->
            ps.setInt(index++, value)
        }
        return index
    }

    private fun readEntries(rs: ResultSet): List<Entry> {
        val out = mutableListOf<Entry>()
        while (rs.next()) {
            out += Entry(
                stableKey = rs.getString("stable_key"),
                name = rs.getString("display_name"),
                russianAlias = rs.getString("display_name_ru"),
                categoryId = rs.getInt("source_category_id"),
                textureValue = rs.getString("texture_value")
            )
        }
        return out
    }
}
