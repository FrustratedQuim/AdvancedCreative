package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.edit.head.HeadTextureMutationSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class GiveService(
    private val headTextureMutationSupport: HeadTextureMutationSupport,
    private val parser: MiniMessageParser,
    private val recentService: RecentService
) {
    fun give(
        player: Player,
        entry: Entry,
        categoryName: String,
        clickEvent: ClickEvent,
        trackRecent: Boolean,
        onRecentCountUpdated: ((Int) -> Unit)? = null
    ) {
        clickEvent.handle.isCancelled = true

        val item = ItemStack(Material.PLAYER_HEAD)
        val result = headTextureMutationSupport.applyFromTextureValue(item, entry.textureValue)
        if (result !is HeadTextureMutationSupport.MutationResult.Success) return
        (item.itemMeta as? SkullMeta)?.let { skull ->
            skull.displayName(parser.parse("<!i><#FFD700>${entry.name}"))
            skull.lore(
                listOf(
                    parser.parse("<!i><#FFD700>▍ <#FFE68A>Категория: <#FFF3E0>$categoryName")
                )
            )
            item.itemMeta = skull
        }

        val isShiftClick = clickEvent.isShiftLeft ||
            clickEvent.isShiftRight ||
            clickEvent.handle.isShiftClick ||
            (player.isSneaking && (clickEvent.isLeft || clickEvent.isRight))
        val giveAmount = if (clickEvent.isMiddle || clickEvent.type == ClickType.CONTROL_DROP) 64 else 1
        item.amount = giveAmount

        when {
            isDropClick(clickEvent) -> dropItem(player, item)
            isShiftClick -> {
                val remainingAmount = PlayerInventoryTransferSupport.tryAddToExistingStacks(player.inventory, item)
                if (remainingAmount > 0) {
                    val remainingItem = item.clone().also { clonedItem ->
                        clonedItem.amount = remainingAmount
                    }
                    val targetSlot = PlayerInventoryTransferSupport.findPreferredEmptySlot(player.inventory)
                    if (targetSlot != null) {
                        player.inventory.setItem(targetSlot, remainingItem)
                    } else {
                        dropItem(player, remainingItem)
                    }
                }
            }
            else -> player.setItemOnCursor(item)
        }

        if (trackRecent) {
            recentService.push(player.uniqueId, entry, onRecentCountUpdated)
        }
    }


    private fun isDropClick(event: ClickEvent): Boolean {
        return event.type == ClickType.DROP || event.type == ClickType.CONTROL_DROP
    }

    private fun dropItem(player: Player, item: ItemStack) {
        val drop = player.world.dropItem(player.location.clone().add(0.0, 1.0, 0.0), item)
        drop.velocity = player.eyeLocation.direction.normalize().multiply(0.3).add(org.bukkit.util.Vector(0.0, 0.1, 0.0))
    }
}
