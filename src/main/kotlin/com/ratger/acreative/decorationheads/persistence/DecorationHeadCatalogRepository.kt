package com.ratger.acreative.decorationheads.persistence

import com.ratger.acreative.decorationheads.model.DecorationHeadEntry
import java.sql.ResultSet
import java.time.LocalDate

class DecorationHeadCatalogRepository(
    private val database: DecorationHeadsDatabase
) {
    fun upsert(entries: Collection<DecorationHeadEntry>) {
        if (entries.isEmpty()) return
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement(
                """
                INSERT INTO decoration_head_catalog (stable_key, api_id, head_name, category_id, texture_value, published_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(stable_key) DO UPDATE SET
                api_id=excluded.api_id,
                head_name=excluded.head_name,
                category_id=excluded.category_id,
                texture_value=excluded.texture_value,
                published_at=excluded.published_at,
                updated_at=excluded.updated_at
                """.trimIndent()
            ).use { ps ->
                entries.forEach { e ->
                    ps.setString(1, e.stableKey)
                    ps.setObject(2, e.apiId)
                    ps.setString(3, e.name)
                    ps.setInt(4, e.categoryId)
                    ps.setString(5, e.textureValue)
                    ps.setString(6, e.publishedAt?.toString())
                    ps.setLong(7, System.currentTimeMillis())
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        }
    }

    fun findByCategoryIds(categoryIds: Set<Int>): List<DecorationHeadEntry> {
        if (categoryIds.isEmpty()) return emptyList()
        val placeholders = categoryIds.joinToString(",") { "?" }
        val sql = "SELECT * FROM decoration_head_catalog WHERE category_id IN ($placeholders)"
        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                categoryIds.forEachIndexed { index, value -> ps.setInt(index + 1, value) }
                ps.executeQuery().use(::readEntries)
            }
        }
    }

    fun findRecentPublished(limit: Int): List<DecorationHeadEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT * FROM decoration_head_catalog
            ORDER BY (published_at IS NULL), published_at DESC, (api_id IS NULL), api_id DESC, head_name ASC
            LIMIT ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, limit)
            ps.executeQuery().use(::readEntries)
        }
    }

    fun findBySearch(query: String, limit: Int): List<DecorationHeadEntry> = database.connection().use { conn ->
        conn.prepareStatement(
            "SELECT * FROM decoration_head_catalog WHERE lower(head_name) LIKE ? ORDER BY head_name ASC LIMIT ?"
        ).use { ps ->
            ps.setString(1, "%${query.lowercase()}%")
            ps.setInt(2, limit)
            ps.executeQuery().use(::readEntries)
        }
    }

    private fun readEntries(rs: ResultSet): List<DecorationHeadEntry> {
        val out = mutableListOf<DecorationHeadEntry>()
        while (rs.next()) {
            out += DecorationHeadEntry(
                apiId = rs.getInt("api_id").takeUnless { rs.wasNull() },
                stableKey = rs.getString("stable_key"),
                name = rs.getString("head_name"),
                categoryId = rs.getInt("category_id"),
                textureValue = rs.getString("texture_value"),
                publishedAt = rs.getString("published_at")?.let { LocalDate.parse(it) }
            )
        }
        return out
    }
}
