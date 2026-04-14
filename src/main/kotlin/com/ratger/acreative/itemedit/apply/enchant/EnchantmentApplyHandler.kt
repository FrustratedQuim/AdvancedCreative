package com.ratger.acreative.itemedit.apply.enchant

import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.enchant.EnchantmentSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class EnchantmentApplyHandler : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.ENCHANTMENT

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.isEmpty() || args.size > 2) return ApplyExecutionResult.InvalidValue

        val enchantment = EnchantmentSupport.resolve(args[0]) ?: return ApplyExecutionResult.InvalidValue
        val level = (args.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 127)

        val meta = session.editableItem.itemMeta ?: return ApplyExecutionResult.InvalidValue
        EnchantmentSupport.add(meta, enchantment, level, ignoreLevelRestriction = true)
        session.editableItem.itemMeta = meta
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> EnchantmentSupport.suggestions(args[0])
            2 -> listOf("1", "3", "5", "10", "25").filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
