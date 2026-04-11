package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
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
        val existingShadow = textStyleService.lore(session.editableItem).firstOrNull()?.let(textStyleService::detectShadowColor)
        var line = textStyleService.parseMiniMessage(raw).decoration(TextDecoration.ITALIC, false)
        line = textStyleService.applyOrderedColors(line, session.orderedLoreColors)
        line = textStyleService.applyShadow(line, existingShadow)
        textStyleService.setLore(
            session.editableItem,
            listOf(line)
        )
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
