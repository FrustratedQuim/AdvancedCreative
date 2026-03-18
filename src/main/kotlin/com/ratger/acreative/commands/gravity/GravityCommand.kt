package com.ratger.acreative.commands.gravity

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GravityCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GRAVITY) {
    companion object {
        private val GRAVITY_SUGGESTIONS = listOf("0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0", "basic")
    }

    override fun handle(player: Player, args: Array<out String>) = hooker.gravityManager.applyEffect(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (args.size == 1) {
            GRAVITY_SUGGESTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
