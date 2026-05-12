package com.ratger.acreative.menus.edit.apply.tool

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.edit.apply.core.OneArgumentEditorApplyHandler
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.tool.ToolDamageSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable

class DamageApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : OneArgumentEditorApplyHandler<Int>() {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.DAMAGE
    override val inputSpec = ApplyInputSpecs.AMOUNT

    override fun parseValue(rawValue: String, session: ItemEditSession): Int? {
        return ToolDamageSupport.parseDamageLikeValue(rawValue, session.editableItem)?.takeIf { it >= 0 }
    }

    override fun applyValue(player: Player, session: ItemEditSession, value: Int): ApplyExecutionResult {
        val action = ItemAction.SetDamage(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) {
            return ApplyExecutionResult.InvalidValue
        }

        val meta = session.editableItem.itemMeta as? Damageable ?: return ApplyExecutionResult.InvalidValue
        meta.damage = value
        session.editableItem.itemMeta = meta
        return ApplyExecutionResult.Success
    }
}
