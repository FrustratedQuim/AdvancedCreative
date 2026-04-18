package com.ratger.acreative.menus.edit.effects.visual

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.api.EffectActionSpec
import com.ratger.acreative.menus.edit.api.EffectApplyEntrySpec
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.effects.ConsumableComponentSupport
import com.ratger.acreative.menus.edit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.menus.edit.effects.EdibleMenuSupport
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

sealed interface VisualEffectContext {
    val key: VisualEffectContextKey
    val supportsProbability: Boolean

    fun apply(player: Player, session: ItemEditSession, draft: VisualEffectDraft): Boolean
}

class VisualEffectFlowService(
    validationService: ValidationService,
    targetResolver: EditTargetResolver
) {
    private val contexts: Map<VisualEffectContextKey, VisualEffectContext> = listOf(
        PotionVisualEffectContext(validationService, targetResolver),
        ConsumableVisualEffectContext(validationService, targetResolver),
        DeathProtectionVisualEffectContext(validationService, targetResolver)
    ).associateBy { it.key }

    fun begin(session: ItemEditSession, contextKey: VisualEffectContextKey) {
        if (session.visualEffectContext != contextKey) {
            session.visualEffectDraft = VisualEffectDraft()
            session.visualEffectLastTypePage = 0
        }
        session.visualEffectContext = contextKey
    }

    fun resolveType(effectTypeKey: String?): org.bukkit.potion.PotionEffectType? {
        effectTypeKey ?: return null
        return Registry.MOB_EFFECT.get(org.bukkit.NamespacedKey.minecraft(effectTypeKey.lowercase()))
    }

    fun context(session: ItemEditSession): VisualEffectContext? = session.visualEffectContext?.let(contexts::get)

    fun apply(player: Player, session: ItemEditSession): Boolean {
        val context = context(session) ?: return false
        return context.apply(player, session, session.visualEffectDraft)
    }

    fun reset(session: ItemEditSession) {
        session.visualEffectContext = null
        session.visualEffectDraft = VisualEffectDraft()
        session.visualEffectLastTypePage = 0
    }
}

private class PotionVisualEffectContext(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : VisualEffectContext {
    override val key: VisualEffectContextKey = VisualEffectContextKey.POTION
    override val supportsProbability: Boolean = false

    override fun apply(player: Player, session: ItemEditSession, draft: VisualEffectDraft): Boolean {
        val type = draft.effectTypeKey?.let { Registry.MOB_EFFECT.get(org.bukkit.NamespacedKey.minecraft(it.lowercase())) } ?: return false
        val visibleTicks = VisualEffectInputSupport.visibleTicksFromDurationSeconds(draft.durationSeconds)
        val storedTicks = PotionItemSupport.denormalizeDurationForItemForm(session.editableItem.type, type, visibleTicks)
        val action = ItemAction.PotionEffectAdd(type, storedTicks, draft.level - 1, false, draft.showParticles, draft.showIcon)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return false

        PotionItemSupport.addEffect(
            session.editableItem,
            PotionEffect(type, storedTicks, draft.level - 1, false, draft.showParticles, draft.showIcon)
        )
        return true
    }
}

private class ConsumableVisualEffectContext(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : VisualEffectContext {
    override val key: VisualEffectContextKey = VisualEffectContextKey.CONSUMABLE
    override val supportsProbability: Boolean = true

    override fun apply(player: Player, session: ItemEditSession, draft: VisualEffectDraft): Boolean {
        val type = draft.effectTypeKey?.let { Registry.MOB_EFFECT.get(org.bukkit.NamespacedKey.minecraft(it.lowercase())) } ?: return false
        val entry = EffectApplyEntrySpec(
            type = type,
            duration = VisualEffectInputSupport.visibleTicksFromDurationSeconds(draft.durationSeconds),
            amplifier = draft.level - 1,
            showParticles = draft.showParticles,
            showIcon = draft.showIcon
        )
        val probability = draft.probabilityPercent.coerceIn(0, 100) / 100f
        val action = ItemAction.ConsumableEffectAdd(EffectActionSpec.ApplyEffects(probability, listOf(entry)))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return false

        EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
        ConsumableComponentSupport.addApplyEffect(session.editableItem, probability, entry)
        return true
    }
}

private class DeathProtectionVisualEffectContext(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : VisualEffectContext {
    override val key: VisualEffectContextKey = VisualEffectContextKey.DEATH_PROTECTION
    override val supportsProbability: Boolean = true

    override fun apply(player: Player, session: ItemEditSession, draft: VisualEffectDraft): Boolean {
        val type = draft.effectTypeKey?.let { Registry.MOB_EFFECT.get(org.bukkit.NamespacedKey.minecraft(it.lowercase())) } ?: return false
        val entry = EffectApplyEntrySpec(
            type = type,
            duration = VisualEffectInputSupport.visibleTicksFromDurationSeconds(draft.durationSeconds),
            amplifier = draft.level - 1,
            showParticles = draft.showParticles,
            showIcon = draft.showIcon
        )
        val probability = draft.probabilityPercent.coerceIn(0, 100) / 100f
        val action = ItemAction.DeathProtectionEffectAdd(EffectActionSpec.ApplyEffects(probability, listOf(entry)))
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return false

        DeathProtectionMenuSupport.addApplyEffect(session.editableItem, probability, entry)
        return true
    }
}
