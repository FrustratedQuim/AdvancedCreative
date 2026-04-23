package com.ratger.acreative.persistence

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

class AdvancedCreativeDatabase(
    dataFolder: File
) {
    private val databaseFile = File(dataFolder, DATABASE_FILE_NAME)
    private val legacyBannerDatabaseFile = File(dataFolder, LEGACY_BANNER_DATABASE_FILE_NAME)
    private val legacyHeadDatabaseFile = File(dataFolder, LEGACY_HEAD_DATABASE_FILE_NAME)
    private val legacyPlayerDatabaseFile = File(dataFolder, LEGACY_PLAYER_DATABASE_FILE_NAME)
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    fun init() {
        databaseFile.parentFile?.mkdirs()
        Class.forName("org.sqlite.JDBC")
        connection().use { conn ->
            configureDatabase(conn)
            conn.autoCommit = false
            createTables(conn)
            createIndexes(conn)
            applySchemaVersion(conn)
            migrateLegacyDatabasesIfNeeded(conn)
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
                CREATE TABLE IF NOT EXISTS storage_meta (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """.trimIndent()
            )
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

    private fun applySchemaVersion(conn: Connection) {
        val currentVersion = conn.createStatement().use { st ->
            st.executeQuery("PRAGMA user_version").use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
        require(currentVersion <= SCHEMA_VERSION) {
            "Unsupported database schema version $currentVersion, expected <= $SCHEMA_VERSION"
        }
        if (currentVersion < SCHEMA_VERSION) {
            conn.createStatement().use { st ->
                st.executeUpdate("PRAGMA user_version = $SCHEMA_VERSION")
            }
        }
    }

    private fun migrateLegacyDatabasesIfNeeded(conn: Connection) {
        if (hasMetaValue(conn, LEGACY_MIGRATION_META_KEY)) {
            return
        }

        if (legacyBannerDatabaseFile.exists()) {
            attachDatabase(conn, legacyBannerDatabaseFile, LEGACY_BANNER_SCHEMA)
        }
        if (legacyHeadDatabaseFile.exists()) {
            attachDatabase(conn, legacyHeadDatabaseFile, LEGACY_HEAD_SCHEMA)
        }
        if (legacyPlayerDatabaseFile.exists()) {
            attachDatabase(conn, legacyPlayerDatabaseFile, LEGACY_PLAYER_SCHEMA)
        }

        var legacyStructureFound = false
        if (legacyBannerDatabaseFile.exists()) {
            legacyStructureFound = migrateLegacyBannerDatabase(conn) || legacyStructureFound
        }
        if (legacyHeadDatabaseFile.exists()) {
            legacyStructureFound = migrateLegacyHeadDatabase(conn) || legacyStructureFound
        }
        if (legacyPlayerDatabaseFile.exists()) {
            legacyStructureFound = migrateLegacyPlayerDatabase(conn) || legacyStructureFound
        }

        if (legacyStructureFound) {
            upsertMetaValue(conn, LEGACY_MIGRATION_META_KEY, Instant.now().epochSecond.toString())
        }
    }

    private fun migrateLegacyBannerDatabase(conn: Connection): Boolean {
        val publishedExists = tableExists(conn, LEGACY_BANNER_SCHEMA, "banner_published")
        val blockedPatternsExists = tableExists(conn, LEGACY_BANNER_SCHEMA, "banner_banned_patterns")
        val blockedPlayersExists = tableExists(conn, LEGACY_BANNER_SCHEMA, "banner_banned_users")
        if (!publishedExists && !blockedPatternsExists && !blockedPlayersExists) {
            return false
        }

        conn.createStatement().use { st ->
            if (publishedExists) {
                st.executeUpdate(
                    """
                    INSERT INTO banner_patterns (pattern_signature, banner_item_data)
                    SELECT pattern_signature, banner_data
                    FROM $LEGACY_BANNER_SCHEMA.banner_published
                    WHERE pattern_signature IS NOT NULL
                    AND true
                    ON CONFLICT(pattern_signature) DO UPDATE SET
                    banner_item_data=excluded.banner_item_data
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    INSERT OR IGNORE INTO banner_publications (
                        id,
                        author_uuid,
                        author_name,
                        author_name_lower,
                        title,
                        title_lower,
                        category_key,
                        pattern_signature,
                        takes_count,
                        published_at
                    )
                    SELECT
                        id,
                        author_uuid,
                        author_name,
                        author_name_lower,
                        title,
                        title_lower,
                        category_key,
                        pattern_signature,
                        takes_count,
                        published_at
                    FROM $LEGACY_BANNER_SCHEMA.banner_published
                    """.trimIndent()
                )
            }
            if (blockedPatternsExists) {
                st.executeUpdate(
                    """
                    INSERT INTO banner_patterns (pattern_signature, banner_item_data)
                    SELECT pattern_signature, banner_data
                    FROM $LEGACY_BANNER_SCHEMA.banner_banned_patterns
                    WHERE pattern_signature IS NOT NULL
                    AND true
                    ON CONFLICT(pattern_signature) DO UPDATE SET
                    banner_item_data=excluded.banner_item_data
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    INSERT OR REPLACE INTO banner_blocked_patterns (pattern_signature, blocked_at)
                    SELECT pattern_signature, banned_at
                    FROM $LEGACY_BANNER_SCHEMA.banner_banned_patterns
                    """.trimIndent()
                )
            }
            if (blockedPlayersExists) {
                st.executeUpdate(
                    """
                    INSERT OR REPLACE INTO banner_blocked_players (
                        player_uuid,
                        player_name,
                        player_name_lower,
                        reason,
                        skin_value,
                        skin_signature,
                        blocked_at
                    )
                    SELECT
                        player_uuid,
                        player_name,
                        player_name_lower,
                        reason,
                        skin_value,
                        skin_signature,
                        banned_at
                    FROM $LEGACY_BANNER_SCHEMA.banner_banned_users
                    """.trimIndent()
                )
            }
        }

        return true
    }

    private fun migrateLegacyHeadDatabase(conn: Connection): Boolean {
        val catalogExists = tableExists(conn, LEGACY_HEAD_SCHEMA, "decoration_head_catalog")
        val recentExists = tableExists(conn, LEGACY_HEAD_SCHEMA, "decoration_head_recent")
        val savedPagesExists = tableExists(conn, LEGACY_HEAD_SCHEMA, "decoration_head_saved_pages")
        if (!catalogExists && !recentExists && !savedPagesExists) {
            return false
        }

        conn.createStatement().use { st ->
            if (catalogExists) {
                st.executeUpdate(
                    """
                    INSERT INTO head_catalog_entries (
                        stable_key,
                        display_name,
                        display_name_ru,
                        source_category_id,
                        texture_value,
                        updated_at
                    )
                    SELECT
                        stable_key,
                        head_name,
                        head_name_ru,
                        category_id,
                        texture_value,
                        updated_at
                    FROM $LEGACY_HEAD_SCHEMA.decoration_head_catalog
                    WHERE true
                    ON CONFLICT(stable_key) DO UPDATE SET
                    display_name=excluded.display_name,
                    display_name_ru=COALESCE(excluded.display_name_ru, head_catalog_entries.display_name_ru),
                    source_category_id=excluded.source_category_id,
                    texture_value=excluded.texture_value,
                    updated_at=excluded.updated_at
                    """.trimIndent()
                )
            }
            if (recentExists) {
                st.executeUpdate(
                    """
                    INSERT OR IGNORE INTO head_catalog_entries (
                        stable_key,
                        display_name,
                        display_name_ru,
                        source_category_id,
                        texture_value,
                        updated_at
                    )
                    SELECT
                        stable_key,
                        head_name,
                        NULL,
                        category_id,
                        texture_value,
                        last_used_at * 1000
                    FROM $LEGACY_HEAD_SCHEMA.decoration_head_recent
                    """.trimIndent()
                )
                st.executeUpdate(
                    """
                    INSERT OR REPLACE INTO head_recent_entries (
                        player_uuid,
                        position,
                        stable_key,
                        last_used_at
                    )
                    SELECT
                        player_uuid,
                        position,
                        stable_key,
                        last_used_at
                    FROM $LEGACY_HEAD_SCHEMA.decoration_head_recent
                    """.trimIndent()
                )
            }
            if (savedPagesExists) {
                st.executeUpdate(
                    """
                    INSERT OR IGNORE INTO head_saved_pages (
                        id,
                        player_uuid,
                        source_mode,
                        category_key,
                        source_page,
                        search_query,
                        search_query_key,
                        note,
                        map_color_key,
                        created_at,
                        updated_at
                    )
                    SELECT
                        id,
                        player_uuid,
                        source_mode,
                        category_key,
                        source_page,
                        search_query,
                        search_query_key,
                        note,
                        map_color_key,
                        created_at,
                        updated_at
                    FROM $LEGACY_HEAD_SCHEMA.decoration_head_saved_pages
                    """.trimIndent()
                )
            }
        }

        return true
    }

    private fun migrateLegacyPlayerDatabase(conn: Connection): Boolean {
        if (!tableExists(conn, LEGACY_PLAYER_SCHEMA, "edit_personal_items")) {
            return false
        }

        conn.createStatement().use { st ->
            st.executeUpdate(
                """
                INSERT OR REPLACE INTO edit_personal_items (
                    player_uuid,
                    content_hash,
                    item_data,
                    last_used_at
                )
                SELECT
                    player_uuid,
                    content_hash,
                    item_data,
                    last_used_at
                FROM $LEGACY_PLAYER_SCHEMA.edit_personal_items
                """.trimIndent()
            )
        }

        return true
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

    private fun attachDatabase(conn: Connection, file: File, schemaName: String) {
        conn.createStatement().use { st ->
            st.execute("ATTACH DATABASE '${escapeSqlLiteral(file.absolutePath)}' AS $schemaName")
        }
    }

    private fun tableExists(conn: Connection, schemaName: String, tableName: String): Boolean {
        return conn.prepareStatement(
            """
            SELECT 1
            FROM $schemaName.sqlite_master
            WHERE type='table' AND name=?
            LIMIT 1
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, tableName)
            ps.executeQuery().use { rs -> rs.next() }
        }
    }

    private fun hasMetaValue(conn: Connection, key: String): Boolean {
        return conn.prepareStatement("SELECT 1 FROM storage_meta WHERE key=? LIMIT 1").use { ps ->
            ps.setString(1, key)
            ps.executeQuery().use { rs -> rs.next() }
        }
    }

    private fun upsertMetaValue(conn: Connection, key: String, value: String) {
        conn.prepareStatement(
            """
            INSERT INTO storage_meta(key, value)
            VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value=excluded.value
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, key)
            ps.setString(2, value)
            ps.executeUpdate()
        }
    }

    private fun escapeSqlLiteral(value: String): String = value.replace("'", "''")

    private companion object {
        const val DATABASE_FILE_NAME = "advanced-creative-data.db"
        const val LEGACY_BANNER_DATABASE_FILE_NAME = "banner.db"
        const val LEGACY_HEAD_DATABASE_FILE_NAME = "decoration-heads.db"
        const val LEGACY_PLAYER_DATABASE_FILE_NAME = "player_data.db"
        const val LEGACY_BANNER_SCHEMA = "legacy_banner"
        const val LEGACY_HEAD_SCHEMA = "legacy_heads"
        const val LEGACY_PLAYER_SCHEMA = "legacy_player"
        const val LEGACY_MIGRATION_META_KEY = "legacy_split_databases_migrated_at"
        const val BUSY_TIMEOUT_MILLIS = 5_000
        const val SCHEMA_VERSION = 1
    }
}
