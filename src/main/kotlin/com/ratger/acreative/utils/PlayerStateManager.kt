package com.ratger.acreative.utils

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class PlayerStateManager(
    private val hooker: FunctionHooker
) {

    val savedItems = mutableMapOf<UUID, PlayerInventoryState>()
    val monitoredPlayers = mutableSetOf<Player>()

    data class PlayerInventoryState(
        val armor: Array<ItemStack?>,
        val offHand: ItemStack?,
        val mainHand: ItemStack?,
        var currentHotbarSlot: Int,
        val hotbarItems: MutableMap<Int, ItemStack?>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PlayerInventoryState
            if (currentHotbarSlot != other.currentHotbarSlot) return false
            if (!armor.contentEquals(other.armor)) return false
            if (offHand != other.offHand) return false
            if (hotbarItems != other.hotbarItems) return false
            return true
        }

        override fun hashCode(): Int {
            var result = currentHotbarSlot
            result = 31 * result + armor.contentHashCode()
            result = 31 * result + (offHand?.hashCode() ?: 0)
            result = 31 * result + hotbarItems.hashCode()
            return result
        }
    }

    fun savePlayerInventory(player: Player) {
        val inventory = player.inventory
        val state = PlayerInventoryState(
            armor = inventory.armorContents.clone(),
            offHand = inventory.itemInOffHand.clone(),
            mainHand = inventory.itemInMainHand.clone(),
            currentHotbarSlot = inventory.heldItemSlot,
            hotbarItems = mutableMapOf(inventory.heldItemSlot to inventory.getItem(inventory.heldItemSlot)?.clone())
        )
        savedItems[player.uniqueId] = state
        monitoredPlayers.add(player)
    }

    fun restorePlayerInventory(player: Player) {
        savedItems[player.uniqueId]?.let { state ->
            player.inventory.armorContents = state.armor
            player.inventory.setItemInOffHand(state.offHand)
            state.hotbarItems.forEach { (slot, item) ->
                player.inventory.setItem(slot, item)
            }
            savedItems.remove(player.uniqueId)
        }
        monitoredPlayers.remove(player)
    }

    fun handleItemSwitch(player: Player, newSlot: Int) {
        savedItems[player.uniqueId]?.let { state ->
            val inventory = player.inventory
            val previousSlot = state.currentHotbarSlot

            state.hotbarItems[previousSlot]?.let { item ->
                inventory.setItem(previousSlot, item)
            }

            val newItem = inventory.getItem(newSlot)?.clone()
            state.hotbarItems[newSlot] = newItem
            inventory.setItem(newSlot, null)
            state.currentHotbarSlot = newSlot

            if (hooker.utils.isLaying(player)) {
                hooker.layManager.updateMainHandEquipment(player)
            }
        }
    }

    fun refreshPlayerPose(player: Player) {
        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            player.isSneaking = false
            for (viewer in Bukkit.getOnlinePlayers()) {
                viewer.hidePlayer(hooker.plugin, player)
                if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                    viewer.showPlayer(hooker.plugin, player)
                }
            }
        }, 1L)
    }
}