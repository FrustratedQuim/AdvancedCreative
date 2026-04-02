package com.ratger.acreative.menus.apply

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.meta.ItemStackReplacementSupport
import com.ratger.acreative.menus.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player

class ItemIdApplyHandler(
    private val parser: EditParsers
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.ITEM_ID

    private val materialSuggestions = Material.entries
        .asSequence()
        .filter { it.isItem && it != Material.AIR }
        .map { it.key.key }
        .sorted()
        .toList()

    override fun apply(player: Player, session: ItemEditSession, rawValue: String): ApplyExecutionResult {
        val material = parser.material(rawValue) ?: return ApplyExecutionResult.InvalidValue
        session.editableItem = ItemStackReplacementSupport.replaceItemId(session.editableItem, material)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(prefix: String): List<String> {
        return materialSuggestions.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
