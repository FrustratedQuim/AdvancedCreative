package com.ratger.acreative.persistence

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class AdvancedCreativeDatabase(
    dataFolder: File
) {
    private val databaseFile = File(dataFolder, DATABASE_FILE_NAME)
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    fun init() {
        databaseFile.parentFile?.mkdirs()
        Class.forName("org.sqlite.JDBC")
        connection().use { conn ->
            configureDatabase(conn)
            conn.autoCommit = false
            createTables(conn)
            createIndexes(conn)
            validateForeignKeys(conn)
            conn.commit()
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl).also(::configureConnection)

    private fun configureConnection(conn: Connection) {
        conn.createStatement().use { st ->
            st.execute("PRAGMA foreign_keys = ON")
            st.execute("PRAGMA busy_timeout = $BUSY_TIMEOUT_MILLIS")
        }
    }

    private fun configureDatabase(conn: Connection) {
        conn.createStatement().use { st ->
            st.execute("PRAGMA journal_mode = WAL")
            st.execute("PRAGMA synchronous = NORMAL")
        }
    }

    private fun createTables(conn: Connection) {
        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS edit_personal_items (
                    player_uuid TEXT NOT NULL,
                    content_hash TEXT NOT NULL,
                    item_data BLOB NOT NULL,
                    last_used_at INTEGER NOT NULL,
                    PRIMARY KEY(player_uuid, content_hash)
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS banner_patterns (
                    pattern_signature TEXT PRIMARY KEY,
                    banner_item_data BLOB NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS banner_publications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    author_uuid TEXT NOT NULL,
                    author_name TEXT NOT NULL,
                    author_name_lower TEXT NOT NULL,
                    title TEXT,
                    title_lower TEXT NOT NULL,
                    category_key TEXT NOT NULL,
                    pattern_signature TEXT NOT NULL,
                    takes_count INTEGER NOT NULL,
                    published_at INTEGER NOT NULL,
                    FOREIGN KEY(pattern_signature) REFERENCES banner_patterns(pattern_signature) ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS banner_blocked_patterns (
                    pattern_signature TEXT PRIMARY KEY,
                    blocked_at INTEGER NOT NULL,
                    FOREIGN KEY(pattern_signature) REFERENCES banner_patterns(pattern_signature) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS banner_blocked_players (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    player_name_lower TEXT NOT NULL,
                    reason TEXT,
                    skin_value TEXT,
                    skin_signature TEXT,
                    blocked_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS head_catalog_entries (
                    stable_key TEXT PRIMARY KEY,
                    display_name TEXT NOT NULL,
                    display_name_ru TEXT,
                    source_category_id INTEGER NOT NULL,
                    texture_value TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS head_recent_entries (
                    player_uuid TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    stable_key TEXT NOT NULL,
                    last_used_at INTEGER NOT NULL,
                    PRIMARY KEY(player_uuid, position),
                    UNIQUE(player_uuid, stable_key),
                    FOREIGN KEY(stable_key) REFERENCES head_catalog_entries(stable_key) ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS head_saved_pages (
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
    }

    private fun createIndexes(conn: Connection) {
        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_edit_personal_items_player_last_used
                ON edit_personal_items(player_uuid, last_used_at DESC, content_hash ASC)
                """.trimIndent()
            )
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_publications_author_uuid ON banner_publications(author_uuid)")
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_publications_author_name_lower ON banner_publications(author_name_lower)")
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_banner_publications_category_key ON banner_publications(category_key)")
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_banner_publications_author_pattern_category
                ON banner_publications(author_uuid, pattern_signature, category_key)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_banner_publications_pattern_category_published_at
                ON banner_publications(pattern_signature, category_key, published_at)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_banner_publications_published_at
                ON banner_publications(published_at DESC, id DESC)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_banner_publications_takes
                ON banner_publications(takes_count DESC, published_at DESC, id DESC)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_banner_blocked_patterns_blocked_at
                ON banner_blocked_patterns(blocked_at DESC, pattern_signature ASC)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_banner_blocked_players_blocked_at
                ON banner_blocked_players(blocked_at DESC, player_name_lower ASC)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_head_catalog_entries_display_name
                ON head_catalog_entries(display_name COLLATE NOCASE ASC)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_head_catalog_entries_category_display_name
                ON head_catalog_entries(source_category_id, display_name COLLATE NOCASE ASC)
                """.trimIndent()
            )
            st.executeUpdate(
                """
                CREATE INDEX IF NOT EXISTS idx_head_saved_pages_player_id
                ON head_saved_pages(player_uuid, id ASC)
                """.trimIndent()
            )
        }
    }

    private fun validateForeignKeys(conn: Connection) {
        conn.createStatement().use { st ->
            st.executeQuery("PRAGMA main.foreign_key_check").use { rs ->
                require(!rs.next()) {
                    val tableName = rs.getString(1)
                    val rowId = rs.getString(2)
                    val parentTable = rs.getString(3)
                    "Foreign key violation detected in table=$tableName rowId=$rowId parentTable=$parentTable"
                }
            }
        }
    }

    private companion object {
        const val DATABASE_FILE_NAME = "global_data.db"
        const val BUSY_TIMEOUT_MILLIS = 5_000
    }
}
