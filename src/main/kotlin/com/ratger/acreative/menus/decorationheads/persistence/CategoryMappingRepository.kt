package com.ratger.acreative.menus.decorationheads.persistence

import com.ratger.acreative.menus.decorationheads.category.ApiCategoryMapping
import com.ratger.acreative.persistence.AdvancedCreativeDatabase

class CategoryMappingRepository(
    private val database: AdvancedCreativeDatabase
) {
    fun findAll(): List<ApiCategoryMapping> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT category_id, api_name
            FROM head_api_categories
            ORDER BY api_name COLLATE NOCASE ASC
            """.trimIndent()
        ).use { ps ->
            ps.executeQuery().use { rs ->
                val out = mutableListOf<ApiCategoryMapping>()
                while (rs.next()) {
                    out += ApiCategoryMapping(
                        id = rs.getInt("category_id"),
                        name = rs.getString("api_name")
                    )
                }
                out
            }
        }
    }

    fun replaceAll(entries: Collection<ApiCategoryMapping>) {
        database.connection().use { conn ->
            conn.autoCommit = false
            conn.prepareStatement("DELETE FROM head_api_categories").use { it.executeUpdate() }
            if (entries.isNotEmpty()) {
                conn.prepareStatement(
                    """
                    INSERT INTO head_api_categories (category_id, api_name, updated_at)
                    VALUES (?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    val updatedAt = System.currentTimeMillis()
                    entries.forEach { entry ->
                        ps.setInt(1, entry.id)
                        ps.setString(2, entry.name)
                        ps.setLong(3, updatedAt)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
            conn.commit()
        }
    }
}
