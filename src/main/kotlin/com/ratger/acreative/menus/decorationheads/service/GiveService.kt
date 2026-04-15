package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.edit.head.HeadTextureMutationSupport
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class GiveService(
    private val headTextureMutationSupport: HeadTextureMutationSupport,
    private val recentService: RecentService
) {
    fun give(player: Player, entry: Entry, clickEvent: ClickEvent, trackRecent: Boolean) {
        clickEvent.handle.isCancelled = true

        val item = ItemStack(Material.PLAYER_HEAD)
        val result = headTextureMutationSupport.applyFromTextureValue(item, entry.textureValue)
        if (result !is HeadTextureMutationSupport.MutationResult.Success) return

        val isShiftClick = clickEvent.isShiftLeft ||
            clickEvent.isShiftRight ||
            clickEvent.handle.isShiftClick ||
            (player.isSneaking && (clickEvent.isLeft || clickEvent.isRight))
        val amount = if (clickEvent.isMiddle || clickEvent.type == ClickType.CONTROL_DROP) 64 else 1
        item.amount = amount

        when {
            isDropClick(clickEvent) -> dropItem(player, item)
            isShiftClick -> {
                val targetSlot = findPreferredShiftTargetSlot(player.inventory)
                if (targetSlot != null) {
                    player.inventory.setItem(targetSlot, item)
                } else {
                    dropItem(player, item)
                }
            }
            else -> player.setItemOnCursor(item)
        }

        if (trackRecent) {
            recentService.push(player.uniqueId, entry)
        }
    }

    private fun findPreferredShiftTargetSlot(inventory: PlayerInventory): Int? {
        for (slot in 8 downTo 0) {
            if (isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        for (slot in 35 downTo 9) {
            if (isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        return null
    }

    private fun isDropClick(event: ClickEvent): Boolean {
        return event.type == ClickType.DROP || event.type == ClickType.CONTROL_DROP
    }

    private fun isEmpty(item: ItemStack?): Boolean {
        return item == null || item.type == Material.AIR || item.amount <= 0
    }

    private fun dropItem(player: Player, item: ItemStack) {
        val drop = player.world.dropItem(player.location.clone().add(0.0, 1.0, 0.0), item)
        drop.velocity = player.eyeLocation.direction.normalize().multiply(0.3).add(org.bukkit.util.Vector(0.0, 0.1, 0.0))
    }
}
