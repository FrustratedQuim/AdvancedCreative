package com.ratger.acreative.menus.decorationheads.persistence

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class DecorationHeadsDatabase(dataFolder: File) {
    private val databaseFile = File(dataFolder, "decoration-heads.db")
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    fun init() {
        Class.forName("org.sqlite.JDBC")
        connection().use { conn ->
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS decoration_head_catalog (
                        stable_key TEXT PRIMARY KEY,
                        api_id INTEGER,
                        head_name TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        texture_value TEXT NOT NULL,
                        published_at TEXT,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_catalog_name_lower ON decoration_head_catalog(lower(head_name))")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_catalog_category_id ON decoration_head_catalog(category_id)")
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS decoration_head_recent (
                        player_uuid TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        last_used_at INTEGER NOT NULL,
                        stable_key TEXT NOT NULL,
                        api_id INTEGER,
                        head_name TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        texture_value TEXT NOT NULL,
                        PRIMARY KEY(player_uuid, stable_key)
                    )
                    """.trimIndent()
                )
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_recent_player_pos ON decoration_head_recent(player_uuid, position)")
            }
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl)
}
