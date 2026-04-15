@file:Suppress("UnstableApiUsage") // Experimental Consumable

package com.ratger.acreative.menus.edit.effects

import com.ratger.acreative.menus.edit.api.EffectActionSpec
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.set.RegistrySet
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
