package com.ratger.acreative.menus.edit.apply.meta

import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.ItemEditSession
import kotlin.math.absoluteValue
import org.bukkit.entity.Player

class AmountApplyHandler : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.AMOUNT

    private val presets = listOf("1", "8", "16", "32", "64")

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val rawValue = args[0]
        val parsed = rawValue.toIntOrNull() ?: return ApplyExecutionResult.InvalidValue
        val normalized = parsed.absoluteValue
        if (normalized == 0 || normalized > 99) {
            return ApplyExecutionResult.InvalidValue
        }

        session.editableItem.amount = normalized
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0]
        return presets.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
