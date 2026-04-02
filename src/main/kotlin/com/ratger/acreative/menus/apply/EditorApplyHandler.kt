package com.ratger.acreative.menus.apply

import com.ratger.acreative.menus.ItemEditSession
import org.bukkit.entity.Player

interface EditorApplyHandler {
    val kind: EditorApplyKind

    fun apply(player: Player, session: ItemEditSession, rawValue: String): ApplyExecutionResult

    fun suggestions(prefix: String): List<String>
}

enum class EditorApplyKind {
    ITEM_ID,
    AMOUNT
}

sealed interface ApplyExecutionResult {
    data object Success : ApplyExecutionResult
    data object InvalidValue : ApplyExecutionResult
}
