package com.ratger.acreative.menus

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class ItemEditSessionManager {
    private val sessions = mutableMapOf<UUID, ItemEditSession>()

    fun openSession(player: Player, sourceItem: ItemStack): ItemEditSession {
        val session = ItemEditSession(
            playerId = player.uniqueId,
            originalMainHandSlot = player.inventory.heldItemSlot,
            editableItem = sourceItem.clone()
        )
        sessions[player.uniqueId] = session
        return session
    }

    fun isInSession(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun getSession(player: Player): ItemEditSession? = sessions[player.uniqueId]

    fun updateEditableItem(player: Player, item: ItemStack) {
        sessions[player.uniqueId]?.editableItem = item.clone()
    }

    fun closeSession(player: Player): ItemEditSession? = sessions.remove(player.uniqueId)
}
