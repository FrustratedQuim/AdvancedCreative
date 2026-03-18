package com.ratger.acreative.commands.piss

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class PissCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.PISS) {
    override fun handle(player: Player, args: Array<out String>) = hooker.pissManager.pissPlayer(player)
}
