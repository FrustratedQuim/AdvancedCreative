package com.ratger.acreative.commands.banner

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class MyFlagsCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.MYFLAGS) {
    override fun handle(player: Player, args: Array<out String>) {
        hooker.bannerMenuService.openStorageFromCommand(player)
    }
}
