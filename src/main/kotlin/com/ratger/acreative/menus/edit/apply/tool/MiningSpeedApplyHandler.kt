package com.ratger.acreative.menus.edit.apply.tool

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.api.ToolSpeedScope
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.edit.apply.core.OneArgumentEditorApplyHandler
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.tool.ToolComponentSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.entity.Player

class MiningSpeedApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : OneArgumentEditorApplyHandler<Float>() {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.MINING_SPEED
    override val inputSpec = ApplyInputSpecs.AMOUNT
    override val presets: List<String> = listOf("0", "0.5", "5", "10", "50")

    override fun parseValue(rawValue: String, session: ItemEditSession): Float? = rawValue.toFloatOrNull()

    override fun applyValue(player: Player, session: ItemEditSession, value: Float): ApplyExecutionResult {
        val action = ItemAction.ToolSetDefaultMiningSpeed(value, ToolSpeedScope.INEFFECTIVE_ONLY)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) {
            return ApplyExecutionResult.InvalidValue
        }

        if (!ToolComponentSupport.setMiningSpeed(session.editableItem, value)) {
            return ApplyExecutionResult.InvalidValue
        }
        return ApplyExecutionResult.Success
    }
}
