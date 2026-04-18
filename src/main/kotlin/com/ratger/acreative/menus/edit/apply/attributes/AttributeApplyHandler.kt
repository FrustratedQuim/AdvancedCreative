package com.ratger.acreative.menus.edit.apply.attributes

import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.attributes.AttributeModifierFactory
import com.ratger.acreative.menus.edit.attributes.ItemAttributeMenuSupport
import com.ratger.acreative.menus.edit.attributes.SlotGroupSpec
import com.ratger.acreative.menus.edit.ItemEditSession
import java.math.BigDecimal
import java.util.UUID
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

class AttributeApplyHandler : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.ATTRIBUTE

    private val attributeTokenMap = ItemAttributeMenuSupport.attributeTokenMap()
    private val slotTokens = mapOf(
        "any" to SlotGroupSpec.ANY,
        "any_hand" to SlotGroupSpec.HAND,
        "main_hand" to SlotGroupSpec.MAINHAND,
        "off_hand" to SlotGroupSpec.OFFHAND,
        "any_armor" to SlotGroupSpec.ARMOR,
        "helmet" to SlotGroupSpec.HEAD,
        "chest" to SlotGroupSpec.CHEST,
        "legs" to SlotGroupSpec.LEGS,
        "feet" to SlotGroupSpec.FEET,
        "body" to SlotGroupSpec.BODY
    )
    private val operationTokens = mapOf(
        "add" to AttributeModifier.Operation.ADD_NUMBER,
        "multipled_base" to AttributeModifier.Operation.ADD_SCALAR,
        "multiplied_base" to AttributeModifier.Operation.ADD_SCALAR,
        "multipled_total" to AttributeModifier.Operation.MULTIPLY_SCALAR_1,
        "multiplied_total" to AttributeModifier.Operation.MULTIPLY_SCALAR_1
    )

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size !in 2..4) return ApplyExecutionResult.InvalidValue

        val attribute = attributeTokenMap[args[0].lowercase()] ?: return ApplyExecutionResult.InvalidValue
        val parsedAmount = args[1].toBigDecimalOrNull() ?: return ApplyExecutionResult.InvalidValue

        val slotSpec = if (args.size >= 3) slotTokens[args[2].lowercase()] ?: return ApplyExecutionResult.InvalidValue else SlotGroupSpec.ANY
        val operation = if (args.size >= 4) operationTokens[args[3].lowercase()] ?: return ApplyExecutionResult.InvalidValue else AttributeModifier.Operation.ADD_NUMBER
        val normalizedAmount = ItemAttributeMenuSupport.normalizeInputAmount(parsedAmount, operation)

        val item = session.editableItem
        val explicit = ItemAttributeMenuSupport.currentEffectiveAttributes(item)
        val key = randomModifierKey()
        val modifier = AttributeModifierFactory.create(key, normalizedAmount, operation, slotSpec)

        explicit.put(attribute, modifier)
        ItemAttributeMenuSupport.writeExplicitAttributes(item, explicit)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> attributeTokenMap.keys.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> {
                val attribute = attributeTokenMap[args[0].lowercase()] ?: return emptyList()
                ItemAttributeMenuSupport.suggestedValues(attribute)
                    .filter { it.startsWith(args[1], ignoreCase = true) }
            }
            3 -> slotTokens.keys.filter { it.startsWith(args[2], ignoreCase = true) }
            4 -> listOf("add", "multipled_base", "multipled_total").filter { it.startsWith(args[3], ignoreCase = true) }
            else -> emptyList()
        }
    }

    private fun randomModifierKey(): NamespacedKey {
        return NamespacedKey.minecraft(UUID.randomUUID().toString())
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}
