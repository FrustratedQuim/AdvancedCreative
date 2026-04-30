package com.ratger.acreative.menus.paint

import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.paint.model.PaintSession
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack as BukkitItemStack

class PaintToolInventoryService(
    private val toolKey: NamespacedKey,
    private val parser: MiniMessageParser
) {
    fun prepare(player: Player, session: PaintSession) {
        clear(player)
        player.inventory.storageContents = rotatedLayout(PaintToolCatalog.buildLayout(toolKey, parser, session), session.paletteRotation)
        player.inventory.heldItemSlot = 0
    }

    fun refresh(player: Player, session: PaintSession) {
        val heldSlot = player.inventory.heldItemSlot
        player.inventory.storageContents = rotatedLayout(PaintToolCatalog.buildLayout(toolKey, parser, session), session.paletteRotation)
        player.inventory.heldItemSlot = heldSlot.coerceIn(0, 8)
    }

    fun clear(player: Player) {
        player.inventory.storageContents = arrayOfNulls(36)
        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.extraContents = arrayOfNulls(player.inventory.extraContents.size.coerceAtLeast(1))
    }

    fun resyncHeldToolSlot(player: Player, session: PaintSession) {
        val currentSlot = player.inventory.heldItemSlot
        val currentItem = player.inventory.itemInMainHand
        val itemForSync = if (isWorkTool(currentItem)) {
            currentItem.clone()
        } else {
            rotatedLayout(PaintToolCatalog.buildLayout(toolKey, parser, session), session.paletteRotation)
                .getOrNull(currentSlot)
                ?.clone()
                ?: BukkitItemStack(Material.AIR)
        }
        player.inventory.setItem(currentSlot, itemForSync)
    }

    fun resolve(item: BukkitItemStack?): PaintToolDefinition? = PaintToolCatalog.resolve(item, toolKey)

    fun isWorkTool(item: BukkitItemStack?): Boolean = resolve(item) != null

    private fun rotatedLayout(contents: Array<BukkitItemStack?>, rotation: Int): Array<BukkitItemStack?> {
        val result = contents.copyOf()
        repeat(rotation.coerceIn(0, 3)) {
            val hotbar = result.sliceArray(0..8)
            val second = result.sliceArray(9..17)
            val third = result.sliceArray(18..26)
            val fourth = result.sliceArray(27..35)
            fourth.copyInto(result, 0)
            hotbar.copyInto(result, 9)
            second.copyInto(result, 18)
            third.copyInto(result, 27)
        }
        return result
    }
}
