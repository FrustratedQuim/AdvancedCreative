package com.ratger.acreative.menus.banner.persistence

import com.ratger.acreative.menus.banner.model.BannerCategory
import com.ratger.acreative.menus.banner.model.BannerGalleryState
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.menus.banner.model.BannerSort
import com.ratger.acreative.menus.banner.model.PublishedBannerEntry
import org.bukkit.inventory.ItemStack
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.Locale
import java.util.UUID

class PublishedBannerRepository(
    private val database: BannerDatabase,
    private val pageSize: Int
) {
    fun savePublishedBanner(
        authorUuid: UUID,
        authorName: String,
        title: String?,
        category: BannerCategory,
        bannerItem: ItemStack,
        patternSignature: String,
        publishedAtEpochMillis: Long
    ): Long {
        val normalizedTitle = title?.trim().orEmpty()
        val loweredTitle = normalizedTitle.lowercase(Locale.ROOT)
        val loweredAuthor = authorName.lowercase(Locale.ROOT)

        return database.connection().use { conn ->
            val generatedId = conn.prepareStatement(
                """
                INSERT INTO banner_published(
                    author_uuid,
                    author_name,
                    author_name_lower,
                    title,
                    title_lower,
                    category_key,
                    banner_data,
                    pattern_signature,
                    takes_count,
                    published_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
                """.trimIndent(),
                PreparedStatement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, authorUuid.toString())
                ps.setString(2, authorName)
                ps.setString(3, loweredAuthor)
                ps.setString(4, normalizedTitle.ifBlank { null })
                ps.setString(5, loweredTitle)
                ps.setString(6, category.key)
                ps.setBytes(7, bannerItem.serializeAsBytes())
                ps.setString(8, patternSignature)
                ps.setLong(9, publishedAtEpochMillis)
                ps.executeUpdate()
                ps.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else 0L
                }
            }
            generatedId
        }
    }

    fun page(state: BannerGalleryState): BannerPageResult<PublishedBannerEntry> {
        val totalItems = count(state)
        val totalPages = if (totalItems == 0) 1 else ((totalItems + pageSize - 1) / pageSize)
        val page = state.page.coerceIn(1, totalPages)
        val offset = (page - 1) * pageSize

        return BannerPageResult(
            entries = list(state, offset),
            page = page,
            totalPages = totalPages,
            totalItems = totalItems
        )
    }

    fun count(state: BannerGalleryState): Int {
        val (sql, binder) = buildQuery(
            state = state,
            select = "SELECT COUNT(*) FROM banner_published",
            withOrder = false
        )

        return database.connection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                binder(ps)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }
    }

    fun countByAuthor(authorUuid: UUID): Int = database.connection().use { conn ->
        conn.prepareStatement("SELECT COUNT(*) FROM banner_published WHERE author_uuid=?").use { ps ->
            ps.setString(1, authorUuid.toString())
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    fun countByAuthorName(authorName: String): Int = database.connection().use { conn ->
        conn.prepareStatement("SELECT COUNT(*) FROM banner_published WHERE author_name_lower=?").use { ps ->
            ps.setString(1, authorName.lowercase(Locale.ROOT))
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    fun hasPublicationHistory(authorUuid: UUID, patternSignature: String, category: BannerCategory): Boolean =
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                SELECT 1
                FROM banner_published
                WHERE author_uuid=? AND pattern_signature=? AND category_key=?
                LIMIT 1
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, authorUuid.toString())
                ps.setString(2, patternSignature)
                ps.setString(3, category.key)
                ps.executeQuery().use(ResultSet::next)
            }
        }

    fun existsPublishedToday(patternSignature: String, category: BannerCategory, dayStartEpochMillis: Long): Boolean =
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                SELECT 1
                FROM banner_published
                WHERE pattern_signature=? AND category_key=? AND published_at>=?
                LIMIT 1
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, patternSignature)
                ps.setString(2, category.key)
                ps.setLong(3, dayStartEpochMillis)
                ps.executeQuery().use(ResultSet::next)
            }
        }

    fun deleteById(id: Long): Boolean = database.connection().use { conn ->
        conn.prepareStatement("DELETE FROM banner_published WHERE id=?").use { ps ->
            ps.setLong(1, id)
            ps.executeUpdate() > 0
        }
    }

    fun deleteByPatternSignature(patternSignature: String): Int = database.connection().use { conn ->
        conn.prepareStatement("DELETE FROM banner_published WHERE pattern_signature=?").use { ps ->
            ps.setString(1, patternSignature)
            ps.executeUpdate()
        }
    }

    fun incrementTakes(id: Long) {
        database.connection().use { conn ->
            conn.prepareStatement("UPDATE banner_published SET takes_count=takes_count+1 WHERE id=?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    fun listAuthorNames(): List<String> = database.connection().use { conn ->
        conn.prepareStatement(
            """
            SELECT DISTINCT author_name, author_name_lower
            FROM banner_published
            ORDER BY author_name_lower ASC, author_name ASC
            """.trimIndent()
        ).use { ps ->
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(rs.getString("author_name"))
                    }
                }
            }
        }
    }

    private fun list(state: BannerGalleryState, offset: Int): List<PublishedBannerEntry> {
        val (sql, binder) = buildQuery(
            state = state,
            select = "SELECT * FROM banner_published",
            withOrder = true
        )

        return database.connection().use { conn ->
            conn.prepareStatement("$sql LIMIT ? OFFSET ?").use { ps ->
                val lastIndex = binder(ps)
                ps.setInt(lastIndex, pageSize)
                ps.setInt(lastIndex + 1, offset)
                ps.executeQuery().use(::readEntries)
            }
        }
    }

    private fun buildQuery(
        state: BannerGalleryState,
        select: String,
        withOrder: Boolean
    ): Pair<String, (PreparedStatement) -> Int> {
        val filters = buildList {
            if (state.category != BannerCategory.ALL) {
                add("category_key=?")
            }
            if (state.authorFilterUuid != null) {
                add("author_uuid=?")
            }
            if (!state.authorFilterName.isNullOrBlank()) {
                add("author_name_lower=?")
            }
            if (!state.searchQuery.isNullOrBlank()) {
                add("title_lower LIKE ?")
            }
        }

        val where = if (filters.isEmpty()) "" else " WHERE ${filters.joinToString(" AND ")}"
        val order = if (!withOrder) {
            ""
        } else {
            when (state.sort) {
                BannerSort.POPULAR -> " ORDER BY takes_count DESC, published_at DESC, id DESC"
                BannerSort.NEW -> " ORDER BY published_at DESC, id DESC"
                BannerSort.OLD -> " ORDER BY published_at ASC, id ASC"
            }
        }

        val sql = select + where + order
        val binder: (PreparedStatement) -> Int = { ps ->
            var index = 1
            if (state.category != BannerCategory.ALL) {
                ps.setString(index++, state.category.key)
            }
            if (state.authorFilterUuid != null) {
                ps.setString(index++, state.authorFilterUuid.toString())
            }
            if (!state.authorFilterName.isNullOrBlank()) {
                ps.setString(index++, state.authorFilterName.lowercase(Locale.ROOT))
            }
            if (!state.searchQuery.isNullOrBlank()) {
                ps.setString(index++, "%${state.searchQuery.lowercase(Locale.ROOT)}%")
            }
            index
        }

        return sql to binder
    }

    private fun readEntries(rs: ResultSet): List<PublishedBannerEntry> {
        return buildList {
            while (rs.next()) {
                val bytes = rs.getBytes("banner_data") ?: continue
                val banner = runCatching { ItemStack.deserializeBytes(bytes) }.getOrNull() ?: continue
                add(
                    PublishedBannerEntry(
                        id = rs.getLong("id"),
                        authorUuid = UUID.fromString(rs.getString("author_uuid")),
                        authorName = rs.getString("author_name"),
                        title = rs.getString("title"),
                        category = BannerCategory.fromKey(rs.getString("category_key")),
                        bannerItem = banner,
                        patternSignature = rs.getString("pattern_signature"),
                        takes = rs.getInt("takes_count"),
                        publishedAtEpochMillis = rs.getLong("published_at")
                    )
                )
            }
        }
    }
}
