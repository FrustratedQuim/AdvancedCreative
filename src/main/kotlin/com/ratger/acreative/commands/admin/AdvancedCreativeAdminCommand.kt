package com.ratger.acreative.commands.admin

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdvancedCreativeAdminCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.ACREATIVE) {

    override fun handle(player: Player, args: Array<out String>) {
        when (args.firstOrNull()?.lowercase()) {
            "memory" -> hooker.adminManager.showMemoryUsage(player)
            "heads" -> handleHeads(player, args.drop(1))
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
            1 -> listOf("memory", "heads").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("heads", ignoreCase = true)) {
                listOf("restore_from_dat", "restore_from_api")
                    .filter { it.startsWith(args[1], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }
}
