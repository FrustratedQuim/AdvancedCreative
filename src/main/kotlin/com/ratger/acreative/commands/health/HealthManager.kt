package com.ratger.acreative.commands.health

import com.ratger.acreative.commands.common.NumericAttributeManager
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

class HealthManager(hooker: FunctionHooker) : NumericAttributeManager(hooker) {

    companion object {
        private const val MIN_HEALTH_VALUE = 1.0
        private const val MAX_HEALTH_VALUE = 1000.0
        private const val DEFAULT_HEALTH_VALUE = 20.0
    }

    override val minValue: Double = MIN_HEALTH_VALUE
    override val maxValue: Double = MAX_HEALTH_VALUE
    override val defaultValue: Double = DEFAULT_HEALTH_VALUE
    override val usageMessageKey: MessageKey = MessageKey.USAGE_HEALTH
    override val successSetMessageKey: MessageKey = MessageKey.SUCCESS_HEALTH_SET
    override val successResetMessageKey: MessageKey = MessageKey.SUCCESS_HEALTH_RESET
    override val trackedPlayers: MutableMap<Player, Double> = mutableMapOf()
    val healthPlayers: MutableMap<Player, Double> get() = trackedPlayers
    override val playerStateType: PlayerStateType = PlayerStateType.CUSTOM_HEALTH

    override fun applyAttribute(player: Player, value: Double) {
        removeAttribute(player)

        player.getAttribute(Attribute.MAX_HEALTH)?.addModifier(
            AttributeModifier(
                NamespacedKey(hooker.plugin, "health_mod"),
                value - defaultValue,
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
        player.health = value
    }

    override fun removeAttribute(player: Player) {
        val modifierKey = NamespacedKey(hooker.plugin, "health_mod")
        player.getAttribute(Attribute.MAX_HEALTH)
            ?.modifiers
            ?.find { it.key == modifierKey }
            ?.let { player.getAttribute(Attribute.MAX_HEALTH)?.removeModifier(it) }
    }

    override fun onAfterEffectRemoved(player: Player) {
        player.health = defaultValue
    }

}
