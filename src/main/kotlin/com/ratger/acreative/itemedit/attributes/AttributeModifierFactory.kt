@file:Suppress("UnstableApiUsage") // Experimental AttributeModifier

package com.ratger.acreative.itemedit.attributes

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier

object AttributeModifierFactory {
    fun create(
        key: NamespacedKey,
        amount: Double,
        operation: AttributeModifier.Operation,
        slotGroupSpec: SlotGroupSpec?
    ): AttributeModifier {
        if (slotGroupSpec == null) {
            return AttributeModifier(key, amount, operation)
        }

        return AttributeModifier(key, amount, operation, SlotGroupAdapter.toPaperGroup(slotGroupSpec))
    }
}
