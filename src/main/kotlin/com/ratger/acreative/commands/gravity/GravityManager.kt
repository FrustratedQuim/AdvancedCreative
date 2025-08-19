package com.ratger.acreative.commands.gravity

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class GravityManager(private val hooker: FunctionHooker) {

    companion object {
        private const val MIN_GRAVITY_VALUE = 0.1
        private const val MAX_GRAVITY_VALUE = 1.0
        private const val DEFAULT_GRAVITY_VALUE = 1.0
        private const val MAX_GRAVITY_MODIFIER = -0.075
        private const val MIN_GRAVITY_MODIFIER = 0.0
    }

    val gravityPlayers = mutableMapOf<Player, Double>()

    fun applyEffect(player: Player, arg: String?) {
        if (arg == null) {
            hooker.messageManager.sendMiniMessage(player, key = "usage-gravity")
            return
        }

        val value = parseValue(arg) ?: run {
            hooker.messageManager.sendMiniMessage(player, key = "error-unknown-value")
            return
        }

        if (value == DEFAULT_GRAVITY_VALUE) {
            removeEffect(player)
            return
        }

        setGravityToPlayer(player, value)
        val formattedValue = if (((value * 100).toInt() % 10) != 0) {
            String.format(Locale.US, "%.2f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        hooker.messageManager.sendMiniMessage(
            player,
            key = "success-gravity-set",
            variables = mapOf("value" to formattedValue)
        )
    }

    private fun setGravityToPlayer(player: Player, value: Double) {
        removeGravityAttribute(player)
        gravityPlayers[player] = value
        val modifierValue = calculateModifier(value)
        val attribute = player.getAttribute(Attribute.GENERIC_GRAVITY)
        if (attribute != null) {
            val key = NamespacedKey(hooker.plugin, "gravity_mod")
            val modifier = AttributeModifier(
                key,
                modifierValue,
                AttributeModifier.Operation.ADD_NUMBER
            )
            attribute.addModifier(modifier)
        }
    }

    fun removeEffect(player: Player) {
        if (!gravityPlayers.containsKey(player)) return
        removeGravityAttribute(player)
        gravityPlayers.remove(player)
        hooker.messageManager.sendMiniMessage(player, key = "success-gravity-reset")
    }

    private fun parseValue(arg: String): Double? {
        if (arg.equals("basic", ignoreCase = true)) return DEFAULT_GRAVITY_VALUE
        if (arg.startsWith("-")) return MIN_GRAVITY_VALUE

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

        val value = numericStr.toDoubleOrNull() ?: return null
        return when {
            value > MAX_GRAVITY_VALUE -> MAX_GRAVITY_VALUE
            value < MIN_GRAVITY_VALUE && value != 0.0 -> MIN_GRAVITY_VALUE
            value == 0.0 || value == 1.0 -> DEFAULT_GRAVITY_VALUE
            else -> value
        }
    }

    private fun calculateModifier(value: Double): Double {
        val clampedValue = min(MAX_GRAVITY_VALUE, max(MIN_GRAVITY_VALUE, value))
        val normalized = (clampedValue - MIN_GRAVITY_VALUE) / (MAX_GRAVITY_VALUE - MIN_GRAVITY_VALUE)
        return MAX_GRAVITY_MODIFIER + (MIN_GRAVITY_MODIFIER - MAX_GRAVITY_MODIFIER) * normalized
    }

    private fun removeGravityAttribute(player: Player) {
        val attribute = player.getAttribute(Attribute.GENERIC_GRAVITY)
        if (attribute != null) {
            val modifier = attribute.modifiers.find { it.key == NamespacedKey(hooker.plugin, "gravity_mod") }
            if (modifier != null) {
                attribute.removeModifier(modifier)
            }
        }
    }
}