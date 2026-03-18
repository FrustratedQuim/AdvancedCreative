package com.ratger.acreative.commands.glide

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GlideCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GLIDE) {
    companion object {
        private val BOOST_OPTIONS = listOf("0", "0.1", "0.3", "0.5", "0.7", "1.0")
    }

    override fun handle(player: Player, args: Array<out String>) {
        val boost = hooker.glideManager.parseBoost(args.firstOrNull())
        hooker.glideManager.glidePlayer(player, boost)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (args.size == 1) {
            BOOST_OPTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
