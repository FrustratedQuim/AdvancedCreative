package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.restrictions.ItemRestrictionSupport
import com.ratger.acreative.itemedit.restrictions.RestrictionMode
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class RestrictionBlockApplyHandler(
    override val kind: EditorApplyKind,
    private val mode: RestrictionMode,
    private val parser: EditParsers
) : EditorApplyHandler {
    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue

        val material = parser.blockItemMaterial(args[0]) ?: return ApplyExecutionResult.UnknownValue

        ItemRestrictionSupport.add(session.editableItem, mode, material.key)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return parser.blockItemSuggestions(args[0])
    }
}
