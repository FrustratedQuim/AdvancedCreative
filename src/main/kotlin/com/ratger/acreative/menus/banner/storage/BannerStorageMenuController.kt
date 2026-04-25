package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class BannerStorageMenuController(
    private val storageService: BannerStorageService
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

        if (MenuUiSupport.isDropClick(event)) {
            if (clickedItem == null) return true
            val dropAmount = if (event.type == ClickType.CONTROL_DROP) {
                clickedItem.amount
            } else {
                1
            }
            val copy = clickedItem.clone().apply {
                amount = dropAmount
            }
            player.world.dropItemNaturally(player.location.clone().add(0.0, 1.0, 0.0), copy)
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
            onRefresh()
            return true
        }

        if (event.isShiftLeft || event.isShiftRight) {
            if (clickedItem == null) return true
            val moveCandidate = clickedItem.clone()
            val remainder = PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, moveCandidate)
            if (remainder <= 0) {
                storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, null)
            } else {
                storageService.setPageSlot(
                    session.layout,
                    session.page,
                    pageSize,
                    clickedSlot,
                    clickedItem.clone().apply { amount = remainder }
                )
            }
            onRefresh()
            return true
        }

        if (isEmpty(cursor)) {
            return true
        }

        if (!isBanner(cursor)) {
            return true
        }

        val targetHasBanner = clickedItem != null
        val limitSnapshot = storageService.limitSnapshot(player, session.layout)
        if (!targetHasBanner && limitSnapshot.limit >= 0 && limitSnapshot.current >= limitSnapshot.limit) {
            return true
        }

        storageService.setPageSlot(session.layout, session.page, pageSize, clickedSlot, cursor.clone())
        if (targetHasBanner) {
            player.setItemOnCursor(clickedItem)
        } else {
            player.setItemOnCursor(null)
        }
        onRefresh()
        return true
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
        val clickedInventory = event.clickedInventory ?: return true
        val clickedItem = event.clickedItem ?: return true
        if (!isBanner(clickedItem)) {
            return false
        }

        val limit = storageService.limitSnapshot(player, session.layout)
        if (limit.limit >= 0 && limit.current >= limit.limit) {
            return false
        }

        val occupiedPageSlots = storageService.pageSlice(session.layout, session.page, pageSize)
        val targetSlot = (0 until pageSize).firstOrNull { it !in occupiedPageSlots } ?: return false

        storageService.setPageSlot(session.layout, session.page, pageSize, targetSlot, clickedItem.clone())
        clickedInventory.setItem(event.slot, null)
        onRefresh()
        return true
    }

    private fun isEmpty(item: ItemStack?): Boolean = item == null || item.type.isAir || item.amount <= 0

    private fun isBanner(item: ItemStack?): Boolean = item != null && item.type.name.endsWith("_BANNER")
}
