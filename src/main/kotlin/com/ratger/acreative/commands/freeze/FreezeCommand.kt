package com.ratger.acreative.commands.freeze

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Bukkit

class FreezeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.FREEZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.freezeManager.prepareToFreezePlayer(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (sender.hasPermission("advancedcreative.freeze.other")) {
            if (args.size == 1 || args.size == 2) {
                Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
