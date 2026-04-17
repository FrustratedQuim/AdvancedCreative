package com.ratger.acreative.menus.edit.apply.potion

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
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
        val durationTicks = parseDurationTicks(args.getOrNull(2)) ?: return ApplyExecutionResult.InvalidValue
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

        if (level < 1) return ApplyExecutionResult.InvalidValue

        val amplifier = level - 1

        val action = ItemAction.PotionEffectAdd(
            type = effectType,
            duration = durationTicks,
            amplifier = amplifier,
            ambient = false,
            particles = showParticles,
            icon = showIcon
        )
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return ApplyExecutionResult.InvalidValue

        PotionItemSupport.addEffect(
            session.editableItem,
            PotionEffect(effectType, durationTicks, amplifier, false, showParticles, showIcon)
        )
        return ApplyExecutionResult.Success
    }

    private fun parseDurationTicks(raw: String?): Int? {
        if (raw == null) return 30 * 20
        if (raw.equals("inf", ignoreCase = true)) return PotionEffect.INFINITE_DURATION

        val seconds = raw.toIntOrNull() ?: return null
        if (seconds <= 0) return null

        val ticksLong = seconds.toLong() * 20L
        if (ticksLong > Int.MAX_VALUE) return null

        return ticksLong.toInt()
    }

    override fun suggestions(args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> PotionItemSupport.effectSuggestions(args[0])
            2 -> listOf("1", "2", "5", "10").filter { it.startsWith(args[1], ignoreCase = true) }
            3 -> listOf("30", "60", "120", "300", "inf").filter { it.startsWith(args[2], ignoreCase = true) }
            4, 5 -> listOf("true", "false").filter { it.startsWith(args.last(), ignoreCase = true) }
            else -> emptyList()
        }
    }
}
