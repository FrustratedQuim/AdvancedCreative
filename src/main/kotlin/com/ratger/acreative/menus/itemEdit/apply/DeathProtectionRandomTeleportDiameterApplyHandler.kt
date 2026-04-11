package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.EffectActionSpec
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class DeathProtectionRandomTeleportDiameterApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.DEATH_PROTECTION_RANDOM_TELEPORT_DIAMETER

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val diameter = args[0].toFloatOrNull() ?: return ApplyExecutionResult.InvalidValue

        val action = ItemAction.DeathProtectionEffectAdd(EffectActionSpec.TeleportRandomly(diameter))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) return ApplyExecutionResult.InvalidValue

        DeathProtectionMenuSupport.setRandomTeleportDiameter(session.editableItem, diameter)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        val presets = listOf("5", "10", "15", "50", "100")
        return presets.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
