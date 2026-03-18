package com.ratger.acreative.commands.crawl

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class CrawlCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.CRAWL) {
    override fun handle(player: Player, args: Array<out String>) = hooker.crawlManager.crawlPlayer(player)
}
