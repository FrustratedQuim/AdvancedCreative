package com.ratger.acreative.utils

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

object PlayerInventoryTransferSupport {
    fun tryAddToExistingStacks(inventory: PlayerInventory, item: ItemStack): Int {
        var remaining = item.amount
        for (slot in preferredSlots()) {
            remaining = tryAddToStackAtSlot(inventory, item, slot, remaining)
            if (remaining <= 0) {
                return 0
            }
        }
        return remaining
    }

    fun findPreferredEmptySlot(inventory: PlayerInventory): Int? {
        for (slot in preferredSlots()) {
            if (isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        return null
    }

    fun storeInPreferredSlots(inventory: PlayerInventory, item: ItemStack): Int {
        var remaining = tryAddToExistingStacks(inventory, item)

        while (remaining > 0) {
            val emptySlot = findPreferredEmptySlot(inventory) ?: break
            val maxForStack = item.maxStackSize.coerceAtLeast(1)
            val amountForSlot = remaining.coerceAtMost(maxForStack)
            inventory.setItem(emptySlot, item.clone().apply { amount = amountForSlot })
            remaining -= amountForSlot
        }

        return remaining
    }

    private fun tryAddToStackAtSlot(
        inventory: PlayerInventory,
        item: ItemStack,
        slot: Int,
        remaining: Int
    ): Int {
        var newRemaining = remaining

        val stack = inventory.getItem(slot) ?: return newRemaining
        if (!stack.isSimilar(item)) return newRemaining

        val maxForStack = stack.maxStackSize.coerceAtLeast(1)
        if (stack.amount >= maxForStack) return newRemaining

        val canAdd = (maxForStack - stack.amount).coerceAtMost(newRemaining)
        stack.amount += canAdd
        inventory.setItem(slot, stack)
        newRemaining -= canAdd

        return newRemaining
    }

    private fun preferredSlots(): Sequence<Int> = sequence {
        for (slot in 8 downTo 0) {
            yield(slot)
        }
        for (slot in 35 downTo 9) {
            yield(slot)
        }
    }

    private fun isEmpty(item: ItemStack?): Boolean {
        return item == null || item.type == Material.AIR || item.amount <= 0
    }
}
