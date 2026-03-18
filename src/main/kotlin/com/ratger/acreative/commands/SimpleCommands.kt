package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.entity.Player

class AhelpCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.AHELP) {
    override fun handle(player: Player, args: Array<out String>) = hooker.messageManager.sendChat(player, MessageKey.AHELP)
}
