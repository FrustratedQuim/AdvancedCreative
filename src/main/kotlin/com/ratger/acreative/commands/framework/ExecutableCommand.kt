package com.ratger.acreative.commands.framework

import com.ratger.acreative.core.FunctionHooker
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
                    com.ratger.acreative.core.MessageKey.ACTION_COOLDOWN,
                    variables = mapOf("time" to remainingSeconds)
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

    protected open fun hasPermission(player: Player): Boolean {
        val permissionNode = hooker.permissionManager.getPermissionNodeForCommand(type.id)
        return player.hasPermission(permissionNode)
    }

    protected abstract fun handle(player: Player, args: Array<out String>)

    open fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> = emptyList()
}
