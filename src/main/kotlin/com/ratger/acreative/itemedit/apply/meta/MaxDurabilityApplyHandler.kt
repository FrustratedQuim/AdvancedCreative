package com.ratger.acreative.itemedit.apply.meta

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable

class MaxDurabilityApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.MAX_DURABILITY

    private val presets = listOf("5", "25", "150", "500", "10000")

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val value = args[0].toIntOrNull() ?: return ApplyExecutionResult.InvalidValue
        if (value <= 0) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.SetMaxDamage(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) {
            return ApplyExecutionResult.InvalidValue
        }

        val meta = session.editableItem.itemMeta as? Damageable ?: return ApplyExecutionResult.InvalidValue
        meta.setMaxDamage(value)
        session.editableItem.itemMeta = meta
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return presets.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
