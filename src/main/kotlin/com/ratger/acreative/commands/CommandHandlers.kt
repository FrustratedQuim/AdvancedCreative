package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Bukkit
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

private fun completeFromList(args: Array<out String>, options: List<String>): List<String> {
    return if (args.size == 1) options.filter { it.startsWith(args[0], ignoreCase = true) } else emptyList()
}

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

class SitCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SIT) {
    override fun handle(player: Player, args: Array<out String>) = hooker.sitManager.sitPlayer(player)
}

class LayCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.LAY) {
    override fun handle(player: Player, args: Array<out String>) = hooker.layManager.layPlayer(player)
}

class CrawlCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.CRAWL) {
    override fun handle(player: Player, args: Array<out String>) = hooker.crawlManager.crawlPlayer(player)
}

class HideCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.HIDE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.hideManager.prepareToHidePlayer(player, args.firstOrNull())
    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = completeOnlinePlayers(args)
}

class SneezeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SNEEZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.sneezeManager.sneezePlayer(player)
}

class GlideCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GLIDE) {
    private val boostOptions = listOf("0", "0.1", "0.3", "0.5", "0.7", "1.0")

    override fun handle(player: Player, args: Array<out String>) {
        val boost = args.firstOrNull()?.toDoubleOrNull()?.takeIf { it in 0.0..1.0 } ?: 0.0
        hooker.glideManager.glidePlayer(player, boost)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = completeFromList(args, boostOptions)
}

class GravityCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GRAVITY) {
    override fun handle(player: Player, args: Array<out String>) = hooker.gravityManager.applyEffect(player, args.firstOrNull())
    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = completeFromList(args, listOf("0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0", "basic"))
}

class ResizeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.RESIZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.resizeManager.applyEffect(player, args.firstOrNull())
    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = completeFromList(args, listOf("0.1", "0.5", "1.0", "1.5", "5.0", "10.0", "15.0", "basic"))
}

class FreezeCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.FREEZE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.freezeManager.prepareToFreezePlayer(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return if (sender.hasPermission("advancedcreative.freeze.other")) completeOnlinePlayers(args) else emptyList()
    }
}

class GlowCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GLOW) {
    override fun handle(player: Player, args: Array<out String>) = hooker.glowManager.glowPlayer(player)
}

class SpitCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SPIT) {
    override fun handle(player: Player, args: Array<out String>) = hooker.spitManager.spitPlayer(player)
}

class PissCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.PISS) {
    override fun handle(player: Player, args: Array<out String>) = hooker.pissManager.pissPlayer(player)
}

class DisguiseCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DISGUISE) {
    override fun handle(player: Player, args: Array<out String>) = hooker.disguiseManager.disguisePlayer(player, args.firstOrNull(), args.getOrNull(1))

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

        if (args.size == 2) {
            return listOf("-self", "-noself").filter { it.startsWith(args[1], ignoreCase = true) }
        }

        return emptyList()
    }
}

class EffectsCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.EFFECTS) {
    override fun handle(player: Player, args: Array<out String>) = hooker.effectsManager.applyEffect(player, args.getOrNull(0), args.getOrNull(1), args.getOrNull(2))

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> (Registry.EFFECT.iterator().asSequence().map { it.key.key.lowercase() } + "clear")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .sorted()
                .toList()

            2 -> listOf("1", "2", "3", "5", "10").filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> {
                if (sender.hasPermission("advancedcreative.effects.other")) {
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
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
