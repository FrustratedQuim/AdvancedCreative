package com.ratger.acreative.menus.edit

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.security.MessageDigest
import java.util.UUID

class ItemEditSessionManager {
    private val sessions = mutableMapOf<UUID, ItemEditSession>()
    private val closeListeners = mutableListOf<(Player, ItemEditSession) -> Unit>()

    fun openSession(player: Player, sourceItem: ItemStack): ItemEditSession {
        val session = ItemEditSession(
            playerId = player.uniqueId,
            originalMainHandSlot = player.inventory.heldItemSlot,
            editableItem = sourceItem.clone(),
            initialContentHash = contentHash(sourceItem)
        )
        sessions[player.uniqueId] = session
        return session
    }

    fun isInSession(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun getSession(player: Player): ItemEditSession? = sessions[player.uniqueId]

    fun updateEditableItem(player: Player, item: ItemStack) {
        sessions[player.uniqueId]?.editableItem = item.clone()
    }

    fun closeSession(player: Player): ItemEditSession? {
        val closed = sessions.remove(player.uniqueId) ?: return null
        closeListeners.forEach { it(player, closed) }
        return closed
    }

    fun addCloseListener(listener: (Player, ItemEditSession) -> Unit) {
        closeListeners += listener
    }

    fun sessionsSnapshot(): List<ItemEditSession> = sessions.values.toList()

    private fun contentHash(item: ItemStack): String? {
        if (item.type.isAir || item.amount <= 0) return null
        val normalized = item.clone().apply { amount = 1 }
        val bytes = normalized.serializeAsBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
