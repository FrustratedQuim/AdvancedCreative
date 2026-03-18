package com.ratger.acreative.commands.effects

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EffectsCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.EFFECTS) {
    override fun handle(player: Player, args: Array<out String>) = hooker.effectsManager.applyEffect(player, args.getOrNull(0), args.getOrNull(1), args.getOrNull(2))

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return hooker.effectsManager.tabCompletions(sender, args)
    }
}
