package com.ratger.acreative.itemedit.container

import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemResult
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object LockActionsHelper {
    private val mini = MiniMessage.miniMessage()

    fun apply(player: Player, action: ItemAction, item: ItemStack): ItemResult {
        if (!LockItemSupport.supports(item)) {
            return ItemResult(false, listOf(mini.deserialize("<red>Эта ветка только для shulker box item")))
        }

        when (action) {
            ItemAction.LockSetFromOffhand -> LockItemSupport.setOrClear(item, player.inventory.itemInOffHand)
            ItemAction.LockClear -> LockItemSupport.clear(item)
            else -> return ItemResult(false, listOf(mini.deserialize("<red>Некорректное lock действие")))
        }

        return ItemResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }
}
