package com.ratger.acreative.commands.disguise

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.EntityType

class DisguiseCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DISGUISE) {
    private companion object {
        val ALL_FLAGS = listOf("-self", "-noself", "-withnick", "-nonick")
    }

    override fun handle(player: Player, args: Array<out String>) {
        val type = args.firstOrNull { !it.startsWith("-") }
        val flags = args.filter { it.startsWith("-") }
        hooker.disguiseManager.disguisePlayer(player, type, flags)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> completeDisguiseTypes(sender, args[0])
            2, 3 -> completeFlags(args.last())
            else -> emptyList()
        }
    }

    private fun completeDisguiseTypes(sender: CommandSender, currentArg: String): List<String> {
        val blocked = hooker.configManager.getBlockedDisguises()
        val allowedTypes = EntityType.entries
            .asSequence()
            .filter { it !in blocked }
            .filter { sender.hasPermission("advancedcreative.disguise.full") || it !in hooker.disguiseManager.restrictedEntities }
            .map { it.name.lowercase() }
            .toMutableList()

        allowedTypes.addAll(listOf("off", "player"))
        return allowedTypes.filter { it.startsWith(currentArg, ignoreCase = true) }
    }

    private fun completeFlags(currentArg: String): List<String> {
        return ALL_FLAGS
            .asSequence()
            .distinct()
            .filter { it.startsWith(currentArg, ignoreCase = true) }
            .toList()
    }
}
