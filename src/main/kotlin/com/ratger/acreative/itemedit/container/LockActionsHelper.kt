@file:Suppress("UnstableApiUsage") // Experimental Lockable

package com.ratger.acreative.itemedit.container

import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemResult
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.Lockable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object LockActionsHelper {
    private val mini = MiniMessage.miniMessage()

    fun apply(player: Player, action: ItemAction, item: ItemStack): ItemResult {
        val meta = item.itemMeta as? BlockStateMeta
            ?: return ItemResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает block state (BlockStateMeta)")))
        val state = meta.blockState
        val lockable = state as? Lockable
            ?: return ItemResult(false, listOf(mini.deserialize("<red>Block state предмета не поддерживает lock API")))

        when (action) {
            ItemAction.LockSetFromOffhand -> lockable.setLockItem(player.inventory.itemInOffHand.clone())
            ItemAction.LockClear -> lockable.setLockItem(null)
            else -> return ItemResult(false, listOf(mini.deserialize("<red>Некорректное lock действие")))
        }

        meta.blockState = state
        item.itemMeta = meta
        return ItemResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }
}
