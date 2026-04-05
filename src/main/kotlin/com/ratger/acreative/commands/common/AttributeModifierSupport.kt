package com.ratger.acreative.commands.common

import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

object AttributeModifierSupport {

    fun applyAddNumberModifier(
        player: Player,
        attribute: Attribute,
        plugin: Plugin,
        modifierName: String,
        value: Double
    ) {
        removeModifier(player, attribute, plugin, modifierName)
        player.getAttribute(attribute)?.addModifier(
            AttributeModifier(
                NamespacedKey(plugin, modifierName),
                value,
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
    }

    fun removeModifier(
        player: Player,
        attribute: Attribute,
        plugin: Plugin,
        modifierName: String
    ) {
        val modifierKey = NamespacedKey(plugin, modifierName)
        player.getAttribute(attribute)
            ?.modifiers
            ?.find { it.key == modifierKey }
            ?.let { player.getAttribute(attribute)?.removeModifier(it) }
    }
}
