package com.ratger.acreative.commands.strength

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.Locale

class StrengthManager(private val hooker: FunctionHooker) {

    companion object {
        private const val MIN_STRENGTH_VALUE = 0.0
        private const val MAX_STRENGTH_VALUE = 1000.0
        private const val DEFAULT_STRENGTH_VALUE = 0.0
    }

    val strengthPlayers = mutableMapOf<Player, Double>()

    fun applyEffect(player: Player, arg: String?) {
        if (arg == null) {
            if (strengthPlayers.containsKey(player)) {
                removeEffect(player)
            } else {
                hooker.messageManager.sendMiniMessage(player, key = "usage-strength")
            }
            return
        }

        val value = parseValue(arg) ?: run {
            hooker.messageManager.sendMiniMessage(player, key = "error-unknown-value")
            return
        }

        if (value == DEFAULT_STRENGTH_VALUE) {
            removeEffect(player)
            return
        }

        setStrengthToPlayer(player, value)
        val formattedValue = if (((value * 100).toInt() % 10) != 0) {
            String.format(Locale.US, "%.2f", value)
        } else {
            String.format(Locale.US, "%.0f", value)
        }
        hooker.messageManager.sendMiniMessage(
            player,
            key = "success-strength-set",
            variables = mapOf("value" to formattedValue)
        )
    }

    private fun setStrengthToPlayer(player: Player, value: Double) {
        removeStrengthAttribute(player)
        strengthPlayers[player] = value
        val attribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)
        if (attribute != null) {
            val key = NamespacedKey(hooker.plugin, "strength_mod")
            val modifier = AttributeModifier(
                key,
                value,
                AttributeModifier.Operation.ADD_NUMBER
            )
            attribute.addModifier(modifier)
        }
    }

    fun removeEffect(player: Player) {
        if (!strengthPlayers.containsKey(player)) return
        removeStrengthAttribute(player)
        strengthPlayers.remove(player)
        hooker.messageManager.sendMiniMessage(player, key = "success-strength-reset")
    }

    private fun parseValue(arg: String): Double? {
        if (arg.equals("basic", ignoreCase = true)) return DEFAULT_STRENGTH_VALUE
        if (arg.startsWith("-")) return DEFAULT_STRENGTH_VALUE

        val cleanedArg = arg.replace(",", ".").trim()

        val numericStr = when {
            cleanedArg.matches(Regex("^\\d+$")) -> cleanedArg
            cleanedArg.matches(Regex("^\\d*\\.\\d+$")) -> cleanedArg
            else -> return null
        }

        val value = numericStr.toDoubleOrNull() ?: return null
        return when {
            value > MAX_STRENGTH_VALUE -> MAX_STRENGTH_VALUE
            value < MIN_STRENGTH_VALUE -> DEFAULT_STRENGTH_VALUE
            value == 0.0 -> DEFAULT_STRENGTH_VALUE
            else -> value
        }
    }

    private fun removeStrengthAttribute(player: Player) {
        val attribute = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)
        if (attribute != null) {
            val modifier = attribute.modifiers.find { it.key == NamespacedKey(hooker.plugin, "strength_mod") }
            if (modifier != null) {
                attribute.removeModifier(modifier)
            }
        }
    }
}