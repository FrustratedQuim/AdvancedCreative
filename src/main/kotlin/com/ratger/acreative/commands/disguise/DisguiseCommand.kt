package com.ratger.acreative.commands.disguise

import com.ratger.acreative.commands.disguise.command.DisguiseArgumentParser
import com.ratger.acreative.commands.disguise.command.DisguiseTabCompleter
import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DisguiseCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DISGUISE) {
    private val argumentParser = DisguiseArgumentParser()
    private val tabCompleter = DisguiseTabCompleter(hooker)

    override fun handle(player: Player, args: Array<out String>) {
        hooker.disguiseManager.disguisePlayer(player, argumentParser.parse(args))
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return tabCompleter.complete(sender, args)
    }

    override fun hasPermission(player: Player): Boolean {
        return player.hasPermission(DisguisePermissions.BASE)
    }
}
