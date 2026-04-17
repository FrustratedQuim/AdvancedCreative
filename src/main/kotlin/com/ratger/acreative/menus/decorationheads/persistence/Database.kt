package com.ratger.acreative.menus.decorationheads.persistence

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class Database(dataFolder: File) {
    private val databaseFile = File(dataFolder, "decoration-heads.db")
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    fun init() {
        Class.forName("org.sqlite.JDBC")
        connection().use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS decoration_head_catalog (
                        stable_key TEXT PRIMARY KEY,
                        head_name TEXT NOT NULL,
                        head_name_ru TEXT,
                        category_id INTEGER NOT NULL,
                        texture_value TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS decoration_head_recent (
                        player_uuid TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        last_used_at INTEGER NOT NULL,
                        stable_key TEXT NOT NULL,
                        head_name TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        texture_value TEXT NOT NULL,
                        PRIMARY KEY(player_uuid, stable_key)
                    )
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS decoration_head_sync_state (
                        state_key TEXT PRIMARY KEY,
                        state_value TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS decoration_head_saved_pages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        source_mode TEXT NOT NULL,
                        category_key TEXT NOT NULL,
                        source_page INTEGER NOT NULL,
                        search_query TEXT,
                        search_query_key TEXT NOT NULL,
                        note TEXT,
                        map_color_key TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        UNIQUE(player_uuid, source_mode, category_key, source_page, search_query_key)
                    )
                    """.trimIndent()
                )
            }
            conn.createStatement().use { st ->
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_catalog_name_lower ON decoration_head_catalog(lower(head_name))")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_catalog_ru_lower ON decoration_head_catalog(lower(head_name_ru))")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_catalog_category_id ON decoration_head_catalog(category_id)")
                st.executeUpdate(
                    """
                    CREATE INDEX IF NOT EXISTS idx_catalog_recent_order
                    ON decoration_head_catalog(head_name COLLATE NOCASE)
                    """.trimIndent()
                )
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_recent_player_pos ON decoration_head_recent(player_uuid, position)")
st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_saved_pages_player ON decoration_head_saved_pages(player_uuid)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_saved_pages_player_category ON decoration_head_saved_pages(player_uuid, category_key)")
            }
            conn.commit()
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl)
}
