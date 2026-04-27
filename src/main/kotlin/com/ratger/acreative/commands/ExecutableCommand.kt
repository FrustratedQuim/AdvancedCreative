package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class ExecutableCommand(
    protected val hooker: FunctionHooker,
    val type: PluginCommandType,
    private val useCooldown: Boolean = true
) {
    fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: return false

        if (!hasPermission(player)) {
            hooker.permissionManager.sendPermissionDenied(player, type.id)
            return true
        }

        if (useCooldown) {
            val remainingMillis = hooker.commandManager.cooldownService.remainingMillis(player.uniqueId, type)
            if (remainingMillis > 0L) {
                val remainingSeconds = String.format("%.1f", remainingMillis / 1000.0)
                hooker.messageManager.sendActionBar(
                    player,
                    MessageKey.ACTION_COOLDOWN,
                    mapOf("time" to remainingSeconds)
                )
                return true
            }
        }

        handle(player, args)

        if (useCooldown) {
            hooker.commandManager.cooldownService.setCooldown(player.uniqueId, type)
        }

        return true
    }

    fun canUse(sender: CommandSender): Boolean {
        val player = sender as? Player ?: return false
        return hasPermission(player)
    }

    protected open fun hasPermission(player: Player): Boolean {
        val permissionNode = type.permissionNode ?: return true
        return player.hasPermission(permissionNode)
    }

    protected abstract fun handle(player: Player, args: Array<out String>)

    open fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = emptyList()
}
