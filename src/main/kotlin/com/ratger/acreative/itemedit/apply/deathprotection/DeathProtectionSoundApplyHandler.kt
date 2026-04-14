package com.ratger.acreative.itemedit.apply.deathprotection

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.EffectActionSpec
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class DeathProtectionSoundApplyHandler(
    private val parser: EditParsers,
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.DEATH_PROTECTION_SOUND

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue

        val key = parser.parseSoundKey(args[0]) ?: return ApplyExecutionResult.InvalidValue
        val action = ItemAction.DeathProtectionEffectAdd(EffectActionSpec.PlaySound(key))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) return ApplyExecutionResult.InvalidValue

        DeathProtectionMenuSupport.setSound(session.editableItem, key)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return parser.soundSuggestions(args[0])
    }
}
