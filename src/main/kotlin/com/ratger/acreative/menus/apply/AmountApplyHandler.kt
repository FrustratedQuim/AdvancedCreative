package com.ratger.acreative.menus.apply

import com.ratger.acreative.menus.ItemEditSession
import org.bukkit.entity.Player
import kotlin.math.absoluteValue

class AmountApplyHandler : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.AMOUNT

    private val presets = listOf("1", "8", "16", "32", "64")

    override fun apply(player: Player, session: ItemEditSession, rawValue: String): ApplyExecutionResult {
        val parsed = rawValue.toIntOrNull() ?: return ApplyExecutionResult.InvalidValue
        val normalized = parsed.absoluteValue
        if (normalized == 0 || normalized > 99) {
            return ApplyExecutionResult.InvalidValue
        }

        session.editableItem.amount = normalized
        return ApplyExecutionResult.Success
    }

    override fun suggestions(prefix: String): List<String> {
        return presets.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
