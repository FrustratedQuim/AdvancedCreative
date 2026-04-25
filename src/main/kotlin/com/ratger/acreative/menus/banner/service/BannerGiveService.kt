package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.PublishedBannerEntry
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class BannerGiveService(
    private val galleryService: BannerGalleryService
) {
    fun give(
        player: Player,
        sourceItem: ItemStack,
        clickEvent: ClickEvent
    ) {
        giveInternal(player, sourceItem, clickEvent)
    }

    fun give(
        player: Player,
        entry: PublishedBannerEntry,
        clickEvent: ClickEvent,
        trackTake: Boolean = true
    ) {
        giveInternal(player, entry.bannerItem, clickEvent)
        if (trackTake) {
            galleryService.recordTakeIfNeeded(entry, player)
        }
    }

    private fun giveInternal(
        player: Player,
        sourceItem: ItemStack,
        clickEvent: ClickEvent
    ) {
        clickEvent.handle.isCancelled = true

        val item = BannerPatternSupport.normalizeForStorage(sourceItem)?.apply { amount = 1 }
            ?: sourceItem.clone().apply { amount = 1 }
        val isShiftClick = clickEvent.isShiftLeft ||
            clickEvent.isShiftRight ||
            clickEvent.handle.isShiftClick ||
            (player.isSneaking && (clickEvent.isLeft || clickEvent.isRight))

        item.amount = if (clickEvent.isMiddle || clickEvent.type == ClickType.CONTROL_DROP) {
            item.maxStackSize.coerceAtLeast(1)
        } else {
            1
        }

        when {
            isDropClick(clickEvent) -> dropItem(player, item)
            isShiftClick -> {
                val remainingAmount = PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, item)
                if (remainingAmount > 0) {
                    dropItem(player, item.clone().apply { amount = remainingAmount })
                }
            }
            else -> player.setItemOnCursor(item)
        }
    }

    private fun isDropClick(event: ClickEvent): Boolean {
        return event.type == ClickType.DROP || event.type == ClickType.CONTROL_DROP
    }

    private fun dropItem(player: Player, item: ItemStack) {
        if (item.type == Material.AIR || item.amount <= 0) {
            return
        }

        val drop = player.world.dropItem(player.location.clone().add(0.0, 1.0, 0.0), item)
        drop.velocity = player.eyeLocation.direction.normalize().multiply(0.3).add(Vector(0.0, 0.1, 0.0))
    }
}
