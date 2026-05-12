package com.ratger.acreative.menus.edit.apply.meta

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.edit.apply.core.OneArgumentEditorApplyHandler
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable

class MaxDurabilityApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : OneArgumentEditorApplyHandler<Int>() {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.MAX_DURABILITY
    override val inputSpec = ApplyInputSpecs.AMOUNT
    override val presets: List<String> = listOf("5", "25", "150", "500", "10000")

    override fun parseValue(rawValue: String, session: ItemEditSession): Int? {
        return rawValue.toIntOrNull()?.takeIf { it > 0 }
    }

    override fun applyValue(player: Player, session: ItemEditSession, value: Int): ApplyExecutionResult {
        val action = ItemAction.SetMaxDamage(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) {
            return ApplyExecutionResult.InvalidValue
        }

        val meta = session.editableItem.itemMeta as? Damageable ?: return ApplyExecutionResult.InvalidValue
        meta.setMaxDamage(value)
        session.editableItem.itemMeta = meta
        return ApplyExecutionResult.Success
    }
}
