package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object EditEquippableSupport {
    fun existingEquippable(item: ItemStack): Equippable? = item.getData(DataComponentTypes.EQUIPPABLE)

    fun equippableBuilder(item: ItemStack): Equippable.Builder? = existingEquippable(item)?.toBuilder()

    fun equippableBuilderOrPrototype(item: ItemStack): Equippable.Builder? {
        return equippableBuilder(item) ?: item.type.getDefaultData(DataComponentTypes.EQUIPPABLE)?.toBuilder()
    }

    fun rebuildEquippableWithSlot(current: Equippable, newSlot: EquipmentSlot): Equippable {
        val builder = Equippable.equippable(newSlot)
        builder.dispensable(current.dispensable())
        builder.swappable(current.swappable())
        builder.damageOnHurt(current.damageOnHurt())
        builder.equipSound(current.equipSound())
        builder.cameraOverlay(current.cameraOverlay())
        builder.assetId(current.assetId())
        builder.allowedEntities(current.allowedEntities())
        return builder.build()
    }

    fun apply(item: ItemStack, builder: Equippable.Builder) {
        item.setData(DataComponentTypes.EQUIPPABLE, builder.build())
    }
}
