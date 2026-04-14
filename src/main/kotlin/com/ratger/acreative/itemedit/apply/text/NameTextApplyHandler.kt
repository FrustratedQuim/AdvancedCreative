package com.ratger.acreative.itemedit.apply.text

import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
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
        var updated = textStyleService
            .parseInputText(raw, ItemTextStyleService.TextInputMode.LITERAL_ESCAPED)
            .decoration(TextDecoration.ITALIC, false)
        updated = textStyleService.applyOrderedColors(updated, session.orderedNameColors, player.locale())
        updated = textStyleService.applyShadow(updated, textStyleService.resolveShadowColor(session.nameShadowKey))
        textStyleService.setCustomName(session.editableItem, updated)
        session.usesVanillaNameBase = false
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
