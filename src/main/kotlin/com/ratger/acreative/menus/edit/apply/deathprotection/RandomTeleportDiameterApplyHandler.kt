package com.ratger.acreative.menus.edit.apply.deathprotection

import com.ratger.acreative.menus.edit.apply.preset.ApplyPresetCatalog
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.api.EffectActionSpec
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import org.bukkit.entity.Player

class RandomTeleportDiameterApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.DEATH_PROTECTION_RANDOM_TELEPORT_DIAMETER
    override val inputSpec = ApplyInputSpecs.AMOUNT

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val diameter = args[0].toFloatOrNull() ?: return ApplyExecutionResult.InvalidValue

        val action = ItemAction.DeathProtectionEffectAdd(EffectActionSpec.TeleportRandomly(diameter))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return ApplyExecutionResult.InvalidValue

        DeathProtectionMenuSupport.setRandomTeleportDiameter(session.editableItem, diameter)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return ApplyPresetCatalog.getPresets(kind).filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
