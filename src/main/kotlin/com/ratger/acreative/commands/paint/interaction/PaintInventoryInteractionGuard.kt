package com.ratger.acreative.commands.paint.interaction

import com.ratger.acreative.commands.paint.session.PaintSessionManager
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.paint.PaintMenuController
import com.ratger.acreative.menus.paint.PaintToolInventoryService
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack as BukkitItemStack

class PaintInventoryInteractionGuard(
    private val sessionManager: PaintSessionManager,
    private val toolInventoryService: PaintToolInventoryService,
    private val menuController: PaintMenuController,
    private val handleDropAction: (Player, Boolean) -> Boolean
) {

    fun handleInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = sessionManager.getSession(player.uniqueId) ?: return

        if (session.isMenuOpen) {
            return
        }

        val clicked = event.currentItem
        if (isWorkTool(clicked)) {
            event.isCancelled = true
            if (isDropClick(event.click)) {
                handleDropAction(player, event.click == ClickType.CONTROL_DROP)
            }
            return
        }

        val hotbarButton = event.hotbarButton
        if (hotbarButton in 0..8 && isWorkTool(player.inventory.getItem(hotbarButton))) {
            event.isCancelled = true
            return
        }

        if (isWorkTool(event.cursor)) {
            event.isCancelled = true
        }
    }

    fun handleInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = sessionManager.getSession(player.uniqueId) ?: return
        if (session.isMenuOpen) {
            return
        }
        if (event.newItems.values.any(::isWorkTool)) {
            event.isCancelled = true
            return
        }
        val topInventorySize = event.view.topInventory.size
        if (event.rawSlots.any { slot ->
                if (slot < topInventorySize) {
                    return@any false
                }
                val inventorySlot = event.view.convertSlot(slot)
                isWorkTool(player.inventory.getItem(inventorySlot))
            }
        ) {
            event.isCancelled = true
        }
    }

    fun handleInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val session = sessionManager.getSession(player.uniqueId) ?: return
        if (!session.isMenuOpen) return
        menuController.handleInventoryClose(player, session)
    }

    private fun isWorkTool(item: BukkitItemStack?): Boolean = toolInventoryService.isWorkTool(item)

    private fun isDropClick(click: ClickType): Boolean = MenuUiSupport.isDropClick(click)
}
