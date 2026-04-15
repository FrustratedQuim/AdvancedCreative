package com.ratger.acreative.menus.edit.container

import org.bukkit.Material
import org.bukkit.block.BrushableBlock
import org.bukkit.block.Campfire
import org.bukkit.block.ChiseledBookshelf
import org.bukkit.block.Container
import org.bukkit.block.DecoratedPot
import org.bukkit.block.Jukebox
import org.bukkit.block.Lectern
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object ContainerSupport {
    data class ContainerContentsSnapshot(
        val capacity: Int,
        val contents: MutableList<ItemStack>
    )

    fun containerCapacity(material: Material): Int? {
        if (material.name.endsWith("SHULKER_BOX")) return 27
        return when (material) {
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL -> 27
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER -> 3
            Material.DISPENSER,
            Material.DROPPER,
            Material.CRAFTER -> 9
            Material.HOPPER -> 5
            Material.BREWING_STAND -> 5
            Material.JUKEBOX -> 1
            Material.CHISELED_BOOKSHELF -> 6
            Material.LECTERN -> 1
            Material.DECORATED_POT -> 1
            Material.SUSPICIOUS_SAND,
            Material.SUSPICIOUS_GRAVEL -> 1
            Material.SOUL_CAMPFIRE,
            Material.CAMPFIRE -> 4
            Material.GRINDSTONE -> 3
            else -> null
        }
    }

    fun readContainerContents(item: ItemStack): ContainerContentsSnapshot? {
        val capacity = containerCapacity(item.type) ?: return null
        val meta = item.itemMeta as? BlockStateMeta ?: return null
        val state = meta.blockState
        val out = MutableList(capacity) { ItemStack(Material.AIR) }

        when (state) {
            is Container -> copyFromInventory(state.inventory, out)
            is ChiseledBookshelf -> copyFromInventory(state.inventory, out)
            is Jukebox -> copyFromInventory(state.inventory, out)
            is Lectern -> copyFromInventory(state.inventory, out)
            is DecoratedPot -> copyFromInventory(state.inventory, out)
            is BrushableBlock -> out[0] = normalize(state.item)
            is Campfire -> for (slot in 0 until minOf(capacity, 4)) out[slot] = normalize(state.getItem(slot))
            else -> return null
        }

        return ContainerContentsSnapshot(capacity, out)
    }

    fun supportsStableContainerEditing(item: ItemStack): Boolean = readContainerContents(item) != null

    fun applyContainerContents(item: ItemStack, contents: List<ItemStack>): Boolean {
        val capacity = containerCapacity(item.type) ?: return false
        if (contents.size < capacity) return false
        val meta = item.itemMeta as? BlockStateMeta ?: return false
        val state = meta.blockState

        val normalized = MutableList(capacity) { index -> normalize(contents[index]) }

        val applied = when (state) {
            is Container -> applyToInventory(state.inventory, normalized)
            is ChiseledBookshelf -> applyToInventory(state.inventory, normalized)
            is Jukebox -> applyToInventory(state.inventory, normalized)
            is Lectern -> applyToInventory(state.inventory, normalized)
            is DecoratedPot -> applyToInventory(state.inventory, normalized)
            is BrushableBlock -> {
                state.setItem(normalized[0])
                true
            }
            is Campfire -> {
                for (slot in 0 until minOf(capacity, 4)) {
                    state.setItem(slot, normalized[slot])
                }
                true
            }
            else -> false
        }

        if (!applied) return false

        meta.blockState = state
        item.itemMeta = meta
        return true
    }

    private fun copyFromInventory(inventory: Inventory?, out: MutableList<ItemStack>) {
        if (inventory == null) return
        val limit = minOf(out.size, inventory.size)
        for (slot in 0 until limit) {
            out[slot] = normalize(inventory.getItem(slot))
        }
    }

    private fun applyToInventory(inventory: Inventory?, contents: List<ItemStack>): Boolean {
        if (inventory == null) return false
        val limit = minOf(inventory.size, contents.size)
        for (slot in 0 until limit) {
            inventory.setItem(slot, normalize(contents[slot]))
        }
        return true
    }

    private fun normalize(stack: ItemStack?): ItemStack {
        if (stack == null || stack.type == Material.AIR || stack.amount <= 0) {
            return ItemStack(Material.AIR)
        }
        return stack.clone()
    }

    fun isEmpty(stack: ItemStack?): Boolean = stack == null || stack.type == Material.AIR || stack.amount <= 0
}
