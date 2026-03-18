package com.ratger.acreative.commands.effects

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EffectsCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.EFFECTS) {
    companion object {
        private val LEVEL_SUGGESTIONS = listOf("1", "2", "3", "5", "10")
    }

    override fun handle(player: Player, args: Array<out String>) = hooker.effectsManager.applyEffect(player, args.getOrNull(0), args.getOrNull(1), args.getOrNull(2))

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> (Registry.EFFECT.iterator().asSequence().map { it.key.key.lowercase() } + "clear")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
                .toList()

            2 -> LEVEL_SUGGESTIONS.filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> {
                if (sender.hasPermission("advancedcreative.effects.other")) {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
    }
}
