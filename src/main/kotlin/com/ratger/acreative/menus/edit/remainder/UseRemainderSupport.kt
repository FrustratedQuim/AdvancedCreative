@file:Suppress("UnstableApiUsage") // Experimental UseRemainder

package com.ratger.acreative.menus.edit.remainder

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseRemainder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object UseRemainderSupport {

    fun get(item: ItemStack): ItemStack? {
        val remainder = item.getData(DataComponentTypes.USE_REMAINDER) ?: return null
        return remainder.transformInto().clone()
    }

    fun has(item: ItemStack): Boolean = get(item) != null

    fun set(item: ItemStack, remainder: ItemStack) {
        if (isEmpty(remainder)) {
            clear(item)
            return
        }
        item.setData(DataComponentTypes.USE_REMAINDER, UseRemainder.useRemainder(remainder.clone()))
    }

    fun clear(item: ItemStack) {
        item.unsetData(DataComponentTypes.USE_REMAINDER)
    }

    fun setOrClear(item: ItemStack, remainder: ItemStack?) {
        if (isEmpty(remainder)) {
            clear(item)
            return
        }
        set(item, requireNotNull(remainder))
    }

    fun isEmpty(stack: ItemStack?): Boolean {
        return stack == null || stack.type == Material.AIR || stack.amount <= 0
    }
}
