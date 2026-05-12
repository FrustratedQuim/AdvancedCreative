package com.ratger.acreative.menus.edit.apply.core

import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

/**
 * Template for editor /apply actions that accept exactly one argument and then
 * execute domain-specific mutation logic. Keeps argument-count checks and preset
 * suggestions consistent without moving domain behavior into the lifecycle layer.
 */
abstract class OneArgumentEditorApplyHandler<T> : EditorApplyHandler {
    protected open val presets: List<String> = emptyList()

    final override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val value = parseValue(args[0], session) ?: return ApplyExecutionResult.InvalidValue
        return applyValue(player, session, value)
    }

    final override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return suggestions(args[0])
    }

    protected abstract fun parseValue(rawValue: String, session: ItemEditSession): T?

    protected abstract fun applyValue(player: Player, session: ItemEditSession, value: T): ApplyExecutionResult

    protected open fun suggestions(prefix: String): List<String> {
        return presets.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
