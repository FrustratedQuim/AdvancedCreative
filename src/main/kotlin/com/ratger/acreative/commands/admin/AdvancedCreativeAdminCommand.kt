package com.ratger.acreative.commands.admin

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.ManagedSystem
import com.ratger.acreative.core.MessageKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdvancedCreativeAdminCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.ACREATIVE) {

    override fun handle(player: Player, args: Array<out String>) {
        when (args.firstOrNull()?.lowercase()) {
            "memory" -> hooker.adminManager.showMemoryUsage(player)
            "toggle" -> handleToggle(player, args.drop(1))
            "status" -> handleStatus(player, args.drop(1))
            "heads" -> handleHeads(player, args.drop(1))
            else -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
        }
    }

    private fun handleToggle(player: Player, args: List<String>) {
        val system = args.firstOrNull()?.let(ManagedSystem::fromId)
        if (system == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
            return
        }

        val enabled = hooker.systemToggleService.toggle(system)
        hooker.messageManager.sendChat(
            player,
            if (enabled) MessageKey.SYSTEM_TOGGLE_ENABLED else MessageKey.SYSTEM_TOGGLE_DISABLED,
            mapOf("system" to system.displayName)
        )
    }

    private fun handleStatus(player: Player, args: List<String>) {
        when (args.firstOrNull()?.lowercase()) {
            null, "toggle" -> hooker.adminManager.showToggleStatus(player)
            else -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
        }
    }

    private fun handleHeads(player: Player, args: List<String>) {
        when (args.firstOrNull()?.lowercase()) {
            "restore_from_dat" -> hooker.adminManager.restoreHeadsFromDat(player)
            "restore_from_api" -> hooker.adminManager.restoreHeadsFromApi(player)
            else -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        return when (args.size) {
            1 -> listOf("memory", "heads", "toggle", "status").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when {
                args[0].equals("heads", ignoreCase = true) -> {
                    listOf("restore_from_dat", "restore_from_api")
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                args[0].equals("toggle", ignoreCase = true) -> {
                    ManagedSystem.entries.map { it.id }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                args[0].equals("status", ignoreCase = true) -> {
                    listOf("toggle").filter { it.startsWith(args[1], ignoreCase = true) }
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
