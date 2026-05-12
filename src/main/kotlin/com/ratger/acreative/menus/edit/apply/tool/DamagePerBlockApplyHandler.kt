package com.ratger.acreative.menus.edit.apply.tool

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.edit.apply.core.OneArgumentEditorApplyHandler
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.tool.ToolComponentSupport
import com.ratger.acreative.menus.edit.tool.ToolDamageSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.entity.Player

class DamagePerBlockApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : OneArgumentEditorApplyHandler<Int>() {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.DAMAGE_PER_BLOCK
    override val inputSpec = ApplyInputSpecs.AMOUNT
    override val presets: List<String> = listOf("0", "1", "5", "25", "100", "10%", "25%", "max")

    override fun parseValue(rawValue: String, session: ItemEditSession): Int? {
        return ToolDamageSupport.parseDamageLikeValue(rawValue, session.editableItem)?.takeIf { it >= 0 }
    }

    override fun applyValue(player: Player, session: ItemEditSession, value: Int): ApplyExecutionResult {
        val action = ItemAction.ToolSetDamagePerBlock(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) {
            return ApplyExecutionResult.InvalidValue
        }

        if (!ToolComponentSupport.setDamagePerBlock(session.editableItem, value)) {
            return ApplyExecutionResult.InvalidValue
        }
        return ApplyExecutionResult.Success
    }
}
