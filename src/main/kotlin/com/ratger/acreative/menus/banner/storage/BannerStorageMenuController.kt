package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.menus.banner.service.BannerPatternSupport
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import java.util.logging.Logger

class BannerStorageMenuController(
    private val storageService: BannerStorageService,
    private val logger: Logger
) {
    fun buildEditClickListener(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        onRefresh: () -> Unit
    ): (ClickEvent) -> Boolean {
        return { event ->
            if (event.rawSlot in 0 until pageSize) {
                handleTopInventoryClick(player, session, pageSize, event, onRefresh)
            } else {
                handlePlayerInventoryClick(player, session, pageSize, event, onRefresh)
            }
        }
    }

    private fun handleTopInventoryClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        event: ClickEvent,
        onRefresh: () -> Unit
    ): Boolean {
        val clickedSlot = event.rawSlot
        val clickedItem = storageService.pageSlice(session.layout, session.page, pageSize)[clickedSlot]
        val cursor = player.itemOnCursor
        event.handle.isCancelled = true

        when {
            MenuUiSupport.isDropClick(event) -> {
                handleTopDropClick(player, session, pageSize, clickedSlot, clickedItem, cursor, event)
            }

            event.isShiftLeft -> {
                handleTopShiftLeftClick(player, session, pageSize, clickedSlot, clickedItem, cursor, event)
            }

            event.isLeft -> {
                handleTopLeftClick(player, session, pageSize, clickedSlot, clickedItem, cursor, event)
            }

            else -> {
                // Other click types are ignored in edit mode top area.
                logEditClickDecision(event, cursor, clickedItem, "ignored_click_type")
            }
        }
        onRefresh()
        return false
    }

    private fun handleTopLeftClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        clickedSlot: Int,
        clickedItem: ItemStack?,
        cursor: ItemStack?,
        event: ClickEvent
    ) {
        if (!isEmpty(cursor) && !BannerPatternSupport.isBanner(cursor)) {
            logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "left_blocked_non_banner_cursor")
            return
        }

        val targetHasBanner = clickedItem != null
        val limitSnapshot = storageService.limitSnapshot(player, session.layout)
        if (!targetHasBanner && !isEmpty(cursor) && limitSnapshot.limit >= 0 && limitSnapshot.current >= limitSnapshot.limit) {
            logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "left_blocked_limit_reached")
            return
        }

        when {
            clickedItem != null && !isEmpty(cursor) -> {
                val cursorItem = cursor ?: return
                storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, cursorItem.clone())
                player.setItemOnCursor(clickedItem)
                logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "left_swap_slot_with_cursor")
            }

            clickedItem != null && isEmpty(cursor) -> {
                storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, null)
                player.setItemOnCursor(clickedItem)
                logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "left_pickup_from_slot_to_cursor")
            }

            clickedItem == null && !isEmpty(cursor) -> {
                val cursorItem = cursor ?: return
                storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, cursorItem.clone())
                player.setItemOnCursor(null)
                logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "left_place_cursor_into_empty_slot")
            }

            else -> logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "left_noop_empty_slot_and_empty_cursor")
        }
    }

    private fun handleTopShiftLeftClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        clickedSlot: Int,
        clickedItem: ItemStack?,
        cursor: ItemStack?,
        event: ClickEvent
    ) {
        if (clickedItem != null) {
            val moveCandidate = clickedItem.clone()
            val remainder = PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, moveCandidate)
            if (remainder <= 0) {
                storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, null)
                logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "shift_move_to_inventory_full_success")
            } else {
                storageService.setPageSlot(
                    session.layout,
                    session.page,
                    pageSize,
                    clickedSlot,
                    clickedItem.clone().apply { amount = remainder }
                )
                logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "shift_move_to_inventory_partial_remainder")
            }
            return
        }

        if (isEmpty(cursor) || !BannerPatternSupport.isBanner(cursor)) {
            logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "shift_noop_empty_slot_invalid_or_empty_cursor")
            return
        }
        val cursorItem = cursor ?: return
        val limitSnapshot = storageService.limitSnapshot(player, session.layout)
        if (limitSnapshot.limit >= 0 && limitSnapshot.current >= limitSnapshot.limit) {
            logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "shift_blocked_limit_reached")
            return
        }
        storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, cursorItem.clone())
        player.setItemOnCursor(null)
        logEditClickDecision(event = event, cursor = cursor, clickedItem = clickedItem, action = "shift_place_cursor_into_empty_slot")
    }

    private fun handleTopDropClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        clickedSlot: Int,
        clickedItem: ItemStack?,
        cursor: ItemStack?,
        event: ClickEvent
    ) {
        if (clickedItem == null || !isEmpty(cursor)) {
            logEditClickDecision(event, cursor, clickedItem, "drop_noop_empty_slot_or_non_empty_cursor")
            return
        }

        val dropAmount = if (event.type == ClickType.CONTROL_DROP) clickedItem.amount else 1
        val copy = clickedItem.clone().apply { amount = dropAmount }
        val drop = player.world.dropItem(player.location.clone().add(0.0, 1.0, 0.0), copy)
        drop.velocity = player.eyeLocation.direction.normalize().multiply(0.3).add(Vector(0.0, 0.1, 0.0))

        val leftInSlot = clickedItem.amount - dropAmount
        if (leftInSlot > 0) {
            storageService.setPageSlot(
                session.layout,
                session.page,
                pageSize,
                clickedSlot,
                clickedItem.clone().apply { amount = leftInSlot }
            )
        } else {
            storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, null)
        }
        logEditClickDecision(event, cursor, clickedItem, "drop_from_slot_to_world_amount_$dropAmount")
    }

    private fun handlePlayerInventoryClick(
        player: Player,
        session: BannerStorageSession,
        pageSize: Int,
        event: ClickEvent,
        onRefresh: () -> Unit
    ): Boolean {
        if (!(event.isShiftLeft || event.isShiftRight)) {
            return true
        }
        event.handle.isCancelled = true
        val clickedInventory = event.clickedInventory ?: return true
        val clickedItem = event.clickedItem ?: return true
        if (!BannerPatternSupport.isBanner(clickedItem)) {
            logEditClickDecision(event, player.itemOnCursor, clickedItem, "player_shift_blocked_non_banner")
            return false
        }

        val limit = storageService.limitSnapshot(player, session.layout)
        if (limit.limit >= 0 && limit.current >= limit.limit) {
            logEditClickDecision(event, player.itemOnCursor, clickedItem, "player_shift_blocked_limit_reached")
            return false
        }

        val occupiedPageSlots = storageService.pageSlice(session.layout, session.page, pageSize)
        val targetSlot = (0 until pageSize).firstOrNull { it !in occupiedPageSlots } ?: return false

        storageService.setPageSlot(session.layout, session.page, pageSize, targetSlot, clickedItem.clone())
        clickedInventory.setItem(event.slot, null)
        logEditClickDecision(event, player.itemOnCursor, clickedItem, "player_shift_move_inventory_to_storage_slot_$targetSlot")
        onRefresh()
        return false
    }

    private fun isEmpty(item: ItemStack?): Boolean = item == null || item.type.isAir || item.amount <= 0

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
}
