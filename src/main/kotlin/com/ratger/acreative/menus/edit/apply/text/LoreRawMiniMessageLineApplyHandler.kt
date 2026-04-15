package com.ratger.acreative.menus.edit.apply.text

import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class LoreRawMiniMessageLineApplyHandler(
    private val textStyleService: ItemTextStyleService
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.LORE_RAW_MINIMESSAGE_LINE

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        val raw = args.joinToString(" ").trim()
        if (raw.isBlank()) {
            return ApplyExecutionResult.InvalidValue
        }

        return runCatching {
            val updatedLines = textStyleService.updateVirtualLoreLine(
                lines = session.virtualLoreRawLines,
                index = session.loreRawFocusIndex,
                rawValue = raw
            )
            session.virtualLoreRawLines.clear()
            session.virtualLoreRawLines.addAll(updatedLines)
            session.loreRawFocusIndex = session.loreRawFocusIndex.coerceIn(0, session.virtualLoreRawLines.lastIndex)
            textStyleService.setLoreFromVirtualRawLines(session.editableItem, session.virtualLoreRawLines)
            ApplyExecutionResult.Success
        }.getOrElse {
            ApplyExecutionResult.InvalidValue
        }
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
