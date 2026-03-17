package com.ratger.acreative.commands.itemdb

import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class ItemdbManager(private val functionHooker: FunctionHooker) {

    fun showItemInfo(player: Player) {
        val item = player.inventory.itemInMainHand
        val material = item.type
        val itemName = material.name
        val numericId = functionHooker.configManager.getNumericId(itemName)

        functionHooker.messageManager.sendChat(
            player,
            MessageKey.ITEMDB_INFO,
            variables = mapOf(
                "item_name" to itemName,
                "numeric_id" to numericId
            )
        )
    }
}


class ItemdbCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.ITEMDB) {
    override fun handle(player: Player, args: Array<out String>) = hooker.itemdbManager.showItemInfo(player)
}
