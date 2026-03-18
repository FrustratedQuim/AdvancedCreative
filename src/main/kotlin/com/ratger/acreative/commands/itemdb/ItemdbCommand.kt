package com.ratger.acreative.commands.itemdb

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class ItemdbCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.ITEMDB) {
    override fun handle(player: Player, args: Array<out String>) = hooker.itemdbManager.showItemInfo(player)
}
