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
            }
            migrateCatalogSchema(conn)
            migrateRecentSchema(conn)
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
            }
            conn.commit()
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl)

    private fun migrateCatalogSchema(conn: Connection) {
        val columns = tableColumns(conn, "decoration_head_catalog")
        val requiresMigration =
            "api_id" in columns ||
                "published_at" in columns ||
                "head_name_ru_alias" in columns ||
                "head_name_ru" !in columns
        if (!requiresMigration) return

        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE decoration_head_catalog_new (
                    stable_key TEXT PRIMARY KEY,
                    head_name TEXT NOT NULL,
                    head_name_ru TEXT,
                    category_id INTEGER NOT NULL,
                    texture_value TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )

            val russianNameExpr = when {
                "head_name_ru" in columns && "head_name_ru_alias" in columns -> "COALESCE(head_name_ru, head_name_ru_alias)"
                "head_name_ru" in columns -> "head_name_ru"
                "head_name_ru_alias" in columns -> "head_name_ru_alias"
                else -> "NULL"
            }
            val updatedAtExpr = if ("updated_at" in columns) "updated_at" else "0"

            st.executeUpdate(
                """
                INSERT INTO decoration_head_catalog_new (stable_key, head_name, head_name_ru, category_id, texture_value, updated_at)
                SELECT stable_key, head_name, $russianNameExpr, category_id, texture_value, $updatedAtExpr
                FROM decoration_head_catalog
                """.trimIndent()
            )
            st.executeUpdate("DROP TABLE decoration_head_catalog")
            st.executeUpdate("ALTER TABLE decoration_head_catalog_new RENAME TO decoration_head_catalog")
        }
    }

    private fun migrateRecentSchema(conn: Connection) {
        val columns = tableColumns(conn, "decoration_head_recent")
        val hasApiId = "api_id" in columns
        if (!hasApiId) return

        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                CREATE TABLE decoration_head_recent_new (
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
                INSERT INTO decoration_head_recent_new (player_uuid, position, last_used_at, stable_key, head_name, category_id, texture_value)
                SELECT player_uuid, position, last_used_at, stable_key, head_name, category_id, texture_value
                FROM decoration_head_recent
                """.trimIndent()
            )
            st.executeUpdate("DROP TABLE decoration_head_recent")
            st.executeUpdate("ALTER TABLE decoration_head_recent_new RENAME TO decoration_head_recent")
        }
    }

    private fun tableColumns(conn: Connection, tableName: String): Set<String> {
        conn.prepareStatement("PRAGMA table_info($tableName)").use { ps ->
            ps.executeQuery().use { rs ->
                val out = mutableSetOf<String>()
                while (rs.next()) {
                    out += rs.getString("name")
                }
                return out
            }
        }
    }
}
