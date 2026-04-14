package com.ratger.acreative.itemedit.apply.text

import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.menus.edit.ItemEditSession
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

class LoreTextApplyHandler(
    private val textStyleService: ItemTextStyleService
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.LORE_TEXT

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        val raw = args.joinToString(" ").trim()
        if (raw.isBlank()) {
            return ApplyExecutionResult.InvalidValue
        }
        var line = textStyleService
            .parseInputText(raw, ItemTextStyleService.TextInputMode.LITERAL_ESCAPED)
            .decoration(TextDecoration.ITALIC, false)
        line = textStyleService.applyOrderedColors(line, session.orderedLoreColors)
        line = textStyleService.applyShadow(line, textStyleService.resolveShadowColor(session.loreShadowKey))
        textStyleService.setLore(
            session.editableItem,
            listOf(line)
        )
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
