package com.ratger.acreative.menus.edit.apply.deathprotection

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.api.EffectActionSpec
import com.ratger.acreative.menus.edit.api.EffectApplyEntrySpec
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectInputSupport
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class DeathProtectionApplyEffectAddApplyHandler(
    private val parser: EditParsers,
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.DEATH_PROTECTION_APPLY_EFFECT_ADD

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.isEmpty() || args.size > 6) return ApplyExecutionResult.InvalidValue

        val effectType = parser.effectFromToken(args[0]) ?: return ApplyExecutionResult.UnknownValue
        val level = VisualEffectInputSupport.parseLevel(args.getOrNull(1)) ?: 1
        val durationSeconds = VisualEffectInputSupport.parseDurationSeconds(args.getOrNull(2)) ?: return ApplyExecutionResult.InvalidValue
        val durationTicks = VisualEffectInputSupport.visibleTicksFromDurationSeconds(durationSeconds)
        val probabilityPercent = VisualEffectInputSupport.parseProbabilityPercent(args.getOrNull(3)) ?: return ApplyExecutionResult.InvalidValue
        val probability = probabilityPercent / 100f
        val showParticlesInput = args.getOrNull(4)
        val showParticles = if (showParticlesInput == null) true else parser.parseBooleanStrict(showParticlesInput) ?: return ApplyExecutionResult.InvalidValue
        val showIconInput = args.getOrNull(5)
        val showIcon = if (showIconInput == null) true else parser.parseBooleanStrict(showIconInput) ?: return ApplyExecutionResult.InvalidValue

        val entry = EffectApplyEntrySpec(
            type = effectType,
            duration = durationTicks,
            amplifier = level - 1,
            showParticles = showParticles,
            showIcon = showIcon
        )

        val action = ItemAction.DeathProtectionEffectAdd(EffectActionSpec.ApplyEffects(probability, listOf(entry)))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return ApplyExecutionResult.InvalidValue

        DeathProtectionMenuSupport.addApplyEffect(session.editableItem, probability, entry)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> PotionItemSupport.effectSuggestions(args[0])
            2 -> listOf("1", "2", "5", "10").filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> listOf("30", "60", "120", "300", "inf").filter { it.startsWith(args[2], ignoreCase = true) }
            4 -> listOf("15%", "50%", "75%", "100%").filter { it.startsWith(args[3], ignoreCase = true) }
            5, 6 -> listOf("true", "false").filter { it.startsWith(args.last(), ignoreCase = true) }
            else -> emptyList()
        }
    }
}
