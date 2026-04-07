package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

interface EditorApplyHandler {
    val kind: EditorApplyKind

    fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult

    fun suggestions(args: Array<out String>): List<String>
}

enum class EditorApplyKind {
    ITEM_ID,
    AMOUNT,
    ITEM_MODEL,
    STACK_SIZE,
    ATTRIBUTE,
    EQUIP_SOUND,
    ENCHANTMENT,
    MAX_DURABILITY,
    DAMAGE,
    MINING_SPEED,
    DAMAGE_PER_BLOCK
}

sealed interface ApplyExecutionResult {
    data object Success : ApplyExecutionResult
    data object InvalidValue : ApplyExecutionResult
}
