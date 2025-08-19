package com.ratger.acreative.commands.health

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.Locale

class HealthManager(private val hooker: FunctionHooker) {

    companion object {
        private const val MIN_HEALTH_VALUE = 1.0
        private const val MAX_HEALTH_VALUE = 1000.0
        private const val DEFAULT_HEALTH_VALUE = 20.0
    }

    val healthPlayers = mutableMapOf<Player, Double>()

    fun applyEffect(player: Player, arg: String?) {
        if (arg == null) {
            if (healthPlayers.containsKey(player)) {
                removeEffect(player)
            } else {
                hooker.messageManager.sendMiniMessage(player, key = "usage-health")
            }
            return
        }

        val value = parseValue(arg) ?: run {
            hooker.messageManager.sendMiniMessage(player, key = "error-unknown-value")
            return
        }

        if (value == DEFAULT_HEALTH_VALUE) {
            removeEffect(player)
            return
        }

        setHealthToPlayer(player, value)
        val formattedValue = if (((value * 100).toInt() % 10) != 0) {
            String.format(Locale.US, "%.2f", value)
        } else {
            String.format(Locale.US, "%.0f", value)
        }
        hooker.messageManager.sendMiniMessage(
            player,
            key = "success-health-set",
            variables = mapOf("value" to formattedValue)
        )
    }

    private fun setHealthToPlayer(player: Player, value: Double) {
        removeHealthAttribute(player)
        healthPlayers[player] = value
        val attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        if (attribute != null) {
            val key = NamespacedKey(hooker.plugin, "health_mod")
            val modifier = AttributeModifier(
                key,
                value - DEFAULT_HEALTH_VALUE,
                AttributeModifier.Operation.ADD_NUMBER
            )
            attribute.addModifier(modifier)
            player.health = value
        }
    }

    fun removeEffect(player: Player) {
        if (!healthPlayers.containsKey(player)) return
        removeHealthAttribute(player)
        healthPlayers.remove(player)
        player.health = DEFAULT_HEALTH_VALUE
        hooker.messageManager.sendMiniMessage(player, key = "success-health-reset")
    }

    private fun parseValue(arg: String): Double? {
        if (arg.equals("basic", ignoreCase = true)) return DEFAULT_HEALTH_VALUE
        if (arg.startsWith("-")) return DEFAULT_HEALTH_VALUE

        val cleanedArg = arg.replace(",", ".").trim()

        val numericStr = when {
            cleanedArg.matches(Regex("^\\d+$")) -> cleanedArg
            cleanedArg.matches(Regex("^\\d*\\.\\d+$")) -> cleanedArg
            else -> return null
        }

        val value = numericStr.toDoubleOrNull() ?: return null
        return when {
            value > MAX_HEALTH_VALUE -> MAX_HEALTH_VALUE
            value < MIN_HEALTH_VALUE -> DEFAULT_HEALTH_VALUE
            else -> value
        }
    }

    private fun removeHealthAttribute(player: Player) {
        val attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        if (attribute != null) {
            val modifier = attribute.modifiers.find { it.key == NamespacedKey(hooker.plugin, "health_mod") }
            if (modifier != null) {
                attribute.removeModifier(modifier)
            }
        }
    }
}