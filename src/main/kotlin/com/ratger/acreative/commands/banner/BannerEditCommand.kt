package com.ratger.acreative.commands.banner

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class BannerEditCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.BANNEREDIT) {
    override fun handle(player: Player, args: Array<out String>) {
        hooker.bannerMenuService.openEditor(player, openedFromMainMenu = false)
    }
}
