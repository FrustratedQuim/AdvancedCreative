package com.ratger.acreative.commands.slap

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class SlapCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SLAP) {
    override fun handle(player: Player, args: Array<out String>) = hooker.slapManager.slapPlayer(player)
}
