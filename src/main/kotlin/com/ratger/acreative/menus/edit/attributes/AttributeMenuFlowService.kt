package com.ratger.acreative.menus.edit.attributes

import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.UUID

class AttributeMenuFlowService {
    private val attributesByKey = ItemAttributeMenuSupport.attributeTokenMap()

    private val orderedOperations = listOf(
        AttributeModifier.Operation.ADD_NUMBER,
        AttributeModifier.Operation.ADD_SCALAR,
        AttributeModifier.Operation.MULTIPLY_SCALAR_1
    )

    private val orderedSlots = listOf(
        SlotGroupSpec.ANY,
        SlotGroupSpec.HAND,
        SlotGroupSpec.MAINHAND,
        SlotGroupSpec.OFFHAND,
        SlotGroupSpec.ARMOR,
        SlotGroupSpec.HEAD,
        SlotGroupSpec.CHEST,
        SlotGroupSpec.LEGS,
        SlotGroupSpec.FEET
    )

    fun begin(session: ItemEditSession) {
        if (session.attributeDraftAmount.isBlank()) {
            session.attributeDraftAmount = "1"
        }
        if (session.attributeDraftOperation !in orderedOperations) {
            session.attributeDraftOperation = AttributeModifier.Operation.ADD_NUMBER
        }
        if (session.attributeDraftSlot !in orderedSlots) {
            session.attributeDraftSlot = SlotGroupSpec.ANY
        }
    }

    fun resolveSelected(session: ItemEditSession): Attribute? {
        val key = session.attributeDraftKey ?: return null
        return attributesByKey[key]
    }

    fun setSelected(session: ItemEditSession, attribute: Attribute?) {
        session.attributeDraftKey = attribute?.key?.key
    }

    fun setAmount(session: ItemEditSession, rawInput: String?) {
        val sanitized = rawInput?.trim().orEmpty()
        if (sanitized.isBlank()) return
        val parsed = sanitized.toBigDecimalOrNull() ?: return
        val clamped = ItemAttributeMenuSupport.clampInputAmount(parsed, session.attributeDraftOperation)
        session.attributeDraftAmount = clamped.stripTrailingZeros().toPlainString()
    }

    fun setOperationByIndex(session: ItemEditSession, index: Int) {
        session.attributeDraftOperation = orderedOperations[index.coerceIn(0, orderedOperations.lastIndex)]
    }

    fun operationIndex(session: ItemEditSession): Int {
        return orderedOperations.indexOf(session.attributeDraftOperation).takeIf { it >= 0 } ?: 0
    }

    fun operationOptions(): List<MenuAttributeOption<AttributeModifier.Operation>> {
        return orderedOperations.map {
            MenuAttributeOption(
                value = it,
                label = ItemAttributeMenuSupport.displayOperation(it)
            )
        }
    }

    fun setSlotByIndex(session: ItemEditSession, index: Int) {
        session.attributeDraftSlot = orderedSlots[index.coerceIn(0, orderedSlots.lastIndex)]
    }

    fun slotIndex(session: ItemEditSession): Int {
        return orderedSlots.indexOf(session.attributeDraftSlot).takeIf { it >= 0 } ?: 0
    }

    fun slotOptions(): List<MenuAttributeOption<SlotGroupSpec>> {
        return orderedSlots.map {
            MenuAttributeOption(
                value = it,
                label = SlotGroupAdapter.displayName(it)
            )
        }
    }

    fun apply(session: ItemEditSession): Boolean {
        val attribute = resolveSelected(session) ?: return false
        val amount = session.attributeDraftAmount.toBigDecimalOrNull() ?: return false
        val normalizedAmount = ItemAttributeMenuSupport.normalizeInputAmount(amount, session.attributeDraftOperation)

        val explicit = ItemAttributeMenuSupport.currentEffectiveAttributes(session.editableItem)
        explicit.put(
            attribute,
            AttributeModifierFactory.create(
                key = randomModifierKey(),
                amount = normalizedAmount,
                operation = session.attributeDraftOperation,
                slotGroupSpec = session.attributeDraftSlot
            )
        )
        ItemAttributeMenuSupport.writeExplicitAttributes(session.editableItem, explicit)
        return true
    }

    fun reset(session: ItemEditSession) {
        session.attributeDraftKey = null
        session.attributeDraftAmount = "1"
        session.attributeDraftOperation = AttributeModifier.Operation.ADD_NUMBER
        session.attributeDraftSlot = SlotGroupSpec.ANY
        session.attributeDraftLastTypePage = 0
    }

    private fun randomModifierKey(): NamespacedKey = NamespacedKey.minecraft(UUID.randomUUID().toString())

    data class MenuAttributeOption<T>(
        val value: T,
        val label: String
    )
}

