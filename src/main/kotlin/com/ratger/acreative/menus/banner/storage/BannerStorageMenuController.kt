package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.menus.banner.service.BannerPatternSupport
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.menu.event.DragEvent
import java.util.logging.Logger

class BannerStorageMenuController(
    private val plugin: Plugin,
    private val storageService: BannerStorageService,
    private val logger: Logger
) {
    fun buildEditClickListener(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        onLimitUpdate: (Menu) -> Unit
    ): (ClickEvent) -> Boolean {
        return { event ->
            val allowed = when {
                event.rawSlot in 0 until pageSize -> handleTopInventoryClick(player, session, pageSize, event)
                else -> handlePlayerInventoryClick(player, session, pageSize, event)
            }

            if (allowed && shouldRefreshAfterClick(event, pageSize)) {
                scheduleRefresh(event.menu, onLimitUpdate)
            }
            allowed
        }
    }

    fun buildEditDragListener(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        onLimitUpdate: (Menu) -> Unit
    ): (DragEvent) -> Boolean {
        return { event ->
            val topSlots = event.rawSlots.filter { it in 0 until pageSize }
            if (topSlots.isEmpty()) {
                true
            } else if (!isEmpty(event.oldCursor) && !BannerPatternSupport.isBanner(event.oldCursor)) {
                logDragDecision(event, event.oldCursor, "drag_blocked_non_banner_cursor")
                false
            } else {
                val occupiedBefore = currentStoredCount(session, event.menu, pageSize)
                val newlyOccupied = topSlots.count { isEmpty(event.menu.inventory.getItem(it)) }
                val limit = storageService.limitSnapshot(player, session.layout)
                val withinLimit = limit.limit < 0 || occupiedBefore + newlyOccupied <= limit.limit
                logDragDecision(event, event.oldCursor, if (withinLimit) "drag_vanilla_allowed" else "drag_blocked_limit_reached")
                if (withinLimit) {
                    scheduleRefresh(event.menu, onLimitUpdate)
                }
                withinLimit
            }
        }
    }

    fun syncPageFromMenu(session: BannerStorageSession, menu: Menu, pageSize: Int) {
        for (slot in 0 until pageSize) {
            val item = menu.inventory.getItem(slot)
            val normalized = item?.let(storageService::normalizeEditableItem)
            storageService.setPageSlot(session.layout, session.page, pageSize, slot, normalized)
        }
    }

    private fun handleTopInventoryClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        event: ClickEvent
    ): Boolean {
        val cursor = event.cursor
        val clickedItem = event.clickedItem
        val currentItem = event.menu.inventory.getItem(event.rawSlot)

        if (event.isShiftLeft || event.isShiftRight) {
            logEditClickDecision(event, cursor, clickedItem, "top_vanilla_shift_allowed")
            return true
        }

        if (event.isHotbar) {
            val hotbarItem = player.inventory.getItem(event.hotbarKey)
            if (!isEmpty(hotbarItem) && !BannerPatternSupport.isBanner(hotbarItem)) {
                logEditClickDecision(event, cursor, hotbarItem, "top_blocked_non_banner_hotbar")
                return false
            }
        }

        if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            logEditClickDecision(event, cursor, clickedItem, "top_vanilla_move_to_other_inventory")
            return true
        }

        if (event.action == InventoryAction.COLLECT_TO_CURSOR) {
            if (!isEmpty(cursor) && !BannerPatternSupport.isBanner(cursor)) {
                logEditClickDecision(event, cursor, clickedItem, "top_blocked_collect_non_banner_cursor")
                return false
            }
            logEditClickDecision(event, cursor, clickedItem, "top_vanilla_collect_to_cursor")
            return true
        }

        if (event.action == InventoryAction.NOTHING) {
            logEditClickDecision(event, cursor, clickedItem, "top_vanilla_nothing")
            return true
        }

        val limitSnapshot = storageService.limitSnapshot(player, session.layout)
        val placingIntoEmptySlot = isEmpty(currentItem) && !isEmpty(cursor)
        if (placingIntoEmptySlot && limitSnapshot.limit >= 0 && currentStoredCount(session, event.menu, pageSize) >= limitSnapshot.limit) {
            logEditClickDecision(event, cursor, clickedItem, "top_blocked_limit_reached")
            return false
        }

        val incomingItem = when {
            !isEmpty(cursor) -> cursor
            !isEmpty(currentItem) -> null
            else -> null
        }

        if (!isEmpty(incomingItem) && !BannerPatternSupport.isBanner(incomingItem)) {
            logEditClickDecision(event, cursor, clickedItem, "top_blocked_non_banner_cursor")
            return false
        }

        logEditClickDecision(event, cursor, clickedItem, "top_vanilla_allowed")
        return true
    }

    private fun handlePlayerInventoryClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        event: ClickEvent
    ): Boolean {
        if (!(event.isShiftLeft || event.isShiftRight)) {
            return true
        }

        val clickedItem = event.clickedItem ?: return true
        if (!BannerPatternSupport.isBanner(clickedItem)) {
            logEditClickDecision(event, player.itemOnCursor, clickedItem, "player_shift_blocked_non_banner")
            return false
        }

        val limit = storageService.limitSnapshot(player, session.layout)
        if (limit.limit >= 0 && currentStoredCount(session, event.menu, pageSize) >= limit.limit) {
            val targetSlot = (0 until pageSize).firstOrNull { isEmpty(event.menu.inventory.getItem(it)) }
            if (targetSlot == null) {
                logEditClickDecision(event, player.itemOnCursor, clickedItem, "player_shift_blocked_limit_reached")
                return false
            }
        }

        logEditClickDecision(event, player.itemOnCursor, clickedItem, "player_shift_vanilla_allowed")
        return true
    }

    private fun isEmpty(item: ItemStack?): Boolean = item == null || item.type.isAir || item.amount <= 0

    private fun currentStoredCount(session: BannerStorageSession, menu: Menu, pageSize: Int): Int {
        val currentPageBase = (session.page - 1).coerceAtLeast(0) * pageSize
        val outsideCurrentPage = session.layout.keys.count { it !in currentPageBase until (currentPageBase + pageSize) }
        val currentPageCount = (0 until pageSize).count { !isEmpty(menu.inventory.getItem(it)) }
        return outsideCurrentPage + currentPageCount
    }

    private fun shouldRefreshAfterClick(event: ClickEvent, pageSize: Int): Boolean {
        if (event.rawSlot in 0 until pageSize) {
            return true
        }
        return event.isShiftLeft || event.isShiftRight
    }

    private fun scheduleRefresh(menu: Menu, onLimitUpdate: (Menu) -> Unit) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            onLimitUpdate(menu)
        })
    }

    private fun logEditClickDecision(
        event: ClickEvent?,
        cursor: ItemStack?,
        clickedItem: ItemStack?,
        action: String
    ) {
        logger.info(
            "[BannerStorage/Edit] clickType=${event?.type ?: "N/A"}, " +
                "cursorHasItem=${!isEmpty(cursor)}, slotHasItem=${!isEmpty(clickedItem)}, action=$action"
        )
    }

    private fun logDragDecision(
        event: DragEvent?,
        cursor: ItemStack?,
        action: String
    ) {
        logger.info(
            "[BannerStorage/Edit] dragType=${event?.type ?: "N/A"}, " +
                "cursorHasItem=${!isEmpty(cursor)}, rawSlots=${event?.rawSlots ?: emptySet<Int>()}, action=$action"
        )
    }
}
