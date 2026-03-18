package com.ratger.acreative.commands.resize

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ResizeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.RESIZE) {
    companion object {
        private val RESIZE_SUGGESTIONS = listOf("0.1", "0.5", "1.0", "1.5", "5.0", "10.0", "15.0", "basic")
    }

    override fun handle(player: Player, args: Array<out String>) = hooker.resizeManager.applyEffectFromCommand(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (args.size == 1) {
            RESIZE_SUGGESTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
