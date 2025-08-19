package com.ratger.acreative.commands.bind

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BindManager(private val hooker: FunctionHooker) {

    data class BindData(val item: Material?, val command: String)

    val binds = ConcurrentHashMap<UUID, MutableList<BindData>>()
    val lastBindUse = ConcurrentHashMap<UUID, Long>()
    private val bindExecuteCooldown = 10L

    fun prepareToBindCommand(player: Player, type: String?, commandArgs: Array<out String>) {
        if (type == null) {
            hooker.messageManager.sendMiniMessage(player, key = "usage-bind")
            return
        }

        when (type.lowercase()) {
            "resetall" -> resetAllBinds(player)
            "reset" -> resetBind(player)
            "set" -> setBind(player, commandArgs)
            else -> hooker.messageManager.sendMiniMessage(player, key = "error-bind-type")
        }
    }

    private fun setBind(player: Player, commandArgs: Array<out String>) {
        if (commandArgs.isEmpty()) {
            hooker.messageManager.sendMiniMessage(player, key = "error-bind-no-command")
            return
        }

        val command = commandArgs.joinToString(" ").removePrefix("/")
        val itemInHand = player.inventory.itemInMainHand
        val material = if (itemInHand.type == Material.AIR) null else itemInHand.type

        val playerBinds = binds.computeIfAbsent(player.uniqueId) { mutableListOf() }
        playerBinds.removeIf { it.item == material }
        playerBinds.add(BindData(material, command))

        val messageKey = if (material == null) "success-bind-hand" else "success-bind-item"
        hooker.messageManager.sendMiniMessage(player, key = messageKey)
    }

    private fun resetBind(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val material = if (itemInHand.type == Material.AIR) null else itemInHand.type
        val playerBinds = binds[player.uniqueId]

        if (playerBinds != null) {
            playerBinds.removeIf { it.item == material }
            if (playerBinds.isEmpty()) {
                binds.remove(player.uniqueId)
            }
            val messageKey = if (material == null) "success-bind-reset-hand" else "success-bind-reset-item"
            hooker.messageManager.sendMiniMessage(player, key = messageKey)
        }
    }

    private fun resetAllBinds(player: Player) {
        binds.remove(player.uniqueId)
        lastBindUse.remove(player.uniqueId)
        hooker.messageManager.sendMiniMessage(player, key = "success-bind-reset-all")
    }

    fun executeBind(player: Player) {
        val now = System.currentTimeMillis()
        if (lastBindUse[player.uniqueId]?.let { now - it < bindExecuteCooldown } == true) {
            return
        }

        val itemInHand = player.inventory.itemInMainHand
        val material = if (itemInHand.type == Material.AIR) null else itemInHand.type
        val bind = binds[player.uniqueId]?.find { it.item == material } ?: return

        lastBindUse[player.uniqueId] = now
        hooker.plugin.server.dispatchCommand(player, bind.command)
    }

    fun clearBinds(player: Player) {
        binds.remove(player.uniqueId)
        lastBindUse.remove(player.uniqueId)
    }
}