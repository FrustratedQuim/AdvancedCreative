package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.usecooldown.UseCooldownSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class UseCooldownSecondsApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.USE_COOLDOWN_SECONDS

    private val presets = listOf("0.5", "1", "5")

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val raw = args[0].trim()
        if (raw.isEmpty()) return ApplyExecutionResult.InvalidValue
        val parsed = raw.toFloatOrNull() ?: return ApplyExecutionResult.InvalidValue
        if (!parsed.isFinite() || parsed <= 0f) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.SetUseCooldown(parsed, UseCooldownSupport.group(session.editableItem))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) {
            return ApplyExecutionResult.InvalidValue
        }

        UseCooldownSupport.setSeconds(session.editableItem, parsed)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return presets.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
