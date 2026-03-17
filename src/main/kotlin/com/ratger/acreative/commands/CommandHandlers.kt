package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

private fun completeOnlinePlayers(args: Array<out String>): List<String> {
    return if (args.size == 1 || args.size == 2) {
        Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
    } else {
        emptyList()
    }
}

class AhelpCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.AHELP) {
    override fun handle(player: Player, args: Array<out String>) = hooker.messageManager.sendChat(player, MessageKey.AHELP)
}

class FreezeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.FREEZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.freezeManager.prepareToFreezePlayer(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (sender.hasPermission("advancedcreative.freeze.other")) completeOnlinePlayers(args) else emptyList()
    }
}

class SpitCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SPIT) {
    override fun handle(player: Player, args: Array<out String>) = hooker.spitManager.spitPlayer(player)
}

class PissCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.PISS) {
    override fun handle(player: Player, args: Array<out String>) = hooker.pissManager.pissPlayer(player)
}

class DisguiseCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DISGUISE) {
    private val allFlags = listOf("-self", "-noself", "-withnick", "-nonick")

    override fun handle(player: Player, args: Array<out String>) {
        val type = args.firstOrNull { !it.startsWith("-") }
        val flags = args.filter { it.startsWith("-") }
        hooker.disguiseManager.disguisePlayer(player, type, flags)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.isEmpty()) return emptyList()

        if (args.size == 1) {
            val blocked = hooker.configManager.getBlockedDisguises()
            val types = EntityType.entries.filter { it !in blocked }.map { it.name.lowercase() }.toMutableList()
            if (!sender.hasPermission("advancedcreative.disguise.full")) {
                types.removeAll(hooker.disguiseManager.restrictedEntities.map { it.name.lowercase() })
            }
            types.addAll(listOf("off", "player"))
            return types.filter { it.startsWith(args[0], ignoreCase = true) }
        }

        if (args.size == 2 || args.size == 3) {
            return allFlags
                .asSequence()
                .distinct()
                .filter { it.startsWith(args.last(), ignoreCase = true) }
                .toList()
        }

        return emptyList()
    }
}

class SlapCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SLAP) {
    override fun handle(player: Player, args: Array<out String>) = hooker.slapManager.slapPlayer(player)
}

class SitheadCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SITHEAD) {
    override fun handle(player: Player, args: Array<out String>) = hooker.sitheadManager.prepareToSithead(player, args.getOrNull(0), args.getOrNull(1))

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (sender.hasPermission("advancedcreative.sithead.other")) {
            val completions = mutableListOf<String>()
            if (args.size < 2) completions.add("toggle")
            if (args.size < 3 && !args.contains("toggle")) {
                completions.addAll(completeOnlinePlayers(args))
                return completions.filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
            }
            return completions
        }

        if (args.size < 2) return listOf("toggle")
        return emptyList()
    }
}

class ItemdbCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.ITEMDB) {
    override fun handle(player: Player, args: Array<out String>) = hooker.itemdbManager.showItemInfo(player)
}
