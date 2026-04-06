@file:Suppress("UnstableApiUsage") // Experimental EquipmentSlotGroup

package com.ratger.acreative.itemedit.attributes

import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import java.util.*

object SlotGroupAdapter {
    private val slotDisplayNames = mapOf(
        SlotGroupSpec.ANY to "Любой слот",
        SlotGroupSpec.HAND to "Любая рука",
        SlotGroupSpec.MAINHAND to "Главная рука",
        SlotGroupSpec.OFFHAND to "Вторая рука",
        SlotGroupSpec.ARMOR to "Броня",
        SlotGroupSpec.HEAD to "Шлем",
        SlotGroupSpec.CHEST to "Нагрудник",
        SlotGroupSpec.LEGS to "Поножи",
        SlotGroupSpec.FEET to "Ботинки",
        SlotGroupSpec.BODY to "Тело"
    )

    fun toPaperGroup(spec: SlotGroupSpec): EquipmentSlotGroup {
        return when (spec) {
            SlotGroupSpec.MAINHAND -> EquipmentSlotGroup.MAINHAND
            SlotGroupSpec.OFFHAND -> EquipmentSlotGroup.OFFHAND
            SlotGroupSpec.HAND -> EquipmentSlotGroup.HAND
            SlotGroupSpec.FEET -> EquipmentSlotGroup.FEET
            SlotGroupSpec.LEGS -> EquipmentSlotGroup.LEGS
            SlotGroupSpec.CHEST -> EquipmentSlotGroup.CHEST
            SlotGroupSpec.HEAD -> EquipmentSlotGroup.HEAD
            SlotGroupSpec.ARMOR -> EquipmentSlotGroup.ARMOR
            SlotGroupSpec.BODY -> EquipmentSlotGroup.BODY
            SlotGroupSpec.ANY -> EquipmentSlotGroup.ANY
        }
    }

    fun fromPaperGroup(group: EquipmentSlotGroup): SlotGroupSpec {
        return when (group) {
            EquipmentSlotGroup.MAINHAND -> SlotGroupSpec.MAINHAND
            EquipmentSlotGroup.OFFHAND -> SlotGroupSpec.OFFHAND
            EquipmentSlotGroup.HAND -> SlotGroupSpec.HAND
            EquipmentSlotGroup.FEET -> SlotGroupSpec.FEET
            EquipmentSlotGroup.LEGS -> SlotGroupSpec.LEGS
            EquipmentSlotGroup.CHEST -> SlotGroupSpec.CHEST
            EquipmentSlotGroup.HEAD -> SlotGroupSpec.HEAD
            EquipmentSlotGroup.ARMOR -> SlotGroupSpec.ARMOR
            EquipmentSlotGroup.BODY -> SlotGroupSpec.BODY
            EquipmentSlotGroup.ANY -> SlotGroupSpec.ANY
            else -> SlotGroupSpec.ANY
        }
    }

    fun displayName(modifier: AttributeModifier): String {
        val spec = fromPaperGroup(modifier.slotGroup)
        return slotDisplayNames[spec] ?: toReadableName(spec.name)
    }

    private fun toReadableName(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
    }
}
