package com.ratger.acreative.commands

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.entity.Player
import java.util.Locale

abstract class NumericAttributeManager(protected val hooker: FunctionHooker) {

    protected abstract val minValue: Double
    protected abstract val maxValue: Double
    protected abstract val defaultValue: Double
    protected abstract val usageMessageKey: MessageKey
    protected abstract val successSetMessageKey: MessageKey
    protected abstract val successResetMessageKey: MessageKey
    protected abstract val trackedPlayers: MutableMap<Player, Double>
    protected open val playerStateType: PlayerStateType? = null

    fun applyEffect(player: Player, arg: String?) {
        if (arg == null) {
            if (trackedPlayers.containsKey(player)) {
                removeEffect(player)
            } else {
                hooker.messageManager.sendChat(player, usageMessageKey)
            }
            return
        }

        val value = parseValue(arg) ?: run {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
            return
        }

        if (value == defaultValue) {
            removeEffect(player)
            return
        }

        applyAttribute(player, value)
        trackedPlayers[player] = value
        playerStateType?.let { hooker.playerStateManager.activateState(player, it) }

        hooker.messageManager.sendChat(
            player,
            successSetMessageKey,
            variables = mapOf("value" to formatValue(value))
        )
    }

    fun removeEffect(player: Player) {
        if (!trackedPlayers.containsKey(player)) return

        removeAttribute(player)
        trackedPlayers.remove(player)
        playerStateType?.let { hooker.playerStateManager.deactivateState(player, it) }
        onAfterEffectRemoved(player)
        hooker.messageManager.sendChat(player, successResetMessageKey)
    }

    protected abstract fun applyAttribute(player: Player, value: Double)

    protected abstract fun removeAttribute(player: Player)

    protected open fun onAfterEffectRemoved(player: Player) = Unit

    protected open fun normalizeNegativeInput(): Double = defaultValue

    protected open fun normalizeParsedValue(value: Double): Double = when {
        value > maxValue -> maxValue
        value < minValue -> defaultValue
        else -> value
    }

    private fun parseValue(arg: String): Double? {
        if (arg.equals("basic", ignoreCase = true)) return defaultValue
        if (arg.startsWith("-")) return normalizeNegativeInput()

        val cleanedArg = arg.replace(",", ".").trim()
        val numericStr = when {
            cleanedArg.matches(Regex("^\\d+$")) -> cleanedArg
            cleanedArg.matches(Regex("^\\d*\\.\\d+$")) -> cleanedArg
            else -> return null
        }

        val value = numericStr.toDoubleOrNull() ?: return null
        return normalizeParsedValue(value)
    }

    private fun formatValue(value: Double): String {
        return if (((value * 100).toInt() % 10) != 0) {
            String.format(Locale.US, "%.2f", value)
        } else {
            String.format(Locale.US, "%.0f", value)
        }
    }
}
