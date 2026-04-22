package com.ratger.acreative.menus.banner.persistence

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class BannerDatabase(dataFolder: File) {
    private val databaseFile = File(dataFolder, "banner.db")
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    fun init() {
        Class.forName("org.sqlite.JDBC")
        connection().use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS banner_published (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        author_uuid TEXT NOT NULL,
                        author_name TEXT NOT NULL,
                        author_name_lower TEXT NOT NULL,
                        title TEXT,
                        title_lower TEXT NOT NULL,
                        category_key TEXT NOT NULL,
                        banner_data BLOB NOT NULL,
                        pattern_signature TEXT NOT NULL,
                        takes_count INTEGER NOT NULL,
                        published_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS banner_publication_history (
                        author_uuid TEXT NOT NULL,
                        pattern_signature TEXT NOT NULL,
                        category_key TEXT NOT NULL,
                        published_at INTEGER NOT NULL,
                        PRIMARY KEY(author_uuid, pattern_signature, category_key)
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS banner_banned_patterns (
                        pattern_signature TEXT PRIMARY KEY,
                        banner_data BLOB NOT NULL,
                        banned_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS banner_banned_users (
                        player_uuid TEXT PRIMARY KEY,
                        player_name TEXT NOT NULL,
                        player_name_lower TEXT NOT NULL,
                        reason TEXT,
                        skin_value TEXT,
                        skin_signature TEXT,
                        banned_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
            conn.createStatement().use { st ->
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_author_lower ON banner_published(author_name_lower)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_author_uuid ON banner_published(author_uuid)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_category ON banner_published(category_key)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_title_lower ON banner_published(title_lower)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_pattern_category ON banner_published(pattern_signature, category_key)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_date ON banner_published(published_at)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_published_takes ON banner_published(takes_count DESC, published_at DESC)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_history_author ON banner_publication_history(author_uuid)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_banned_users_name_lower ON banner_banned_users(player_name_lower)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_banned_users_date ON banner_banned_users(banned_at)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_banned_patterns_date ON banner_banned_patterns(banned_at)")
            }
            conn.commit()
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl)
}
