package com.ratger.acreative.menus.banner.editor

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class BannerEditorSessionManager {
    private val sessions = mutableMapOf<UUID, BannerEditorSession>()
    private val closeListeners = mutableListOf<(Player, BannerEditorSession) -> Unit>()

    fun openSession(player: Player, sourceBanner: ItemStack?, openedFromMainMenu: Boolean): BannerEditorSession {
        val session = BannerEditorSession(
            playerId = player.uniqueId,
            originalMainHandSlot = player.inventory.heldItemSlot,
            editableBanner = sourceBanner?.clone(),
            openedFromMainMenu = openedFromMainMenu
        )
        sessions[player.uniqueId] = session
        return session
    }

    fun getSession(player: Player): BannerEditorSession? = sessions[player.uniqueId]

    fun closeSession(player: Player): BannerEditorSession? {
        val closed = sessions.remove(player.uniqueId) ?: return null
        closeListeners.forEach { it(player, closed) }
        return closed
    }

    fun clear(playerId: UUID) {
        sessions.remove(playerId)
    }

    fun sessionsSnapshot(): List<BannerEditorSession> = sessions.values.toList()
}
