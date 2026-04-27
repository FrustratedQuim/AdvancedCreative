package com.ratger.acreative.commands.disguise

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.EntityType

class DisguiseCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DISGUISE) {
    private companion object {
        const val EXTENDED_PERMISSION = "advancedcreative.disguise.extended"
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
            2, 3 -> completeFlags(sender, args.last())
            else -> emptyList()
        }
    }

    private fun completeDisguiseTypes(sender: CommandSender, currentArg: String): List<String> {
        val blocked = hooker.configManager.getBlockedDisguises()
        val allowedTypes = EntityType.entries
            .asSequence()
            .filter { it !in blocked }
            .filter { sender.hasPermission(EXTENDED_PERMISSION) || it !in hooker.disguiseManager.restrictedEntities }
            .map { it.name.lowercase() }
            .toMutableList()

        allowedTypes.addAll(listOf("off", "player"))
        return allowedTypes.filter { it.startsWith(currentArg, ignoreCase = true) }
    }

    private fun completeFlags(sender: CommandSender, currentArg: String): List<String> {
        val availableFlags = buildList {
            add("-self")
            add("-noself")
            if (sender.hasPermission("advancedcreative.disguise.nick")) {
                add("-withnick")
                add("-nonick")
            }
        }

        return availableFlags
            .asSequence()
            .distinct()
            .filter { it.startsWith(currentArg, ignoreCase = true) }
            .toList()
    }
}
