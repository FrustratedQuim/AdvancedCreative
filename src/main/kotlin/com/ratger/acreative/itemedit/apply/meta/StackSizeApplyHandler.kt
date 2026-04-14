package com.ratger.acreative.itemedit.apply.meta

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import kotlin.math.absoluteValue
import org.bukkit.entity.Player

class StackSizeApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.STACK_SIZE

    private val presets = listOf("1", "8", "16", "32", "64", "max")

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val rawValue = args[0]
        val normalizedInput = if (rawValue.equals("max", ignoreCase = true)) "99" else rawValue
        val parsed = normalizedInput.toIntOrNull() ?: return ApplyExecutionResult.InvalidValue
        val stackSize = parsed.absoluteValue
        if (stackSize == 0) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.SetMaxStackSize(stackSize)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) {
            return ApplyExecutionResult.InvalidValue
        }

        val meta = session.editableItem.itemMeta ?: return ApplyExecutionResult.InvalidValue
        meta.setMaxStackSize(stackSize)
        session.editableItem.itemMeta = meta
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0]
        return presets.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
