package com.ratger.acreative.menus.decorationheads.persistence

class SyncStateRepository(
    private val database: Database
) {
    fun isInitialSyncCompleted(): Boolean = getState(INITIAL_SYNC_COMPLETED)?.toBoolean() == true

    fun markInitialSyncCompleted() {
        setState(INITIAL_SYNC_COMPLETED, true.toString())
    }

    fun getState(key: String): String? = database.connection().use { conn ->
        conn.prepareStatement(
            "SELECT state_value FROM decoration_head_sync_state WHERE state_key = ?"
        ).use { ps ->
            ps.setString(1, key)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.getString("state_value") else null
            }
        }
    }

    fun setState(key: String, value: String) {
        database.connection().use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO decoration_head_sync_state(state_key, state_value, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(state_key) DO UPDATE SET
                state_value = excluded.state_value,
                updated_at = excluded.updated_at
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, key)
                ps.setString(2, value)
                ps.setLong(3, System.currentTimeMillis())
                ps.executeUpdate()
            }
        }
    }

    private companion object {
        const val INITIAL_SYNC_COMPLETED = "initial_sync_completed"
    }
}
