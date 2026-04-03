package com.ratger.acreative.menus.apply

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.menus.ItemEditSession
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class ItemModelApplyHandler(
    private val parser: EditParsers,
    private val itemIdSuggestions: (String) -> List<String>
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.ITEM_MODEL

    override fun apply(player: Player, session: ItemEditSession, rawValue: String): ApplyExecutionResult {
        val candidate = rawValue.trim().lowercase()
        if (candidate.isEmpty()) return ApplyExecutionResult.InvalidValue

        val material = parser.material(candidate)
        val meta = session.editableItem.itemMeta ?: return ApplyExecutionResult.InvalidValue
        val normalized = material?.key?.asString() ?: candidate
        val key = NamespacedKey.fromString(normalized) ?: return ApplyExecutionResult.InvalidValue
        meta.itemModel = key

        session.editableItem.itemMeta = meta
        return ApplyExecutionResult.Success
    }

    override fun suggestions(prefix: String): List<String> = itemIdSuggestions(prefix)
}
