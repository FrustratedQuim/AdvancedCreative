package com.ratger.acreative.commands.sit

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

class SitheadManager(private val hooker: FunctionHooker) {

    fun prepareToSithead(sender: Player, targetName: String?, playerName: String?) {
        if (targetName == null) {
            hooker.messageManager.sendMiniMessage(sender, key = "usage-sithead")
            return
        }

        val target = Bukkit.getPlayer(targetName)
        if (target == null) {
            hooker.messageManager.sendMiniMessage(sender, key = "error-unknown-player")
            return
        }

        val playerToSit = if (playerName != null) {
            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                hooker.messageManager.sendMiniMessage(sender, key = "error-unknown-player")
                return
            }
            player
        } else {
            sender
        }

        if (playerToSit.gameMode == GameMode.SPECTATOR) return
        if (hooker.sitManager.sittingMap.containsKey(playerToSit)) return

        if (hooker.utils.isHiddenFromPlayer(target, playerToSit)) {
            val messageKey = if (playerToSit == sender) "sithead-hidden-self" else "sithead-hidden-self-target"
            val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
            hooker.messageManager.sendMiniMessage(sender, key = messageKey, variables = variables)
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
                val messageKey = if (playerToSit == sender) "sithead-hidden-by-one" else "sithead-hidden-by-one-target"
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendMiniMessage(sender, key = messageKey, variables = variables)
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
                val messageKey = if (playerToSit == sender) "sithead-hidden-by-one" else "sithead-hidden-by-one-target"
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendMiniMessage(sender, key = messageKey, variables = variables)
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
                val messageKey = if (playerToSit == sender) "sithead-you-hide-one" else "sithead-one-hidden-by-player"
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendMiniMessage(sender, key = messageKey, variables = variables)
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
                val messageKey = if (playerToSit == sender) "sithead-you-hide-one" else "sithead-one-hidden-by-player"
                val variables = if (playerToSit != sender) mapOf("player" to playerToSit.name) else null
                hooker.messageManager.sendMiniMessage(sender, key = messageKey, variables = variables)
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
}