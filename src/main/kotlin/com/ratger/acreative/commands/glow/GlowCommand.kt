package com.ratger.acreative.commands.glow

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class GlowCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GLOW) {
    override fun handle(player: Player, args: Array<out String>) = hooker.glowManager.glowPlayer(player)
}
