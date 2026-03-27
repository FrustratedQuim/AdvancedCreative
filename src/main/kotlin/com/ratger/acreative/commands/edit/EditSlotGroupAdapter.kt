@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.commands.edit

import org.bukkit.inventory.EquipmentSlotGroup

object EditSlotGroupAdapter {
    fun toPaperGroup(spec: EditSlotGroupSpec): EquipmentSlotGroup {
        return when (spec) {
            EditSlotGroupSpec.MAINHAND -> EquipmentSlotGroup.MAINHAND
            EditSlotGroupSpec.OFFHAND -> EquipmentSlotGroup.OFFHAND
            EditSlotGroupSpec.HAND -> EquipmentSlotGroup.HAND
            EditSlotGroupSpec.FEET -> EquipmentSlotGroup.FEET
            EditSlotGroupSpec.LEGS -> EquipmentSlotGroup.LEGS
            EditSlotGroupSpec.CHEST -> EquipmentSlotGroup.CHEST
            EditSlotGroupSpec.HEAD -> EquipmentSlotGroup.HEAD
            EditSlotGroupSpec.ARMOR -> EquipmentSlotGroup.ARMOR
            EditSlotGroupSpec.BODY -> EquipmentSlotGroup.BODY
            EditSlotGroupSpec.ANY -> EquipmentSlotGroup.ANY
        }
    }
}
