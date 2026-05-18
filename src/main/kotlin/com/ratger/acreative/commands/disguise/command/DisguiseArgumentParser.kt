package com.ratger.acreative.commands.disguise.command

import com.ratger.acreative.commands.disguise.model.DisguiseFlags
import com.ratger.acreative.commands.disguise.model.DisguiseRequest
import org.bukkit.entity.EntityType

class DisguiseArgumentParser {
    fun parse(args: Array<out String>): DisguiseRequest {
        val type = args.firstOrNull()
        val remaining = args.drop(1)

        return when {
            type.equals("player", ignoreCase = true) -> parsePlayerArguments(type, remaining)
            type.equals(EntityType.TEXT_DISPLAY.name, ignoreCase = true) -> parseTextDisplayArguments(type, remaining)
            type.equals(EntityType.SLIME.name, ignoreCase = true) ||
                type.equals(EntityType.MAGMA_CUBE.name, ignoreCase = true) -> parseSizedSlimeArguments(type, remaining)

            else -> DisguiseRequest(
                type = type,
                flags = remaining.filter(DisguiseFlags::isKnown)
            )
        }
    }

    private fun parsePlayerArguments(type: String?, remaining: List<String>): DisguiseRequest {
        return DisguiseRequest(
            type = type,
            playerName = remaining.firstOrNull { !it.startsWith("-") },
            flags = remaining.filter(DisguiseFlags::isKnown)
        )
    }

    private fun parseSizedSlimeArguments(type: String?, remaining: List<String>): DisguiseRequest {
        val size = remaining.firstOrNull()
            ?.takeUnless { it.startsWith("-") }
            ?.toIntOrNull()
            ?.coerceIn(DisguiseRequest.SLIME_SIZE_RANGE.first, DisguiseRequest.SLIME_SIZE_RANGE.last)

        val flagTokens = if (size != null) {
            remaining.drop(1)
        } else {
            remaining
        }

        return DisguiseRequest(
            type = type,
            flags = flagTokens.filter(DisguiseFlags::isKnown),
            slimeSize = size
        )
    }

    private fun parseTextDisplayArguments(type: String?, remaining: List<String>): DisguiseRequest {
        val textParts = mutableListOf<String>()
        val flags = mutableListOf<String>()
        var hasReachedFlagSection = false

        remaining.forEach { arg ->
            if (DisguiseFlags.isKnown(arg)) {
                flags += arg
                hasReachedFlagSection = true
                return@forEach
            }

            if (!hasReachedFlagSection) {
                textParts += arg
            }
        }

        return DisguiseRequest(
            type = type,
            flags = flags,
            textDisplayText = textParts.joinToString(" ")
        )
    }
}
