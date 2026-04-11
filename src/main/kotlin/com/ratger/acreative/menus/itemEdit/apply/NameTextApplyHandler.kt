package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

class NameTextApplyHandler(
    private val textStyleService: ItemTextStyleService
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.NAME_TEXT

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        val raw = args.joinToString(" ").trim()
        if (raw.isBlank()) {
            return ApplyExecutionResult.InvalidValue
        }
        var updated = textStyleService.parseMiniMessage(raw).decoration(TextDecoration.ITALIC, false)
        updated = textStyleService.applyOrderedColors(updated, session.orderedNameColors)
        updated = textStyleService.applyShadow(updated, textStyleService.resolveShadowColor(session.nameShadowKey))
        textStyleService.setCustomName(session.editableItem, updated)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
