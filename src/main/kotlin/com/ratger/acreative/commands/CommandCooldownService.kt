package com.ratger.acreative.commands

import com.ratger.acreative.core.ConfigManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CommandCooldownService(private val configManager: ConfigManager) {
    private val playerCooldowns = ConcurrentHashMap<UUID, MutableMap<PluginCommandType, Long>>()

    fun remainingMillis(playerId: UUID, commandType: PluginCommandType): Long {
        val now = System.currentTimeMillis()
        val playerEntries = playerCooldowns[playerId] ?: return 0
        val expiresAt = playerEntries[commandType] ?: return 0
        if (expiresAt <= now) {
            playerEntries.remove(commandType)
            if (playerEntries.isEmpty()) {
                playerCooldowns.remove(playerId, playerEntries)
            }
            return 0
        }
        return expiresAt - now
    }

    fun setCooldown(playerId: UUID, commandType: PluginCommandType) {
        pruneExpired()
        val cooldownMillis = configManager.config.getLong("cooldowns.${commandType.cooldownKey}", 1000L)
        playerCooldowns.computeIfAbsent(playerId) { mutableMapOf() }[commandType] = System.currentTimeMillis() + cooldownMillis
    }

    fun clearPlayer(playerId: UUID) {
        playerCooldowns.remove(playerId)
    }

    fun cachedPlayersCount(): Int {
        pruneExpired()
        return playerCooldowns.size
    }

    fun cachedEntriesCount(): Int {
        pruneExpired()
        return playerCooldowns.values.sumOf { it.size }
    }

    private fun pruneExpired(now: Long = System.currentTimeMillis()) {
        playerCooldowns.entries.removeIf { (_, cooldowns) ->
            cooldowns.entries.removeIf { it.value <= now }
            cooldowns.isEmpty()
        }
    }
}
