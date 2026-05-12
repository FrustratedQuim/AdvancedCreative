package com.ratger.acreative.menus.edit.apply.effects.consumable

import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.preset.ApplyPresetCatalog
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.effects.ConsumableComponentSupport
import com.ratger.acreative.menus.edit.effects.EdibleMenuSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class ConsumeSecondsApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.CONSUMABLE_CONSUME_SECONDS
    override val inputSpec = ApplyInputSpecs.CONSUME_SECONDS

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val raw = args[0].trim()
        if (raw.isEmpty()) return ApplyExecutionResult.InvalidValue
        val seconds = raw.toFloatOrNull() ?: return ApplyExecutionResult.InvalidValue
        if (!seconds.isFinite() || seconds <= 0f) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.ConsumableConsumeSeconds(seconds)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return ApplyExecutionResult.InvalidValue

        EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
        ConsumableComponentSupport.setConsumeSeconds(session.editableItem, seconds)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return ApplyPresetCatalog.getPresets(kind).filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
