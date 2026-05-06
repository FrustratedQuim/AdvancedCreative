package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AhelpCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.AHELP) {
    private val pageService = AhelpPageService(hooker)

    override fun handle(player: Player, args: Array<out String>) {
        val requestedPage = args.firstOrNull()?.toIntOrNull()
        hooker.messageManager.sendMiniMessageChat(player, pageService.renderFor(player, requestedPage))
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = emptyList()
}
