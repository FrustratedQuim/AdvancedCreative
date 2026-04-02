package com.ratger.acreative.commands.edit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ApplyCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.APPLY, useCooldown = false) {
    override fun handle(player: Player, args: Array<out String>) {
        hooker.menuService.handleApply(player, args)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()
        return hooker.menuService.tabCompleteApply(player, args)
    }
}
