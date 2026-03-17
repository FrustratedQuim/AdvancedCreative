package com.ratger.acreative.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class MessageManager(
    private val hooker: FunctionHooker
) {

    private data class AudienceTask(
        val audienceId: UUID,
        val channel: MessageChannel,
        val key: MessageKey,
        val variables: Map<String, String>,
        val frequencyTicks: Long,
        var state: MessageTaskState = MessageTaskState.PENDING,
        var nextRunTick: Long = 0L
    )

    private val miniMessage = MiniMessage.miniMessage()
    private val repeatTasks = mutableMapOf<Pair<UUID, MessageChannel>, AudienceTask>()

    private var tickerTaskId: Int? = null
    private var virtualTick: Long = 0L

    init {
        startTicker()
    }

    fun sendChat(player: Player, key: MessageKey, variables: Map<String, String> = emptyMap()) {
        send(player, MessageChannel.CHAT, key, variables)
    }

    fun sendActionBar(player: Player, key: MessageKey, variables: Map<String, String> = emptyMap()) {
        send(player, MessageChannel.ACTION_BAR, key, variables)
    }

    fun sendChatToPlayers(players: Collection<Player>, key: MessageKey, variables: Map<String, String> = emptyMap()) {
        players.forEach { sendChat(it, key, variables) }
    }

    fun sendChatToUuids(uuids: Collection<UUID>, key: MessageKey, variables: Map<String, String> = emptyMap()) {
        resolvePlayers(uuids = uuids).forEach { sendChat(it, key, variables) }
    }

    fun sendActionBarToPlayers(players: Collection<Player>, key: MessageKey, variables: Map<String, String> = emptyMap()) {
        players.forEach { sendActionBar(it, key, variables) }
    }

    fun startRepeatingActionBar(player: Player, key: MessageKey, variables: Map<String, String> = emptyMap(), repeatFrequency: Long = 20L) {
        startRepeating(player, MessageChannel.ACTION_BAR, key, variables, repeatFrequency)
    }

    fun startRepeatingChat(player: Player, key: MessageKey, variables: Map<String, String> = emptyMap(), repeatFrequency: Long = 20L) {
        startRepeating(player, MessageChannel.CHAT, key, variables, repeatFrequency)
    }

    fun stopRepeating(player: Player, channel: MessageChannel) {
        stopRepeating(player.uniqueId, channel)
        if (channel == MessageChannel.ACTION_BAR) {
            player.sendActionBar(Component.empty())
        }
    }

    fun pauseRepeating(player: Player, channel: MessageChannel) {
        repeatTasks[player.uniqueId to channel]?.state = MessageTaskState.PAUSED
    }

    fun resumeRepeating(player: Player, channel: MessageChannel) {
        repeatTasks[player.uniqueId to channel]?.let {
            it.state = MessageTaskState.ACTIVE
            it.nextRunTick = virtualTick
        }
    }

    fun clearAllTasks() {
        repeatTasks.values.forEach { it.state = MessageTaskState.CANCELLED }
        repeatTasks.clear()
        tickerTaskId?.let { hooker.tickScheduler.cancel(it) }
        tickerTaskId = null
    }

    private fun startTicker() {
        if (tickerTaskId != null) return
        tickerTaskId = hooker.tickScheduler.runRepeating(1L, 1L) {
            virtualTick++
            processRepeatingTasks()
        }
    }

    private fun processRepeatingTasks() {
        val iterator = repeatTasks.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next().value
            when (task.state) {
                MessageTaskState.PENDING -> {
                    task.state = MessageTaskState.ACTIVE
                    task.nextRunTick = virtualTick
                }

                MessageTaskState.ACTIVE -> {
                    if (virtualTick >= task.nextRunTick) {
                        send(task.audienceId, task.channel, task.key, task.variables)
                        task.nextRunTick = virtualTick + task.frequencyTicks
                    }
                }

                MessageTaskState.PAUSED -> Unit
                MessageTaskState.CANCELLED -> iterator.remove()
            }
        }
    }

    private fun startRepeating(
        player: Player,
        channel: MessageChannel,
        key: MessageKey,
        variables: Map<String, String>,
        repeatFrequency: Long
    ) {
        val frequency = repeatFrequency.coerceAtLeast(1L)
        repeatTasks[player.uniqueId to channel] = AudienceTask(
            player.uniqueId,
            channel,
            key,
            variables,
            frequency
        )
    }

    private fun stopRepeating(audienceId: UUID, channel: MessageChannel) {
        repeatTasks.remove(audienceId to channel)
    }

    private fun send(player: Player, channel: MessageChannel, key: MessageKey, variables: Map<String, String>) {
        val finalResult = renderComponent(key, variables)
        when (channel) {
            MessageChannel.CHAT -> player.sendMessage(finalResult)
            MessageChannel.ACTION_BAR -> player.sendActionBar(finalResult)
        }
    }

    private fun send(audienceId: UUID, channel: MessageChannel, key: MessageKey, variables: Map<String, String>) {
        val player = Bukkit.getPlayer(audienceId) ?: return
        send(player, channel, key, variables)
    }

    private fun renderComponent(key: MessageKey, variables: Map<String, String>): Component {
        val rawMessage = MessageCatalog.templates[key] ?: MessageCatalog.templates[MessageKey.INFO_EMPTY].orEmpty()
        return miniMessage.deserialize(replaceVariables(rawMessage, variables))
    }

    private fun resolvePlayers(players: Collection<Player> = emptyList(), uuids: Collection<UUID> = emptyList()): Set<Player> {
        val resolvedByUuid = uuids.mapNotNull { Bukkit.getPlayer(it) }
        return (players + resolvedByUuid).toSet()
    }

    private fun replaceVariables(message: String, variables: Map<String, String>): String {
        var result = message
        for ((key, value) in variables) {
            result = result.replace("%$key%", value)
        }
        return result.trimEnd()
    }
}
