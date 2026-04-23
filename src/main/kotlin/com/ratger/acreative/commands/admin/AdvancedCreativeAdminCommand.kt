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
            else -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_ARGUMENT)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()
        return when (args.size) {
            1 -> listOf("memory").filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
