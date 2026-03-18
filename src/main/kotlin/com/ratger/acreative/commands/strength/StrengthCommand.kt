package com.ratger.acreative.commands.strength

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StrengthCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.STRENGTH) {
    override fun handle(player: Player, args: Array<out String>) = hooker.strengthManager.applyEffect(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return hooker.strengthManager.tabCompletions(args)
    }
}
