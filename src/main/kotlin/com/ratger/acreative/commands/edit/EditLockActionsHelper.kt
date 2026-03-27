@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.commands.edit

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.Lockable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object EditLockActionsHelper {
    private val mini = MiniMessage.miniMessage()

    fun apply(player: Player, action: EditAction, item: ItemStack): EditResult {
        val meta = item.itemMeta as? BlockStateMeta
            ?: return EditResult(false, listOf(mini.deserialize("<red>Item meta не поддерживает block state (BlockStateMeta)")))
        val state = meta.blockState
        val lockable = state as? Lockable
            ?: return EditResult(false, listOf(mini.deserialize("<red>Block state предмета не поддерживает lock API")))

        when (action) {
            EditAction.LockSetFromOffhand -> lockable.setLockItem(player.inventory.itemInOffHand.clone())
            EditAction.LockClear -> lockable.setLockItem(null)
            else -> return EditResult(false, listOf(mini.deserialize("<red>Некорректное lock действие")))
        }

        meta.blockState = state
        item.itemMeta = meta
        return EditResult(true, listOf(mini.deserialize("<green>Изменение применено.")))
    }
}
