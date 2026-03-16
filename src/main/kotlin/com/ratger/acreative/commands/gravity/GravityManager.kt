package com.ratger.acreative.commands.gravity

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.NumericAttributeManager
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.math.min

class GravityManager(hooker: FunctionHooker) : NumericAttributeManager(hooker) {

    companion object {
        private const val MIN_GRAVITY_VALUE = 0.1
        private const val MAX_GRAVITY_VALUE = 1.0
        private const val DEFAULT_GRAVITY_VALUE = 1.0
        private const val MAX_GRAVITY_MODIFIER = -0.075
        private const val MIN_GRAVITY_MODIFIER = 0.0
        private val GRAVITY_SUGGESTIONS = listOf("0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0", "basic")
    }

    override val minValue: Double = MIN_GRAVITY_VALUE
    override val maxValue: Double = MAX_GRAVITY_VALUE
    override val defaultValue: Double = DEFAULT_GRAVITY_VALUE
    override val usageMessageKey: MessageKey = MessageKey.USAGE_GRAVITY
    override val successSetMessageKey: MessageKey = MessageKey.SUCCESS_GRAVITY_SET
    override val successResetMessageKey: MessageKey = MessageKey.SUCCESS_GRAVITY_RESET
    override val trackedPlayers: MutableMap<Player, Double> = mutableMapOf()
    val gravityPlayers: MutableMap<Player, Double> get() = trackedPlayers

    override fun applyAttribute(player: Player, value: Double) {
        removeAttribute(player)
        val modifierValue = calculateModifier(value)
        player.getAttribute(Attribute.GENERIC_GRAVITY)?.addModifier(
            AttributeModifier(
                NamespacedKey(hooker.plugin, "gravity_mod"),
                modifierValue,
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
    }

    override fun removeAttribute(player: Player) {
        val modifierKey = NamespacedKey(hooker.plugin, "gravity_mod")
        player.getAttribute(Attribute.GENERIC_GRAVITY)
            ?.modifiers
            ?.find { it.key == modifierKey }
            ?.let { player.getAttribute(Attribute.GENERIC_GRAVITY)?.removeModifier(it) }
    }

    override fun normalizeParsedValue(value: Double): Double {
        return when {
            value > maxValue -> maxValue
            value < minValue && value != 0.0 -> minValue
            value == 0.0 || value == defaultValue -> defaultValue
            else -> value
        }
    }

    override fun normalizeNegativeInput(): Double = minValue

    fun tabCompletions(args: Array<out String>): List<String> {
        return if (args.size == 1) {
            GRAVITY_SUGGESTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    private fun calculateModifier(value: Double): Double {
        val clampedValue = min(MAX_GRAVITY_VALUE, max(MIN_GRAVITY_VALUE, value))
        val normalized = (clampedValue - MIN_GRAVITY_VALUE) / (MAX_GRAVITY_VALUE - MIN_GRAVITY_VALUE)
        return MAX_GRAVITY_MODIFIER + (MIN_GRAVITY_MODIFIER - MAX_GRAVITY_MODIFIER) * normalized
    }

}

class GravityCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GRAVITY) {
    override fun handle(player: Player, args: Array<out String>) = hooker.gravityManager.applyEffect(player, args.firstOrNull())

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return hooker.gravityManager.tabCompletions(args)
    }
}
