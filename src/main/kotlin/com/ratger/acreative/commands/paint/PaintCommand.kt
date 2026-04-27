package com.ratger.acreative.commands.paint

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PaintCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.PAINT) {
    override fun handle(player: Player, args: Array<out String>) {
        hooker.paintManager.handlePaintCommand(player, args)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = emptyList()
}
