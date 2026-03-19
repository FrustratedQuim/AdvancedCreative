package com.ratger.acreative.utils

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class PlayerStateManager(
    private val hooker: FunctionHooker
) {

    enum class PlayerStateType {
        CRAWLING,
        DISGUISED,
        CUSTOM_EFFECT,
        FROZEN,
        GLIDING,
        CUSTOM_HEALTH,
        CUSTOM_DAMAGE,
        CUSTOM_SIZE,
        HIDDEN_BY_SOMEONE,
        HIDING_SOMEONE,
        PISSING,
        LAYING,
        SITTING,
        GRABBING,
        GRABBED;

        fun conflicts(): Set<PlayerStateType> = when (this) {
            CRAWLING -> setOf(DISGUISED, FROZEN, GLIDING, LAYING, SITTING, GRABBING, GRABBED)
            DISGUISED -> setOf(CRAWLING, FROZEN, LAYING, SITTING, GRABBING, GRABBED)
            FROZEN -> setOf(DISGUISED, LAYING, SITTING, GRABBING, GRABBED)
            GLIDING -> setOf(CRAWLING, FROZEN, LAYING, SITTING, GRABBING, GRABBED)
            PISSING -> setOf(FROZEN, GRABBING, GRABBED)
            LAYING -> setOf(CRAWLING, FROZEN, DISGUISED, GLIDING, SITTING, GRABBING, GRABBED)
            SITTING -> setOf(CRAWLING, FROZEN, DISGUISED, GLIDING, LAYING, GRABBING, GRABBED)
            GRABBING -> setOf(CRAWLING, DISGUISED, FROZEN, GLIDING, PISSING, LAYING, SITTING, GRABBED)
            GRABBED -> setOf(CRAWLING, DISGUISED, FROZEN, GLIDING, PISSING, LAYING, SITTING, GRABBING)
            CUSTOM_SIZE -> setOf(CRAWLING, DISGUISED, LAYING, SITTING, GRABBING, GRABBED)
            CUSTOM_EFFECT,
            CUSTOM_HEALTH,
            CUSTOM_DAMAGE,
            HIDDEN_BY_SOMEONE,
            HIDING_SOMEONE -> emptySet()
        }
    }

    private val playerStates = mutableMapOf<UUID, MutableSet<PlayerStateType>>()
    private val deactivators = mutableMapOf<PlayerStateType, (Player) -> Unit>()
    private val inventorySessions = mutableMapOf<UUID, PlayerInventorySession>()

    fun registerDeactivator(state: PlayerStateType, deactivator: (Player) -> Unit) {
        deactivators[state] = deactivator
    }

    fun hasState(player: Player, state: PlayerStateType): Boolean {
        return playerStates[player.uniqueId]?.contains(state) == true
    }

    fun getStates(player: Player): Set<PlayerStateType> {
        return playerStates[player.uniqueId]?.toSet() ?: emptySet()
    }

    fun activateState(player: Player, state: PlayerStateType) {
        val stateSet = playerStates.computeIfAbsent(player.uniqueId) { mutableSetOf() }
        if (stateSet.contains(state)) return

        val conflicts = stateSet.filter { it in state.conflicts() || state in it.conflicts() }
        conflicts.forEach { conflictingState ->
            deactivators[conflictingState]?.invoke(player)
            playerStates[player.uniqueId]?.remove(conflictingState)
        }

        playerStates.computeIfAbsent(player.uniqueId) { mutableSetOf() }.add(state)
    }

    fun deactivateState(player: Player, state: PlayerStateType) {
        playerStates[player.uniqueId]?.remove(state)
        if (playerStates[player.uniqueId].isNullOrEmpty()) {
            playerStates.remove(player.uniqueId)
        }
    }

    fun clearPlayerStates(player: Player) {
        playerStates.remove(player.uniqueId)
        inventorySessions.remove(player.uniqueId)
    }

    fun savePlayerInventory(player: Player) {
        inventorySessions[player.uniqueId] = PlayerInventorySession.capture(player)
    }

    fun restorePlayerInventory(player: Player) {
        inventorySessions.remove(player.uniqueId)?.restore(player)
    }

    fun getCurrentSavedMainHandItem(player: Player): ItemStack? {
        return inventorySessions[player.uniqueId]?.getCurrentMainHandItem()
    }

    fun handleItemSwitch(player: Player, newSlot: Int) {
        inventorySessions[player.uniqueId]?.handleSlotSwitch(player, newSlot)

        if (hooker.utils.isLaying(player)) {
            hooker.layManager.updateMainHandEquipment(player)
        }
    }

    fun refreshPlayerPose(player: Player) {
        // Avoid scheduling or calling hide/show while the plugin is disabled (e.g., during shutdown)
        if (!hooker.plugin.isEnabled) {
            if (player.isOnline) {
                // Minimal local refresh without invoking Bukkit hide/show APIs
                player.isSneaking = false
            }
            return
        }

        if (!player.isOnline) return

        hooker.tickScheduler.runLater(1L) {
            // If the plugin has been disabled or player went offline meanwhile, do nothing
            if (!hooker.plugin.isEnabled || !player.isOnline) return@runLater

            player.isSneaking = false
            for (viewer in Bukkit.getOnlinePlayers()) {
                viewer.hidePlayer(hooker.plugin, player)
                if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                    viewer.showPlayer(hooker.plugin, player)
                }
            }
        }
    }

    private data class PlayerInventorySnapshot(
        val armor: Array<ItemStack?>,
        val offHand: ItemStack?
    ) {
        fun restore(player: Player) {
            val inventory = player.inventory
            inventory.armorContents = armor.map { it?.clone() }.toTypedArray()
            inventory.setItemInOffHand(offHand?.clone())
        }

        companion object {
            fun capture(player: Player): PlayerInventorySnapshot {
                val inventory = player.inventory
                return PlayerInventorySnapshot(
                    armor = inventory.armorContents.map { it?.clone() }.toTypedArray(),
                    offHand = inventory.itemInOffHand.clone()
                )
            }
        }
    }

    private class PlayerInventorySession(
        private val snapshot: PlayerInventorySnapshot,
        private var currentHotbarSlot: Int,
        private val hotbarItems: MutableMap<Int, ItemStack?>
    ) {
        fun restore(player: Player) {
            val inventory = player.inventory
            snapshot.restore(player)
            hotbarItems.forEach { (slot, item) ->
                inventory.setItem(slot, item?.clone())
            }
        }

        fun getCurrentMainHandItem(): ItemStack? {
            return hotbarItems[currentHotbarSlot]?.clone()
        }

        fun handleSlotSwitch(player: Player, newSlot: Int) {
            val inventory = player.inventory
            val previousSlot = currentHotbarSlot

            hotbarItems[previousSlot]?.let { item ->
                inventory.setItem(previousSlot, item.clone())
            }

            hotbarItems[newSlot] = inventory.getItem(newSlot)?.clone()
            inventory.setItem(newSlot, null)
            currentHotbarSlot = newSlot
        }

        companion object {
            fun capture(player: Player): PlayerInventorySession {
                val inventory = player.inventory
                val currentSlot = inventory.heldItemSlot
                return PlayerInventorySession(
                    snapshot = PlayerInventorySnapshot.capture(player),
                    currentHotbarSlot = currentSlot,
                    hotbarItems = mutableMapOf(currentSlot to inventory.getItem(currentSlot)?.clone())
                )
            }
        }
    }
}
