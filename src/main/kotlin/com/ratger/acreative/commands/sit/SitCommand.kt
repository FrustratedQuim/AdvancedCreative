package com.ratger.acreative.commands.sit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class SitCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SIT) {
    override fun handle(player: Player, args: Array<out String>) = hooker.sitManager.sitPlayer(player)
}
