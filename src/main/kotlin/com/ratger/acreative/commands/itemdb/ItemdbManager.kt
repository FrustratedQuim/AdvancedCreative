package com.ratger.acreative.commands.itemdb

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class ItemdbManager(private val functionHooker: FunctionHooker) {

    fun showItemInfo(player: Player) {
        val item = player.inventory.itemInMainHand
        val material = item.type
        val itemName = material.name
        val numericId = functionHooker.configManager.getNumericId(itemName)

        functionHooker.messageManager.sendMiniMessage(
            player,
            key = "itemdb-info",
            variables = mapOf(
                "item_name" to itemName,
                "numeric_id" to numericId
            )
        )
    }
}