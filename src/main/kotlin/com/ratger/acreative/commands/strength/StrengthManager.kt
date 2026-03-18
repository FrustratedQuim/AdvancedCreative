package com.ratger.acreative.commands.strength

import com.ratger.acreative.commands.NumericAttributeManager
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

class StrengthManager(hooker: FunctionHooker) : NumericAttributeManager(hooker) {

    companion object {
        private const val MIN_STRENGTH_VALUE = 0.0
        private const val MAX_STRENGTH_VALUE = 1000.0
        private const val DEFAULT_STRENGTH_VALUE = 0.0
    }

    override val minValue: Double = MIN_STRENGTH_VALUE
    override val maxValue: Double = MAX_STRENGTH_VALUE
    override val defaultValue: Double = DEFAULT_STRENGTH_VALUE
    override val usageMessageKey: MessageKey = MessageKey.USAGE_STRENGTH
    override val successSetMessageKey: MessageKey = MessageKey.SUCCESS_STRENGTH_SET
    override val successResetMessageKey: MessageKey = MessageKey.SUCCESS_STRENGTH_RESET
    override val trackedPlayers: MutableMap<Player, Double> = mutableMapOf()
    val strengthPlayers: MutableMap<Player, Double> get() = trackedPlayers
    override val playerStateType: PlayerStateType = PlayerStateType.CUSTOM_DAMAGE

    override fun normalizeParsedValue(value: Double): Double {
        return when {
            value > maxValue -> maxValue
            value < minValue -> defaultValue
            value == 0.0 -> defaultValue
            else -> value
        }
    }

    override fun applyAttribute(player: Player, value: Double) {
        removeAttribute(player)

        player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.addModifier(
            AttributeModifier(
                NamespacedKey(hooker.plugin, "strength_mod"),
                value,
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
    }

    override fun removeAttribute(player: Player) {
        val modifierKey = NamespacedKey(hooker.plugin, "strength_mod")
        player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)
            ?.modifiers
            ?.find { it.key == modifierKey }
            ?.let { player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.removeModifier(it) }
    }

}
