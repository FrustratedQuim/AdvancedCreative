package com.ratger.acreative.commands.sit

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class SitheadManager(private val hooker: FunctionHooker) {

    private val blockInteractPlayers = mutableSetOf<UUID>()

    fun prepareToSithead(sender: Player, targetName: String?, playerName: String?) {
        if (targetName == null) {
            if (sender.hasPermission("advancedcreative.sithead.other")) {
                hooker.messageManager.sendChat(sender, MessageKey.USAGE_SITHEAD_OTHER)
            } else {
                hooker.messageManager.sendChat(sender, MessageKey.USAGE_SITHEAD)
            }
            return
        }

        if (targetName == "toggle" && sender.hasPermission("advancedcreative.sithead")) {
            if (blockInteractPlayers.contains(sender.uniqueId)) {
                blockInteractPlayers.remove(sender.uniqueId)
                hooker.messageManager.sendChat(sender, MessageKey.SITHEAD_UNBLOCK_INTERACT)
            } else {
                blockInteractPlayers.add(sender.uniqueId)
                hooker.messageManager.sendChat(sender, MessageKey.SITHEAD_BLOCK_INTERACT)
            }
            return
        }

        if (!sender.hasPermission("advancedcreative.sithead.other")) {
            hooker.messageManager.sendChat(sender, MessageKey.PERMISSION_UNKNOWN)
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendChat(sender, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        val playerToSit = if (playerName != null) {
            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                hooker.messageManager.sendChat(sender, MessageKey.ERROR_UNKNOWN_PLAYER)
                return
            }
            player
        } else {
            sender
        }

        if (playerToSit.gameMode == GameMode.SPECTATOR) return
        if (hooker.sitManager.sittingMap.containsKey(playerToSit)) return

        if (hooker.utils.isHiddenFromPlayer(target, playerToSit)) {
            val messageKey = if (playerToSit == sender) MessageKey.SITHEAD_HIDDEN_SELF else MessageKey.SITHEAD_HIDDEN_SELF_TARGET
            val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
            hooker.messageManager.sendChat(sender, messageKey, variables ?: emptyMap())
            return
        }

        var currentTarget: Player? = target
        val checkedPlayers = mutableSetOf<Player>()
        val maxDepth = 10
        var depth = 0
        var finalTarget = target
        while (currentTarget != null && depth < maxDepth) {
            if (currentTarget in checkedPlayers) return
            checkedPlayers.add(currentTarget)
            if (hooker.utils.isHiddenFromPlayer(currentTarget, playerToSit)) {
                val messageKey = if (playerToSit == sender) MessageKey.SITHEAD_HIDDEN_BY_ONE else MessageKey.SITHEAD_HIDDEN_BY_ONE_TARGET
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendChat(sender, messageKey, variables ?: emptyMap())
                return
            }

            if (currentTarget == playerToSit) return
            finalTarget = currentTarget
            currentTarget = hooker.sitManager.getHeadPassenger(currentTarget)
            depth++
        }

        var baseTarget: Player? = finalTarget
        val baseCheckedPlayers = mutableSetOf<Player>()
        depth = 0
        do {
            if (baseTarget == null) break
            if (baseTarget in baseCheckedPlayers) return
            baseCheckedPlayers.add(baseTarget)
            if (baseTarget == playerToSit) return
            if (hooker.utils.isHiddenFromPlayer(baseTarget, playerToSit)) {
                val messageKey = if (playerToSit == sender) MessageKey.SITHEAD_HIDDEN_BY_ONE else MessageKey.SITHEAD_HIDDEN_BY_ONE_TARGET
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendChat(sender, messageKey, variables ?: emptyMap())
                return
            }

            val sitData = hooker.sitManager.sittingMap[baseTarget]
            if (sitData == null || sitData.style != "head") break
            val baseStand = baseTarget.world.getEntity(sitData.armorStandId) ?: break
            baseTarget = baseStand.vehicle as? Player ?: break
            depth++
            if (depth >= maxDepth) break
        } while (true)

        currentTarget = target
        checkedPlayers.clear()
        depth = 0
        while (currentTarget != null && depth < maxDepth) {
            if (currentTarget in checkedPlayers) return
            checkedPlayers.add(currentTarget)
            if (hooker.utils.isHiddenFromPlayer(playerToSit, currentTarget)) {
                val messageKey = if (playerToSit == sender) MessageKey.SITHEAD_YOU_HIDE_ONE else MessageKey.SITHEAD_ONE_HIDDEN_BY_PLAYER
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendChat(sender, messageKey, variables ?: emptyMap())
                return
            }
            currentTarget = hooker.sitManager.getHeadPassenger(currentTarget)
            depth++
        }

        baseTarget = finalTarget
        checkedPlayers.clear()
        depth = 0
        while (baseTarget != null && depth < maxDepth) {
            if (baseTarget in checkedPlayers) return
            checkedPlayers.add(baseTarget)
            if (hooker.utils.isHiddenFromPlayer(playerToSit, baseTarget)) {
                val messageKey = if (playerToSit == sender) MessageKey.SITHEAD_YOU_HIDE_ONE else MessageKey.SITHEAD_ONE_HIDDEN_BY_PLAYER
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendChat(sender, messageKey, variables ?: emptyMap())
                return
            }
            val sitData = hooker.sitManager.sittingMap[baseTarget]
            if (sitData == null || sitData.style != "head") {
                break
            }
            val baseStand = baseTarget.world.getEntity(sitData.armorStandId) ?: break
            baseTarget = baseStand.vehicle as? Player ?: break
            depth++
        }

        hooker.utils.unsetAllPoses(playerToSit)
        hooker.sitManager.sitOnHead(playerToSit, finalTarget, sender)
    }

    fun isInteractionBlocked(player: Player): Boolean {
        return blockInteractPlayers.contains(player.uniqueId)
    }
}

class SitheadCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.SITHEAD) {

    override fun handle(player: Player, args: Array<out String>) {
        hooker.sitheadManager.prepareToSithead(player, args.getOrNull(0), args.getOrNull(1))
    }

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

        return if (args.size < 2) listOf("toggle") else emptyList()
    }

    private fun completeOnlinePlayers(args: Array<out String>): List<String> {
        return if (args.size == 1 || args.size == 2) {
            Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.startsWith(args[args.size - 1], ignoreCase = true) }
        } else {
            emptyList()
        }
    }
}
