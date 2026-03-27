@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.itemedit.attributes

import org.bukkit.inventory.EquipmentSlotGroup

object SlotGroupAdapter {
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
}
