package com.ratger.acreative.commands

import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Registry
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

class CommandManager(private val functionHooker: FunctionHooker) : CommandExecutor, TabCompleter {

    private val handledCommands = setOf(
        "ahelp", "sit", "lay", "crawl", "hide",
        "strength", "health", "effects", "itemdb",
        "sneeze", "glide", "gravity", "resize", "freeze",
        "glow", "disguise", "sithead", "spit", "piss", "slap"
    )

    private val playerCooldowns = mutableMapOf<UUID, MutableMap<String, Long>>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false

        val commandName = command.name.lowercase()
        if (commandName !in handledCommands) return false
        if (!hasPermission(sender, commandName)) {
            sendPermissionMessage(sender, commandName)
            return true
        }
        if (!checkCooldown(sender, commandName)) return true

        executePlayerCommand(sender, commandName, args)
        setCooldown(sender, commandName)

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val cmdName = command.name.lowercase()
        return when (cmdName) {
            "hide" -> completeOnlinePlayers(args)
            "sithead" -> completeSithead(sender, args)
            "gravity" -> completeFromList(args, listOf("0.1","0.2","0.3","0.4","0.5","0.6","0.7","0.8","0.9","1.0","basic"))
            "resize" -> completeFromList(args, listOf("0.1","0.5","1.0","1.5","5.0","10.0","15.0","basic"))
            "strength" -> completeFromList(args, listOf("0","5","10","100","500","basic"))
            "health" -> completeFromList(args, listOf("1","10","50","100","basic"))
            "freeze" -> if (sender.hasPermission("advancedcreative.freeze.other")) completeOnlinePlayers(args) else emptyList()
            "disguise" -> completeDisguise(sender, args)
            "effects" -> completeEffects(sender, args)
            else -> emptyList()
        }
    }

    private fun hasPermission(player: Player, command: String): Boolean {
        val node = functionHooker.permissionManager.getPermissionNodeForCommand(command)
        return player.hasPermission(node)
    }

    private fun getCooldownMillis(command: String): Long {
        val key = if (command == "ahelp") "help" else command
        return functionHooker.configManager.config.getInt("cooldowns.$key", 1000).toLong()
    }

    private fun checkCooldown(player: Player, command: String): Boolean {
        val lastTimes = playerCooldowns[player.uniqueId] ?: return true
        val expiresAt = lastTimes[command] ?: return true
        val currentTime = System.currentTimeMillis()
        if (currentTime < expiresAt) {
            val remainingTime = String.format("%.1f", (expiresAt - currentTime) / 1000.0)
            functionHooker.messageManager.sendActionBar(
                player,
                MessageKey.ACTION_COOLDOWN,
                variables = mapOf("time" to remainingTime)
            )
            return false
        }
        return true
    }

    private fun setCooldown(player: Player, command: String) {
        val cooldown = getCooldownMillis(command)
        playerCooldowns.computeIfAbsent(player.uniqueId) { mutableMapOf() }[command] = System.currentTimeMillis() + cooldown
    }

    private fun sendPermissionMessage(player: Player, command: String) {
        functionHooker.permissionManager.sendPermissionDenied(player, command)
    }

    private fun executePlayerCommand(player: Player, command: String, args: Array<out String>) {
        when (command) {
            "ahelp" -> functionHooker.messageManager.sendChat(player, MessageKey.AHELP)
            "sit" -> functionHooker.sitManager.sitPlayer(player)
            "lay" -> functionHooker.layManager.layPlayer(player)
            "glide" -> functionHooker.glideManager.glidePlayer(player)
            "sneeze" -> functionHooker.sneezeManager.sneezePlayer(player)
            "crawl" -> functionHooker.crawlManager.crawlPlayer(player)
            "hide" -> functionHooker.hideManager.prepareToHidePlayer(player, args.firstOrNull())
            "gravity" -> functionHooker.gravityManager.applyEffect(player, args.firstOrNull())
            "resize" -> functionHooker.resizeManager.applyEffect(player, args.firstOrNull())
            "strength" -> functionHooker.strengthManager.applyEffect(player, args.firstOrNull())
            "health" -> functionHooker.healthManager.applyEffect(player, args.firstOrNull())
            "freeze" -> functionHooker.freezeManager.prepareToFreezePlayer(player, args.firstOrNull())
            "glow" -> functionHooker.glowManager.glowPlayer(player)
            "spit" -> functionHooker.spitManager.spitPlayer(player)
            "piss" -> functionHooker.pissManager.pissPlayer(player)
            "disguise" -> functionHooker.disguiseManager.disguisePlayer(player, args.firstOrNull(), args.getOrNull(1))
            "effects" -> functionHooker.effectsManager.applyEffect(player, args.getOrNull(0), args.getOrNull(1), args.getOrNull(2))
            "slap" -> functionHooker.slapManager.slapPlayer(player)
            "sithead" -> functionHooker.sitheadManager.prepareToSithead(player, args.getOrNull(0), args.getOrNull(1))
            "itemdb" -> functionHooker.itemdbManager.showItemInfo(player)
        }
    }

    private fun completeOnlinePlayers(args: Array<out String>): List<String> {
        return if (args.size == 1 || args.size == 2) {
            Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
        } else emptyList()
    }

    private fun completeFromList(args: Array<out String>, options: List<String>): List<String> {
        return if (args.size == 1) options.filter { it.startsWith(args[0], ignoreCase = true) } else emptyList()
    }

    private fun completeDisguise(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.isEmpty()) return emptyList()
        if (args.size == 1) {
            val blocked = functionHooker.configManager.getBlockedDisguises()
            val types = EntityType.entries.filter { it !in blocked }.map { it.name.lowercase() }.toMutableList()
            if (!sender.hasPermission("advancedcreative.disguise.full")) {
                types.removeAll(functionHooker.disguiseManager.restrictedEntities.map { it.name.lowercase() })
            }
            types.addAll(listOf("off","player"))
            return types.filter { it.startsWith(args[0], ignoreCase = true) }
        } else if (args.size == 2) {
            return listOf("-self","-noself").filter { it.startsWith(args[1], ignoreCase = true) }
        }
        return emptyList()
    }

    private fun completeEffects(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> (Registry.EFFECT.iterator().asSequence().map { it.key.key.lowercase() } + "clear")
                .filter { it.startsWith(args[0], ignoreCase = true) }.sorted().toList()
            2 -> listOf("1","2","3","5","10").filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> if (sender.hasPermission("advancedcreative.effects.other")) {
                Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
            } else emptyList()
            else -> emptyList()
        }
    }

    private fun completeSithead(sender: CommandSender, args: Array<out String>): List<String> {
        if (sender.hasPermission("advancedcreative.sithead.other")) {
            val completions = mutableListOf<String>()
            if (args.size < 2) completions.add("toggle")
            if (args.size < 3 && !args.contains("toggle")) {
                completions.addAll(completeOnlinePlayers(args))
                return completions.filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
            }
            return completions
        } else {
            if (args.size < 2) {
                return listOf("toggle")
            }
        }
        return emptyList()
    }
}