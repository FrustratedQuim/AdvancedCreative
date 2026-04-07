package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.tool.ToolComponentSupport
import com.ratger.acreative.itemedit.tool.ToolDamageSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class DamagePerBlockApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.DAMAGE_PER_BLOCK

    private val presets = listOf("0", "1", "5", "25", "100", "10%", "25%", "max")

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        if (!ToolComponentSupport.supportsToolEditing(session.editableItem)) return ApplyExecutionResult.InvalidValue
        val value = ToolDamageSupport.parseDamageLikeValue(args[0], session.editableItem)
            ?: return ApplyExecutionResult.InvalidValue
        if (value < 0) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.ToolSetDamagePerBlock(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) {
            return ApplyExecutionResult.InvalidValue
        }

        if (!ToolComponentSupport.setDamagePerBlock(session.editableItem, value)) {
            return ApplyExecutionResult.InvalidValue
        }
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return presets.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
