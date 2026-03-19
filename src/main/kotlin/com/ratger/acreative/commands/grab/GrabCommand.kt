package com.ratger.acreative.commands.grab

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GrabCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GRAB) {
    override fun handle(player: Player, args: Array<out String>) = hooker.grabManager.handleGrabCommand(player, args)

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[0], ignoreCase = true) }

            2 -> listOf("-force").filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
