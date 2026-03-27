@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.commands.edit

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier

object EditAttributeModifierFactory {
    fun create(
        key: NamespacedKey,
        amount: Double,
        operation: AttributeModifier.Operation,
        slotGroupSpec: EditSlotGroupSpec?
    ): AttributeModifier {
        if (slotGroupSpec == null) {
            return AttributeModifier(key, amount, operation)
        }

        return AttributeModifier(key, amount, operation, EditSlotGroupAdapter.toPaperGroup(slotGroupSpec))
    }
}
