package com.ratger.acreative.commands.spit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class SpitCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SPIT) {
    override fun handle(player: Player, args: Array<out String>) = hooker.spitManager.spitPlayer(player)
}
