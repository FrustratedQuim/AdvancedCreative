package com.ratger.acreative.core

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

class MessageManager(
    private val hooker: FunctionHooker,
    private val configManager: ConfigManager
) {

    private val miniMessage = MiniMessage.miniMessage()

    private val chatTasks = mutableMapOf<UUID, BukkitTask>()
    private val actionTasks = mutableMapOf<UUID, BukkitTask>()

    fun sendMiniMessage(
        player: Player,
        type: String? = "CHAT",
        key: String? = "info-empty",
        variables: Map<String, String>? = null,
        repeatable: Boolean = false,
        repeatFrequency: Long = 20L
    ) {
        val msgType = type ?: "CHAT"
        val msgKey = key ?: "info-empty"

        println("[sendMiniMessage] Key: $msgKey")

        val rawMessage = configManager.messages.getString("messages.$msgKey")
            ?: configManager.messages.getString("messages.info-empty")
            ?: ""
        val processedMessage = replaceVariables(rawMessage, variables)
        val finalResult = miniMessage.deserialize(processedMessage)

        when (msgType.uppercase()) {
            "CHAT" -> {
                if (repeatable) {
                    chatTasks[player.uniqueId]?.cancel()
                    chatTasks[player.uniqueId] = object : BukkitRunnable() {
                        override fun run() {
                            player.sendMessage(finalResult)
                        }
                    }.runTaskTimer(hooker.plugin, 0L, repeatFrequency)
                } else {
                    player.sendMessage(finalResult)
                }
            }
            "CHAT_STOP" -> {
                chatTasks[player.uniqueId]?.cancel()
                chatTasks.remove(player.uniqueId)
            }
            "ACTION" -> {
                if (repeatable) {
                    actionTasks[player.uniqueId]?.cancel()
                    actionTasks[player.uniqueId] = object : BukkitRunnable() {
                        override fun run() {
                            player.sendActionBar(finalResult)
                        }
                    }.runTaskTimer(hooker.plugin, 0L, repeatFrequency)
                } else {
                    player.sendActionBar(finalResult)
                }
            }
            "ACTION_STOP" -> {
                actionTasks[player.uniqueId]?.cancel()
                actionTasks.remove(player.uniqueId)
                player.sendActionBar(miniMessage.deserialize(" "))
            }
        }
    }

    private fun replaceVariables(message: String, variables: Map<String, String>?): String {
        if (variables == null) return message.trimEnd()
        var result = message
        for ((key, value) in variables) {
            result = result.replace("%$key%", value)
        }
        return result.trimEnd()
    }

    fun clearAllTasks() {
        chatTasks.values.forEach { it.cancel() }
        chatTasks.clear()
        actionTasks.values.forEach { it.cancel() }
        actionTasks.clear()
    }
}
