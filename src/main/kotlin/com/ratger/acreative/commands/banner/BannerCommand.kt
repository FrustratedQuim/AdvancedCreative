package com.ratger.acreative.commands.banner

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BannerCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.BANNER) {
    override fun handle(player: Player, args: Array<out String>) {
        when (args.firstOrNull()?.lowercase()) {
            null -> hooker.bannerMenuService.openMainMenu(player)
            "post" -> hooker.bannerMenuService.openPostFromCommand(player)
            "ban" -> if (requireModerationPermission(player)) hooker.bannerMenuService.togglePatternBan(player)
            "banlist" -> if (requireModerationPermission(player)) hooker.bannerMenuService.openBannedPatterns(player)
            "banuser" -> {
                if (!requireModerationPermission(player)) {
                    return
                }
                val targetName = args.getOrNull(1)
                if (targetName.isNullOrBlank()) {
                    hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
                    return
                }
                val reason = args.drop(2).joinToString(" ").trim().takeIf { it.isNotBlank() }
                hooker.bannerMenuService.toggleUserBan(player, targetName, reason)
            }
            "banuserlist" -> if (requireModerationPermission(player)) hooker.bannerMenuService.openBannedUsers(player)
            else -> hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()
        return when (args.size) {
            1 -> {
                val base = mutableListOf("post")
                if (player.hasPermission(MODERATION_PERMISSION)) {
                    base += listOf("ban", "banlist", "banuser", "banuserlist")
                }
                base.filter { it.startsWith(args[0], ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    private fun requireModerationPermission(player: Player): Boolean {
        return if (player.hasPermission(MODERATION_PERMISSION)) {
            true
        } else {
            hooker.permissionManager.sendPermissionDenied(player, "banner.moderation")
            false
        }
    }

    private companion object {
        const val MODERATION_PERMISSION = "advancedcreative.banner.moderation"
    }
}
