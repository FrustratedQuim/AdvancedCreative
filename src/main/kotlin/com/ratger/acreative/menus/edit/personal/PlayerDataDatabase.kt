package com.ratger.acreative.menus.edit.personal

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class PlayerDataDatabase(dataFolder: File) {
    private val databaseFile = File(dataFolder, "player_data.db")
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    fun init() {
        Class.forName("org.sqlite.JDBC")
        connection().use { conn ->
            conn.autoCommit = false
            conn.createStatement().use { st ->
                st.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS edit_personal_items (
                        player_uuid TEXT NOT NULL,
                        last_used_at INTEGER NOT NULL,
                        content_hash TEXT NOT NULL,
                        item_data BLOB NOT NULL,
                        PRIMARY KEY(player_uuid, content_hash)
                    )
                    """.trimIndent()
                )
            }
            conn.createStatement().use { st ->
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edit_personal_items_player_last_used ON edit_personal_items(player_uuid, last_used_at DESC)")
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edit_personal_items_player_hash ON edit_personal_items(player_uuid, content_hash)")
            }
            conn.commit()
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl)
}
