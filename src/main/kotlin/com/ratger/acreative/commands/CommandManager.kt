package com.ratger.acreative.commands

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

    private val commandPermissions = mapOf(
        "ahelp" to "player",
        "sit" to "player",
        "lay" to "player",
        "crawl" to "player",
        "hide" to "player",
        "strength" to "player",
        "health" to "player",
        "effects" to "player",
        "sneeze" to "rise",
        "glide" to "rise",
        "gravity" to "rise",
        "resize" to "flare",
        "freeze" to "flare",
        "bind" to "flare",
        "glow" to "spark",
        "disguise" to "spark",
        "spit" to "horizon",
        "piss" to "horizon",
        "slap" to "admin"
    )

    private val permissionMessages = mapOf(
        "player" to "permission-unknown",
        "rise" to "permission-rise",
        "flare" to "permission-flare",
        "shine" to "permission-shine",
        "spark" to "permission-spark",
        "sunny" to "permission-sunny",
        "horizon" to "permission-horizon",
        "admin" to "permission-unknown"
    )

    private val playerCooldowns = mutableMapOf<UUID, MutableMap<String, Long>>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false

        val commandName = command.name.lowercase()
        if (commandName !in commandPermissions) return false
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
            "gravity" -> completeFromList(args, listOf("0.1","0.2","0.3","0.4","0.5","0.6","0.7","0.8","0.9","1.0","basic"))
            "resize" -> completeFromList(args, listOf("0.1","0.5","1.0","1.5","5.0","10.0","15.0","basic"))
            "strength" -> completeFromList(args, listOf("0","5","10","100","500","basic"))
            "health" -> completeFromList(args, listOf("1","10","50","100","basic"))
            "freeze" -> if (sender.hasPermission("advancedcreative.freeze.other")) completeOnlinePlayers(args) else emptyList()
            "bind" -> if (args.size == 1) completeFromList(args, listOf("set","reset","resetall")) else emptyList()
            "disguise" -> completeDisguise(sender, args)
            "effects" -> completeEffects(sender, args)
            else -> emptyList()
        }
    }

    private fun hasPermission(player: Player, command: String) = player.hasPermission("advancedcreative.$command")

    private fun checkCooldown(player: Player, command: String): Boolean {
        val lastTimes = playerCooldowns[player.uniqueId] ?: return true
        val lastTime = lastTimes[command] ?: return true
        val now = System.currentTimeMillis()
        val cooldown = functionHooker.configManager.config.getLong("cooldowns.$command", 0L)
        val remaining = cooldown - (now - lastTime)
        if (remaining > 0) {
            val formatted = String.format(Locale.US, "%.1f", remaining / 1000.0)
            functionHooker.messageManager.sendMiniMessage(player, type = "ACTION", key = "action-cooldown", variables = mapOf("time" to "$formatted с."))
            return false
        }
        return true
    }

    private fun setCooldown(player: Player, command: String) {
        val now = System.currentTimeMillis()
        val lastTimes = playerCooldowns.getOrPut(player.uniqueId) { mutableMapOf() }
        lastTimes[command] = now
    }

    private fun sendPermissionMessage(player: Player, command: String, notify: Boolean = true) {
        if (!notify) return
        val permGroup = commandPermissions[command] ?: return
        val msgKey = permissionMessages[permGroup] ?: "permission-unknown"
        functionHooker.messageManager.sendMiniMessage(player, key = msgKey)
    }

    private fun executePlayerCommand(player: Player, command: String, args: Array<out String>) {
        when (command) {
            "ahelp" -> functionHooker.messageManager.sendMiniMessage(player, key = "ahelp")
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
            "bind" -> functionHooker.bindManager.prepareToBindCommand(player, args.firstOrNull(), args.drop(1).toTypedArray())
            "glow" -> functionHooker.glowManager.glowPlayer(player)
            "spit" -> functionHooker.spitManager.spitPlayer(player)
            "piss" -> functionHooker.pissManager.pissPlayer(player)
            "disguise" -> functionHooker.disguiseManager.disguisePlayer(player, args.firstOrNull(), args.getOrNull(1))
            "effects" -> functionHooker.effectsManager.applyEffect(player, args.getOrNull(0), args.getOrNull(1), args.getOrNull(2))
            "slap" -> functionHooker.slapManager.slapPlayer(player)
        }
    }

    private fun completeOnlinePlayers(args: Array<out String>): List<String> {
        return if (args.size == 1) {
            Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
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
        return when {
            args.size == 1 -> (Registry.EFFECT.iterator().asSequence().map { it.key.key.lowercase() } + "clear")
                .filter { it.startsWith(args[0], ignoreCase = true) }.sorted().toList()
            args.size == 2 -> listOf("1","2","3","5","10").filter { it.startsWith(args[1], ignoreCase = true) }
            args.size == 3 && sender.hasPermission("advancedcreative.effects.admin") ->
                Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
