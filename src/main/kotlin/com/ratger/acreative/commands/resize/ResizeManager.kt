package com.ratger.acreative.commands.resize

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ResizeManager(private val hooker: FunctionHooker) {

    companion object {
        private const val MIN_SCALE_VALUE = 0.1f
        private const val MAX_SCALE_VALUE = 15.0f
        private const val DEFAULT_SCALE_VALUE = 1.0f
    }

    val scaledPlayers = mutableMapOf<Player, Float>()

    fun applyEffect(player: Player, arg: String?) {
        if (arg == null) {
            if (scaledPlayers.containsKey(player) && scaledPlayers[player] != DEFAULT_SCALE_VALUE) {
                removeEffect(player)
            } else {
                hooker.messageManager.sendMiniMessage(player, key = "usage-resize")
            }
            return
        }

        val value = parseValue(arg) ?: run {
            hooker.messageManager.sendMiniMessage(player, key = "error-unknown-value")
            return
        }

        if (value == DEFAULT_SCALE_VALUE) {
            removeEffect(player)
            return
        }

        setScaleToPlayer(player, value)
        val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
        hooker.messageManager.sendMiniMessage(
            player,
            key = "success-resize-set",
            variables = mapOf("value" to df.format(value))
        )
    }

    private fun setScaleToPlayer(player: Player, value: Float) {
        hooker.utils.checkCrawlUncrawl(player)

        resetAttributes(player)

        scaledPlayers[player] = value
        player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue = value.toDouble()

        if (value >= 3.0f) {
            val interactDistance = calculateInteractDistance(value)
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.baseValue = 4.5 + interactDistance
            player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.baseValue = 3.0 + interactDistance

            val stepHeight = calculateStepHeight(value)
            player.getAttribute(Attribute.GENERIC_STEP_HEIGHT)?.baseValue = 0.6 + stepHeight

            val speed = calculateSpeed(value)
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.1 + speed
        }
    }

    fun removeEffect(player: Player) {
        scaledPlayers.remove(player)
        resetAttributes(player)
        hooker.messageManager.sendMiniMessage(player, key = "success-resize-reset")
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    private fun resetAttributes(player: Player) {
        player.getAttribute(Attribute.GENERIC_SCALE)?.baseValue = 1.0
        player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.baseValue = 4.5
        player.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE)?.baseValue = 3.0
        player.getAttribute(Attribute.GENERIC_STEP_HEIGHT)?.baseValue = 0.6
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.1
    }

    private fun parseValue(arg: String): Float? {
        if (arg.equals("basic", ignoreCase = true)) return DEFAULT_SCALE_VALUE
        if (arg.startsWith("-")) return MIN_SCALE_VALUE

        val cleanedArg = arg.replace(",", ".").trim()

        val numericStr = when {
            cleanedArg.matches(Regex("^\\d+$")) -> {
                if (cleanedArg.startsWith("0") && cleanedArg.length > 1) {
                    "0.${cleanedArg.trimStart('0')}"
                } else {
                    "$cleanedArg.0"
                }
            }
            cleanedArg.matches(Regex("^\\d*\\.\\d+$")) -> cleanedArg
            else -> return null
        }

        val value = numericStr.toFloatOrNull() ?: return null
        return when {
            value > MAX_SCALE_VALUE -> MAX_SCALE_VALUE
            value < MIN_SCALE_VALUE && value != 0.0f -> MIN_SCALE_VALUE
            value == 0.0f || value == 1.0f -> DEFAULT_SCALE_VALUE
            else -> value
        }
    }

    private fun calculateInteractDistance(scale: Float): Double {
        return if (scale >= 3.0f) {
            1.0 + (scale - 3.0f) * (25.0 - 1.0) / (15.0 - 3.0)
        } else 0.0
    }

    private fun calculateStepHeight(scale: Float): Double {
        return if (scale >= 3.0f) {
            2.0 + (scale - 3.0f) * (8.0 - 2.0) / (15.0 - 3.0)
        } else 0.0
    }

    private fun calculateSpeed(scale: Float): Double {
        return if (scale >= 3.0f) {
            0.04 + (scale - 3.0f) * (0.15 - 0.04) / (15.0 - 3.0)
        } else 0.0
    }
}