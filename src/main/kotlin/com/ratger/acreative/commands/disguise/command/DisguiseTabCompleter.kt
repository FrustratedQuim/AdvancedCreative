package com.ratger.acreative.commands.disguise.command

import com.ratger.acreative.commands.disguise.DisguisePermissions
import com.ratger.acreative.commands.disguise.model.DisguiseFlags
import com.ratger.acreative.commands.disguise.model.DisguiseRequest
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType

class DisguiseTabCompleter(private val hooker: FunctionHooker) {
    fun complete(sender: CommandSender, args: Array<out String>): List<String> {
        if (args.isEmpty()) return emptyList()

        val firstArg = args.firstOrNull()
        return when {
            args.size == 1 -> completeDisguiseTypes(sender, args[0])
            firstArg.equals("player", ignoreCase = true) -> completePlayerVariant(sender, args)
            firstArg.equals(EntityType.TEXT_DISPLAY.name, ignoreCase = true) -> completeTextDisplayVariant(sender, args)
            firstArg.equals(EntityType.SLIME.name, ignoreCase = true) ||
                firstArg.equals(EntityType.MAGMA_CUBE.name, ignoreCase = true) -> completeSlimeVariant(sender, args)

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
            .any(DisguiseFlags::isKnown)

        return if (currentArg.startsWith("-") || hasReachedFlagSection) {
            completeFlags(sender, currentArg)
        } else {
            emptyList()
        }
    }

    private fun completeSlimeVariant(sender: CommandSender, args: Array<out String>): List<String> {
        val currentArg = args.last()
        return when {
            args.size == 2 && !currentArg.startsWith("-") -> DisguiseRequest.SLIME_SIZE_RANGE
                .map(Int::toString)
                .filter { it.startsWith(currentArg, ignoreCase = true) }

            else -> completeFlags(sender, currentArg)
        }
    }

    private fun completeFlags(sender: CommandSender, currentArg: String): List<String> {
        val availableFlags = buildList {
            add("-self")
            add("-noself")
            if (sender.hasPermission(DisguisePermissions.NICK)) {
                add("-withnick")
                add("-nonick")
            }
        }

        return availableFlags
            .distinct()
            .filter { it.startsWith(currentArg, ignoreCase = true) }
    }
}
