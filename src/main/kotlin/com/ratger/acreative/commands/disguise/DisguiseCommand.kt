package com.ratger.acreative.commands.disguise

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class DisguiseCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.DISGUISE) {
    private companion object {
        val KNOWN_FLAGS = setOf("-self", "-noself", "-withnick", "-nonick")
    }

    private data class ParsedDisguiseArguments(
        val type: String?,
        val playerName: String? = null,
        val flags: List<String> = emptyList(),
        val textDisplayText: String? = null
    )

    override fun handle(player: Player, args: Array<out String>) {
        val parsed = parseArguments(args)
        hooker.disguiseManager.disguisePlayer(
            player = player,
            type = parsed.type,
            playerName = parsed.playerName,
            flags = parsed.flags,
            textDisplayRaw = parsed.textDisplayText
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val firstArg = args.firstOrNull()
        return when {
            args.size == 1 -> completeDisguiseTypes(sender, args[0])
            firstArg.equals("player", ignoreCase = true) -> completePlayerVariant(sender, args)
            firstArg.equals(EntityType.TEXT_DISPLAY.name, ignoreCase = true) -> completeTextDisplayVariant(sender, args)
            else -> completeFlags(sender, args.last())
        }
    }

    private fun completeDisguiseTypes(sender: CommandSender, currentArg: String): List<String> {
        val allowedTypes = EntityType.entries
            .asSequence()
            .filter { it != EntityType.UNKNOWN }
            .filter { it != EntityType.PLAYER }
            .filter { !hooker.disguiseManager.isBlockedDisguiseType(it) }
            .filter { it != EntityType.TEXT_DISPLAY || hooker.disguiseManager.canUseTextDisguise(sender) }
            .filter {
                sender.hasPermission(DisguisePermissions.EXTENDED) ||
                    it !in hooker.disguiseManager.donationRestrictedEntities
            }
            .map { it.name.lowercase() }
            .toMutableList()

        allowedTypes += "off"
        if (sender.hasPermission(DisguisePermissions.PLAYER)) {
            allowedTypes += "player"
        }

        return allowedTypes
            .distinct()
            .filter { it.startsWith(currentArg, ignoreCase = true) }
    }

    private fun completePlayerVariant(sender: CommandSender, args: Array<out String>): List<String> {
        if (!sender.hasPermission(DisguisePermissions.PLAYER)) return emptyList()

        return when (args.size) {
            2 -> Bukkit.getOnlinePlayers()
                .asSequence()
                .map { it.name }
                .filter { it.startsWith(args[1], ignoreCase = true) }
                .sorted()
                .toList()

            else -> completeFlags(sender, args.last())
        }
    }

    private fun completeTextDisplayVariant(sender: CommandSender, args: Array<out String>): List<String> {
        if (!hooker.disguiseManager.canUseTextDisguise(sender) ||
            hooker.disguiseManager.isBlockedDisguiseType(EntityType.TEXT_DISPLAY)) {
            return emptyList()
        }

        val currentArg = args.last()
        val hasReachedFlagSection = args
            .drop(1)
            .dropLast(1)
            .any(::isKnownFlag)

        return if (currentArg.startsWith("-") || hasReachedFlagSection) {
            completeFlags(sender, currentArg)
        } else {
            emptyList()
        }
    }

    private fun CommandSender.hasNickDisguisePermission(): Boolean {
        return hasPermission(DisguisePermissions.NICK)
    }

    private fun completeFlags(sender: CommandSender, currentArg: String): List<String> {
        val availableFlags = buildList {
            add("-self")
            add("-noself")
            if (sender.hasNickDisguisePermission()) {
                add("-withnick")
                add("-nonick")
            }
        }

        return availableFlags
            .distinct()
            .filter { it.startsWith(currentArg, ignoreCase = true) }
    }

    private fun parseArguments(args: Array<out String>): ParsedDisguiseArguments {
        val type = args.firstOrNull()
        val remaining = args.drop(1)

        return when {
            type.equals("player", ignoreCase = true) -> ParsedDisguiseArguments(
                type = type,
                playerName = remaining.firstOrNull { !it.startsWith("-") },
                flags = remaining.filter(::isKnownFlag)
            )

            type.equals(EntityType.TEXT_DISPLAY.name, ignoreCase = true) -> parseTextDisplayArguments(type, remaining)

            else -> ParsedDisguiseArguments(
                type = type,
                flags = remaining.filter(::isKnownFlag)
            )
        }
    }

    private fun parseTextDisplayArguments(type: String?, remaining: List<String>): ParsedDisguiseArguments {
        val textParts = mutableListOf<String>()
        val flags = mutableListOf<String>()
        var hasReachedFlagSection = false

        remaining.forEach { arg ->
            if (isKnownFlag(arg)) {
                flags += arg
                hasReachedFlagSection = true
                return@forEach
            }

            if (!hasReachedFlagSection) {
                textParts += arg
            }
        }

        return ParsedDisguiseArguments(
            type = type,
            flags = flags,
            textDisplayText = textParts.joinToString(" ")
        )
    }

    private fun isKnownFlag(value: String): Boolean {
        return value.lowercase() in KNOWN_FLAGS
    }
}
