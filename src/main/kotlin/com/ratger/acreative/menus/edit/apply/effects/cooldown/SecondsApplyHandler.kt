package com.ratger.acreative.menus.edit.apply.effects.cooldown

import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.preset.ApplyPresetCatalog
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.usecooldown.UseCooldownSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class SecondsApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.USE_COOLDOWN_SECONDS
    override val inputSpec = ApplyInputSpecs.AMOUNT

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val raw = args[0].trim()
        if (raw.isEmpty()) return ApplyExecutionResult.InvalidValue
        val parsed = raw.toFloatOrNull() ?: return ApplyExecutionResult.InvalidValue
        if (!parsed.isFinite() || parsed <= 0f) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.SetUseCooldown(parsed, UseCooldownSupport.group(session.editableItem))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) {
            return ApplyExecutionResult.InvalidValue
        }

        UseCooldownSupport.setSeconds(session.editableItem, parsed)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return ApplyPresetCatalog.getPresets(kind).filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
