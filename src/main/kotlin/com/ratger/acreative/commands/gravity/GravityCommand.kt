package com.ratger.acreative.commands.gravity

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GravityCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GRAVITY) {
    override fun handle(player: Player, args: Array<out String>) = hooker.gravityManager.applyEffect(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return hooker.gravityManager.tabCompletions(args)
    }
}
