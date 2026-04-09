package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

class PotionEffectAddApplyHandler(
    private val parser: EditParsers,
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.POTION_EFFECT_ADD

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.isEmpty() || args.size > 5) return ApplyExecutionResult.InvalidValue

        val effectType = parser.effectFromToken(args[0]) ?: return ApplyExecutionResult.UnknownValue
        val level = args.getOrNull(1)?.toIntOrNull() ?: 1
        val durationSeconds = args.getOrNull(2)?.toIntOrNull() ?: 30
        val showParticlesInput = args.getOrNull(3)
        val showIconInput = args.getOrNull(4)
        val showParticles = when {
            showParticlesInput == null -> true
            else -> parser.parseBooleanStrict(showParticlesInput) ?: return ApplyExecutionResult.InvalidValue
        }
        val showIcon = when {
            showIconInput == null -> true
            else -> parser.parseBooleanStrict(showIconInput) ?: return ApplyExecutionResult.InvalidValue
        }

        if (level < 1 || durationSeconds <= 0) return ApplyExecutionResult.InvalidValue

        val amplifier = level - 1
        val ticksLong = durationSeconds.toLong() * 20L
        if (ticksLong > Int.MAX_VALUE) return ApplyExecutionResult.InvalidValue
        val durationTicks = ticksLong.toInt()

        val action = ItemAction.PotionEffectAdd(
            type = effectType,
            duration = durationTicks,
            amplifier = amplifier,
            ambient = false,
            particles = showParticles,
            icon = showIcon
        )
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) return ApplyExecutionResult.InvalidValue

        PotionItemSupport.addEffect(
            session.editableItem,
            PotionEffect(effectType, durationTicks, amplifier, false, showParticles, showIcon)
        )
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> PotionItemSupport.effectSuggestions(args[0])
            2 -> listOf("1", "2", "5", "10").filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> listOf("30", "60", "120", "300").filter { it.startsWith(args[2], ignoreCase = true) }
            4, 5 -> listOf("true", "false").filter { it.startsWith(args.last(), ignoreCase = true) }
            else -> emptyList()
        }
    }
}
