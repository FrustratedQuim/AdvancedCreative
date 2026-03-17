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
        SITTING;

        fun conflicts(): Set<PlayerStateType> = when (this) {
            CRAWLING -> setOf(DISGUISED, FROZEN, GLIDING, LAYING, SITTING)
            DISGUISED -> setOf(CRAWLING, FROZEN, LAYING, SITTING)
            FROZEN -> setOf(DISGUISED, LAYING, SITTING)
            GLIDING -> setOf(CRAWLING, FROZEN, LAYING, SITTING)
            PISSING -> setOf(FROZEN)
            LAYING -> setOf(CRAWLING, FROZEN, DISGUISED, GLIDING, SITTING)
            SITTING -> setOf(CRAWLING, FROZEN, DISGUISED, GLIDING, LAYING)
            CUSTOM_SIZE -> setOf(CRAWLING, DISGUISED, LAYING, SITTING)
            CUSTOM_EFFECT,
            CUSTOM_HEALTH,
            CUSTOM_DAMAGE,
            HIDDEN_BY_SOMEONE,
            HIDING_SOMEONE -> emptySet()
        }
    }

    private val playerStates = mutableMapOf<UUID, MutableSet<PlayerStateType>>()
    private val deactivators = mutableMapOf<PlayerStateType, (Player) -> Unit>()

    val savedItems = mutableMapOf<UUID, PlayerInventoryState>()
    val monitoredPlayers = mutableSetOf<Player>()

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
    }

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
}
