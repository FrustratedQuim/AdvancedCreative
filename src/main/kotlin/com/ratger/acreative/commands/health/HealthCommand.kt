package com.ratger.acreative.commands.health

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class HealthCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.HEALTH) {
    companion object {
        private val HEALTH_SUGGESTIONS = listOf("1", "10", "50", "100", "basic")
    }

    override fun handle(player: Player, args: Array<out String>) = hooker.healthManager.applyEffect(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (args.size == 1) {
            HEALTH_SUGGESTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
