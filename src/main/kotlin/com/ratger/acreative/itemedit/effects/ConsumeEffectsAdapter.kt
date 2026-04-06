@file:Suppress("UnstableApiUsage") // Experimental Consumable

package com.ratger.acreative.itemedit.effects

import com.ratger.acreative.itemedit.api.EffectActionSpec
import com.ratger.acreative.itemedit.api.EffectApplyEntrySpec
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.set.RegistrySet
import org.bukkit.Registry
import org.bukkit.potion.PotionEffect

object ConsumeEffectsAdapter {
    fun addConsumableEffect(current: Consumable, spec: EffectActionSpec): Consumable {
        val effects = current.consumeEffects() + toConsumeEffect(spec)
        return rebuildConsumable(current, effects)
    }

    fun removeConsumableEffect(current: Consumable, index: Int): Consumable? {
        val next = removeByIndex(current.consumeEffects(), index) ?: return null
        return rebuildConsumable(current, next)
    }

    fun clearConsumableEffects(current: Consumable): Consumable {
        return rebuildConsumable(current, emptyList())
    }

    fun addDeathProtectionEffect(current: DeathProtection, spec: EffectActionSpec): DeathProtection {
        return deathProtectionOf(current.deathEffects() + toConsumeEffect(spec))
    }

    fun removeDeathProtectionEffect(current: DeathProtection, index: Int): DeathProtection? {
        val next = removeByIndex(current.deathEffects(), index) ?: return null
        return deathProtectionOf(next)
    }

    fun clearDeathProtectionEffects(): DeathProtection = deathProtectionOf(emptyList())

    fun render(effect: ConsumeEffect): String {
        val spec = toEffectActionSpec(effect)
        return if (spec != null) {
            EffectActionsSupport.render(spec)
        } else {
            "<unknown_effect:${effect::class.simpleName ?: "?"}>"
        }
    }

    private fun toConsumeEffect(spec: EffectActionSpec): ConsumeEffect {
        return when (spec) {
            EffectActionSpec.ClearAllEffects -> ConsumeEffect.clearAllStatusEffects()
            is EffectActionSpec.PlaySound -> ConsumeEffect.playSoundConsumeEffect(spec.key)
            is EffectActionSpec.RemoveEffects -> ConsumeEffect.removeEffects(
                RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, spec.effects)
            )
            is EffectActionSpec.TeleportRandomly -> ConsumeEffect.teleportRandomlyEffect(spec.diameter)
            is EffectActionSpec.ApplyEffects -> ConsumeEffect.applyStatusEffects(
                spec.effects.map { PotionEffect(it.type, it.duration, it.amplifier, false, it.showParticles, it.showIcon) },
                spec.probability
            )
        }
    }

    private fun toEffectActionSpec(effect: ConsumeEffect): EffectActionSpec? {
        return when (effect) {
            is ConsumeEffect.ClearAllStatusEffects -> EffectActionSpec.ClearAllEffects
            is ConsumeEffect.PlaySound -> EffectActionSpec.PlaySound(effect.sound())
            is ConsumeEffect.RemoveStatusEffects -> EffectActionSpec.RemoveEffects(effect.removeEffects().resolve(Registry.MOB_EFFECT).toList())
            is ConsumeEffect.TeleportRandomly -> EffectActionSpec.TeleportRandomly(effect.diameter())
            is ConsumeEffect.ApplyStatusEffects -> {
                val entries = effect.effects().map {
                    EffectApplyEntrySpec(
                        type = it.type,
                        duration = it.duration,
                        amplifier = it.amplifier,
                        showParticles = it.hasParticles(),
                        showIcon = it.hasIcon()
                    )
                }
                EffectActionSpec.ApplyEffects(effect.probability(), entries)
            }
            else -> null
        }
    }

    private fun removeByIndex(effects: List<ConsumeEffect>, index: Int): List<ConsumeEffect>? {
        if (index !in effects.indices) return null
        return effects.filterIndexed { i, _ -> i != index }
    }

    private fun rebuildConsumable(current: Consumable, effects: List<ConsumeEffect>): Consumable {
        return Consumable.consumable()
            .consumeSeconds(current.consumeSeconds())
            .animation(current.animation())
            .hasConsumeParticles(current.hasConsumeParticles())
            .sound(current.sound())
            .addEffects(effects)
            .build()
    }

    private fun deathProtectionOf(effects: List<ConsumeEffect>): DeathProtection {
        return DeathProtection.deathProtection().addEffects(effects).build()
    }
}
