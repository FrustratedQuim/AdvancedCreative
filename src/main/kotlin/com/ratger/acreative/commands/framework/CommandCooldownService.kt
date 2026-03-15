package com.ratger.acreative.commands.framework

import com.ratger.acreative.core.ConfigManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CommandCooldownService(private val configManager: ConfigManager) {
    private val playerCooldowns = ConcurrentHashMap<UUID, MutableMap<PluginCommandType, Long>>()

    fun remainingMillis(playerId: UUID, commandType: PluginCommandType): Long {
        val expiresAt = playerCooldowns[playerId]?.get(commandType) ?: return 0
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun setCooldown(playerId: UUID, commandType: PluginCommandType) {
        val cooldownMillis = configManager.config.getLong("cooldowns.${commandType.cooldownKey}", 1000L)
        playerCooldowns.computeIfAbsent(playerId) { mutableMapOf() }[commandType] = System.currentTimeMillis() + cooldownMillis
    }
}
