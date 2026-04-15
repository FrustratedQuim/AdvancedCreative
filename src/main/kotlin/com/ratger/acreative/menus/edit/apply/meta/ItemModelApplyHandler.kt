package com.ratger.acreative.menus.edit.apply.meta

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class ItemModelApplyHandler(
    private val parser: EditParsers,
    private val itemIdSuggestions: (Array<out String>) -> List<String>
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.ITEM_MODEL

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val rawValue = args[0]
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

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()

        val prefix = args[0]
        val values = itemIdSuggestions(args)
        val air = if ("air".startsWith(prefix.removePrefix("minecraft:"), ignoreCase = true)) listOf("air") else emptyList()
        return (values + air).distinct()
    }
}
