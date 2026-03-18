package com.ratger.acreative.commands.hide

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Bukkit

class HideCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.HIDE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.hideManager.prepareToHidePlayer(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (args.size == 1 || args.size == 2) {
            Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
