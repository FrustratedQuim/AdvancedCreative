package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MenuService(
    private val hooker: FunctionHooker
) {
    private val parser = MiniMessageParser()
    private val sessionManager = ItemEditSessionManager()
    private val buttonFactory = MenuButtonFactory(parser)
    private val itemEditMenu = ItemEditMenu(hooker, sessionManager, buttonFactory, parser)

    fun isInItemEditSession(player: Player): Boolean = sessionManager.isInSession(player)

    fun openItemEditor(player: Player) {
        val existingSession = sessionManager.getSession(player)
        if (existingSession != null) {
            itemEditMenu.openRoot(player, existingSession)
            return
        }

        val handItem = player.inventory.itemInMainHand
        if (handItem.type == Material.AIR || handItem.amount <= 0) {
            hooker.messageManager.sendChat(player, MessageKey.EDIT_EMPTY_HAND)
            return
        }

        val session = sessionManager.openSession(player, handItem)
        player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        itemEditMenu.openRoot(player, session)
    }

    fun syncEditedItemBack(player: Player, session: ItemEditSession) {
        val item = session.editableItem.clone()
        if (item.type == Material.AIR || item.amount <= 0) return

        val inventory = player.inventory
        val targetSlotItem = inventory.getItem(session.originalMainHandSlot)

        if (targetSlotItem == null || targetSlotItem.type == Material.AIR || targetSlotItem.amount <= 0) {
            inventory.setItem(session.originalMainHandSlot, item)
            return
        }

        val emptySlot = inventory.firstEmpty()
        if (emptySlot != -1) {
            inventory.setItem(emptySlot, item)
            return
        }

        player.world.dropItemNaturally(player.location.clone().add(0.0, 1.0, 0.0), item)
    }
}
