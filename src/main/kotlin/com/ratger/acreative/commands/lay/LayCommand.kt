package com.ratger.acreative.commands.lay

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class LayCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.LAY) {
    override fun handle(player: Player, args: Array<out String>) = hooker.layManager.layPlayer(player)
}
