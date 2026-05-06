package com.ratger.acreative.commands.paint

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.commands.paint.model.PaintCanvasSize
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.util.BukkitHelper

class PaintCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.PAINT) {
    override fun handle(player: Player, args: Array<out String>) {
        when (args.firstOrNull()?.lowercase()) {
            "ban" -> {
                if (!requirePermission(player)) return
                val targetName = args.getOrNull(1)
                if (targetName.isNullOrBlank()) {
                    hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
                    return
                }
                if (!hasKnownUser(player, targetName)) return
                val reason = args.drop(2).joinToString(" ").trim().takeIf { it.isNotBlank() }
                hooker.paintManager.toggleUserBan(player, targetName, reason)
            }
            "banlist" -> {
                if (!requirePermission(player)) return
                hooker.paintManager.openBannedUsers(player)
            }
            else -> hooker.paintManager.handlePaintCommand(player, args)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()
        return when (args.size) {
            1 -> {
                val options = mutableListOf<String>()
                options += PaintCanvasSize.TAB_SUGGESTIONS
                if (player.hasPermission(MODERATION_PERMISSION)) options += listOf("ban", "banlist")
                options.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            2 -> {
                if (player.hasPermission(MODERATION_PERMISSION) && args[0].equals("ban", ignoreCase = true)) {
                    Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun requirePermission(player: Player): Boolean {
        return if (player.hasPermission(MODERATION_PERMISSION)) {
            true
        } else {
            hooker.permissionManager.sendPermissionDenied(player, "paint.moderation")
            false
        }
    }

    private fun hasKnownUser(player: Player, targetName: String): Boolean {
        return if (BukkitHelper.getUser(targetName).isPresent) {
            true
        } else {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
            false
        }
    }

    private companion object {
        const val MODERATION_PERMISSION = "advancedcreative.paint.moderation"
    }
}
