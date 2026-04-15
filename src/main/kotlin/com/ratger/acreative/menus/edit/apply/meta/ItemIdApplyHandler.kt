package com.ratger.acreative.menus.edit.apply.meta

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.meta.ItemStackReplacementSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class ItemIdApplyHandler(
    private val parser: EditParsers
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.ITEM_ID

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val rawValue = args[0]
        val material = parser.itemMaterial(rawValue) ?: return ApplyExecutionResult.UnknownValue
        session.editableItem = ItemStackReplacementSupport.replaceItemId(session.editableItem, material)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return parser.materialSuggestions(args[0])
    }
}
