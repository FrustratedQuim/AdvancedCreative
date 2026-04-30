package com.ratger.acreative.paint.artwork

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.edit.map.MapItemSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.paint.model.PaintCanvasBounds
import com.ratger.acreative.paint.model.PaintCanvasCell
import com.ratger.acreative.paint.model.PaintSession
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.inventory.meta.BlockStateMeta

class PaintArtworkService(
    private val hooker: FunctionHooker,
    private val parser: MiniMessageParser
) {
    fun giveResult(player: Player, session: PaintSession) {
        val cells = session.cellsSortedTopLeft()
        if (cells.size <= 1) {
            val only = cells.firstOrNull() ?: return
            giveSingleMapItem(player, createArtworkMapItem(only.mapId, null, null, player.name, session.seriesCode))
            return
        }
        giveItem(player, createArtworkShulker(cells, player.name, session.seriesCode))
    }

    private fun createArtworkMapItem(
        mapId: Int,
        rowNumber: Int?,
        partNumber: Int?,
        author: String,
        seriesCode: String
    ): BukkitItemStack {
        val item = BukkitItemStack(Material.FILLED_MAP)
        MapItemSupport.resolveMapView(mapId)?.let { mapView ->
            MapItemSupport.setMapView(item, mapView)
        }
        item.editMeta { meta ->
            val name = if (rowNumber == null || partNumber == null) {
                "<!i><#FFD700>Рисунок"
            } else {
                "<!i><#FFD700>Строка $rowNumber <#C7A300>[<#FFF3E0>Часть $partNumber<#C7A300>]"
            }
            meta.displayName(parser.parse(name))
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            meta.lore(
                listOf(
                    "<!i><#FFD700>▍ <#FFE68A>Автор: <#FFF3E0>$author",
                    "<!i><#FFD700>▍ <#FFE68A>Серия: <#FFF3E0>$seriesCode"
                ).map(parser::parse)
            )
        }
        return item
    }

    private fun createArtworkShulker(
        cells: List<PaintCanvasCell>,
        author: String,
        seriesCode: String
    ): BukkitItemStack {
        val bounds = PaintCanvasBounds.from(cells.map { it.point }) ?: return BukkitItemStack(Material.WHITE_SHULKER_BOX)
        val shulker = BukkitItemStack(Material.WHITE_SHULKER_BOX)
        shulker.editMeta { rawMeta ->
            rawMeta.displayName(parser.parse(SHULKER_TITLE))
            rawMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            val meta = rawMeta as? BlockStateMeta ?: return@editMeta
            val box = meta.blockState as? ShulkerBox ?: return@editMeta
            box.customName(parser.parse(SHULKER_TITLE))

            val contents = arrayOfNulls<BukkitItemStack>(27)
            cells.forEach { cell ->
                val rowNumber = cell.point.y - bounds.minY + 1
                val partNumber = cell.point.x - bounds.minX + 1
                val rowStart = SHULKER_ROW_STARTS.getOrElse(rowNumber - 1) { 0 }
                val slot = rowStart + (partNumber - 1)
                if (slot in contents.indices) {
                    contents[slot] = createArtworkMapItem(cell.mapId, rowNumber, partNumber, author, seriesCode)
                }
            }
            box.inventory.contents = contents
            meta.blockState = box
        }
        return shulker
    }

    private fun giveSingleMapItem(player: Player, item: BukkitItemStack) {
        val handItem = player.inventory.itemInMainHand
        if (handItem.type == Material.AIR || handItem.amount <= 0) {
            player.inventory.setItemInMainHand(item)
            return
        }
        giveItem(player, item)
    }

    private fun giveItem(player: Player, item: BukkitItemStack) {
        val remainingAmount = PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, item)
        if (remainingAmount > 0) {
            val dropped = player.world.dropItem(
                player.location.clone().add(0.0, 1.0, 0.0),
                item.clone().apply { amount = remainingAmount }
            )
            hooker.utils.getPlayersWithHides().forEach { hider ->
                hooker.hideManager.hideDroppedItem(hider, dropped, player)
            }
        }
    }

    private companion object {
        const val SHULKER_TITLE = "<shadow:#101010:1><!i><#EDC800>Содержимое рисунка</shadow>"
        val SHULKER_ROW_STARTS = listOf(0, 9, 18, 5)
    }
}
