package com.ratger.acreative.menus.edit.apply.text

import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import com.ratger.acreative.menus.edit.ItemEditSession
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

class LoreTextApplyHandler(
    private val textStyleService: ItemTextStyleService
) : EditorApplyHandler {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.LORE_TEXT
    override val inputSpec = ApplyInputSpecs.TEXT

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
