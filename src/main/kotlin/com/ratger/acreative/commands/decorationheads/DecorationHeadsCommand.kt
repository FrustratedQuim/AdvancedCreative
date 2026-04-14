package com.ratger.acreative.commands.decorationheads

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class DecorationHeadsCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DECORATIONHEADS) {
    override fun handle(player: Player, args: Array<out String>) {
        hooker.decorationHeadsMenuService.open(player)
    }
}
