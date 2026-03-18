package com.ratger.acreative.commands.sneeze

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class SneezeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SNEEZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.sneezeManager.sneezePlayer(player)
}
