package com.ratger.acreative.commands.banner

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DecorationBannersCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DECORATIONBANNERS) {
    override fun handle(player: Player, args: Array<out String>) {
        val moderationMode = args.any { it.equals("-m", ignoreCase = true) }
        if (moderationMode && !player.hasPermission(MODERATION_PERMISSION)) {
            hooker.permissionManager.sendPermissionDenied(player, "decorationbanners.moderation")
            return
        }

        val authorName = args.firstOrNull { !it.equals("-m", ignoreCase = true) }
        if (authorName.isNullOrBlank()) {
            hooker.bannerMenuService.openPublicGalleryFromCommand(player, moderationMode)
            return
        }

        hooker.bannerMenuService.openPublicGalleryForAuthor(player, authorName, moderationMode)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player ?: return emptyList()
        return when (args.size) {
            1 -> {
                val input = args[0]
                val suggestions = if (input.equals("-m", ignoreCase = true)) {
                    mutableListOf<String>()
                } else {
                    (hooker.bannerMenuService.authorSuggestions(input) + onlineNameSuggestions(input)).toMutableList()
                }
                if (player.hasPermission(MODERATION_PERMISSION) && "-m".startsWith(input, ignoreCase = true)) {
                    suggestions += "-m"
                }
                suggestions.distinct()
            }
            2 -> {
                if (args[0].equals("-m", ignoreCase = true)) {
                    (hooker.bannerMenuService.authorSuggestions(args[1]) + onlineNameSuggestions(args[1])).distinct()
                } else if (player.hasPermission(MODERATION_PERMISSION)) {
                    listOf("-m").filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private companion object {
        const val MODERATION_PERMISSION = "advancedcreative.decorationbanners.moderation"
    }

    private fun onlineNameSuggestions(prefix: String): List<String> {
        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
