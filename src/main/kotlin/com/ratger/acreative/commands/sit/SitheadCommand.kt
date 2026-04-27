package com.ratger.acreative.commands.sit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Bukkit

class SitheadCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SITHEAD) {

    override fun handle(player: Player, args: Array<out String>) {
        hooker.sitheadManager.prepareToSithead(player, args.getOrNull(0), args.getOrNull(1))
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (sender.hasPermission("advancedcreative.sithead.other")) {
            val completions = mutableListOf<String>()
            if (args.size < 2 && sender.hasPermission("advancedcreative.sithead")) completions.add("toggle")
            if (args.size < 3 && !args.contains("toggle")) {
                completions.addAll(completeOnlinePlayers(args))
                return completions.filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
            }
            return completions
        }

        return if (args.size < 2 && sender.hasPermission("advancedcreative.sithead")) listOf("toggle") else emptyList()
    }

    private fun completeOnlinePlayers(args: Array<out String>): List<String> {
        return if (args.size == 1 || args.size == 2) {
            Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
