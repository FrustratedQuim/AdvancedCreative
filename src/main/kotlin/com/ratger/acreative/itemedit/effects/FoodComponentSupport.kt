@file:Suppress("UnstableApiUsage") // Experimental Consumable

package com.ratger.acreative.itemedit.effects

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.FoodProperties
import org.bukkit.inventory.ItemStack

object FoodComponentSupport {
    fun nutrition(item: ItemStack): Int = item.getData(DataComponentTypes.FOOD)?.nutrition() ?: 0

    fun saturation(item: ItemStack): Float = item.getData(DataComponentTypes.FOOD)?.saturation() ?: 0f

    fun canAlwaysEat(item: ItemStack): Boolean = item.getData(DataComponentTypes.FOOD)?.canAlwaysEat() ?: false

    fun setNutrition(item: ItemStack, value: Int) {
        val builder = foodBuilder(item)
        builder.nutrition(value)
        item.setData(DataComponentTypes.FOOD, builder.build())
    }

    fun setSaturation(item: ItemStack, value: Float) {
        val builder = foodBuilder(item)
        builder.saturation(value)
        item.setData(DataComponentTypes.FOOD, builder.build())
    }

    fun setCanAlwaysEat(item: ItemStack, value: Boolean) {
        val builder = foodBuilder(item)
        builder.canAlwaysEat(value)
        item.setData(DataComponentTypes.FOOD, builder.build())
    }

    fun clear(item: ItemStack) {
        item.unsetData(DataComponentTypes.FOOD)
    }

    private fun foodBuilder(item: ItemStack): FoodProperties.Builder =
        item.getData(DataComponentTypes.FOOD)?.toBuilder() ?: FoodProperties.food()
}
