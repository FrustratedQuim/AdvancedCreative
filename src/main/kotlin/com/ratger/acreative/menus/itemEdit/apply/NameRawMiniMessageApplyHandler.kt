package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class NameRawMiniMessageApplyHandler(
    private val textStyleService: ItemTextStyleService
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.NAME_RAW_MINIMESSAGE

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        val raw = args.joinToString(" ").trim()
        if (raw.isBlank()) {
            return ApplyExecutionResult.InvalidValue
        }

        return runCatching {
            textStyleService.setCustomNameRawMiniMessage(session.editableItem, raw)
            session.rawMiniMessageNameInput = raw
            session.usesVanillaNameBase = false
            ApplyExecutionResult.Success
        }.getOrElse {
            ApplyExecutionResult.InvalidValue
        }
    }

    override fun suggestions(args: Array<out String>): List<String> = emptyList()
}
