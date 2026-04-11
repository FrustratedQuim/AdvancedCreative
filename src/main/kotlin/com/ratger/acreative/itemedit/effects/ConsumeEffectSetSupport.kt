@file:Suppress("UnstableApiUsage") // Experimental Consumable/DeathProtection

package com.ratger.acreative.itemedit.effects

import com.ratger.acreative.itemedit.api.EffectApplyEntrySpec
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object ConsumeEffectSetSupport {
    data class ApplyEffectEntryView(
        val index: Int,
        val probability: Float,
        val effect: EffectApplyEntrySpec
    )

    fun normalize(source: List<ConsumeEffect>): List<ConsumeEffect> {
        val normalized = mutableListOf<ConsumeEffect>()

        source.firstOrNull { it is ConsumeEffect.PlaySound }?.let(normalized::add)
        if (source.any { it is ConsumeEffect.ClearAllStatusEffects }) {
            normalized += ConsumeEffect.clearAllStatusEffects()
        }

        val mergedRemovedEffects = source
            .mapNotNull { it as? ConsumeEffect.RemoveStatusEffects }
            .flatMap { it.removeEffects().resolve(Registry.MOB_EFFECT) }
            .distinctBy { it.key.key }
        if (mergedRemovedEffects.isNotEmpty()) {
            normalized += ConsumeEffect.removeEffects(RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, mergedRemovedEffects))
        }

        source.firstOrNull { it is ConsumeEffect.TeleportRandomly }?.let(normalized::add)

        val applyEffects = source
            .mapNotNull { it as? ConsumeEffect.ApplyStatusEffects }
            .flatMap { apply ->
                apply.effects().map { potion ->
                    ConsumeEffect.applyStatusEffects(listOf(potion), apply.probability())
                }
            }
        normalized += applyEffects

        return normalized
    }

    fun soundKey(effects: List<ConsumeEffect>): Key? {
        val effect = effects.firstOrNull { it is ConsumeEffect.PlaySound } as? ConsumeEffect.PlaySound
        return effect?.sound()
    }

    fun withSound(effects: List<ConsumeEffect>, key: Key): List<ConsumeEffect> {
        val cleaned = effects.filterNot { it is ConsumeEffect.PlaySound }
        return cleaned + ConsumeEffect.playSoundConsumeEffect(key)
    }

    fun withoutSound(effects: List<ConsumeEffect>): List<ConsumeEffect> =
        effects.filterNot { it is ConsumeEffect.PlaySound }

    fun hasClearAllEffects(effects: List<ConsumeEffect>): Boolean = effects.any { it is ConsumeEffect.ClearAllStatusEffects }

    fun withClearAllEffects(effects: List<ConsumeEffect>, enabled: Boolean): List<ConsumeEffect> {
        val without = effects.filterNot { it is ConsumeEffect.ClearAllStatusEffects }
        return if (enabled) without + ConsumeEffect.clearAllStatusEffects() else without
    }

    fun removedEffects(effects: List<ConsumeEffect>): List<PotionEffectType> {
        val effect = effects.firstOrNull { it is ConsumeEffect.RemoveStatusEffects } as? ConsumeEffect.RemoveStatusEffects
        return effect?.removeEffects()?.resolve(Registry.MOB_EFFECT)?.distinctBy { it.key.key } ?: emptyList()
    }

    fun addRemovedEffect(effects: List<ConsumeEffect>, type: PotionEffectType): List<ConsumeEffect> {
        val updated = (removedEffects(effects) + type).distinctBy { it.key.key }
        return setRemovedEffects(effects, updated)
    }

    fun removeRemovedEffect(effects: List<ConsumeEffect>, type: PotionEffectType): List<ConsumeEffect> {
        val updated = removedEffects(effects).filterNot { it == type }
        return setRemovedEffects(effects, updated)
    }

    fun withoutRemovedEffects(effects: List<ConsumeEffect>): List<ConsumeEffect> =
        effects.filterNot { it is ConsumeEffect.RemoveStatusEffects }

    fun randomTeleportDiameter(effects: List<ConsumeEffect>): Float? {
        val effect = effects.firstOrNull { it is ConsumeEffect.TeleportRandomly } as? ConsumeEffect.TeleportRandomly
        return effect?.diameter()
    }

    fun withRandomTeleportDiameter(effects: List<ConsumeEffect>, diameter: Float): List<ConsumeEffect> {
        val cleaned = effects.filterNot { it is ConsumeEffect.TeleportRandomly }
        return cleaned + ConsumeEffect.teleportRandomlyEffect(diameter)
    }

    fun withoutRandomTeleport(effects: List<ConsumeEffect>): List<ConsumeEffect> =
        effects.filterNot { it is ConsumeEffect.TeleportRandomly }

    fun applyEffectEntries(effects: List<ConsumeEffect>): List<ApplyEffectEntryView> {
        return effects
            .mapNotNull { it as? ConsumeEffect.ApplyStatusEffects }
            .mapIndexedNotNull { index, apply ->
                val potion = apply.effects().singleOrNull() ?: return@mapIndexedNotNull null
                ApplyEffectEntryView(
                    index = index,
                    probability = apply.probability(),
                    effect = EffectApplyEntrySpec(
                        type = potion.type,
                        duration = potion.duration,
                        amplifier = potion.amplifier,
                        showParticles = potion.hasParticles(),
                        showIcon = potion.hasIcon()
                    )
                )
            }
    }

    fun addApplyEffect(effects: List<ConsumeEffect>, probability: Float, effect: EffectApplyEntrySpec): List<ConsumeEffect> {
        return effects + ConsumeEffect.applyStatusEffects(
            listOf(PotionEffect(effect.type, effect.duration, effect.amplifier, false, effect.showParticles, effect.showIcon)),
            probability
        )
    }

    fun removeApplyEffect(effects: List<ConsumeEffect>, applyIndex: Int): List<ConsumeEffect> {
        val applyIndices = effects.withIndex().filter { it.value is ConsumeEffect.ApplyStatusEffects }.map { it.index }
        val rawIndex = applyIndices.getOrNull(applyIndex) ?: return effects
        return effects.filterIndexed { index, _ -> index != rawIndex }
    }

    fun withoutApplyEffects(effects: List<ConsumeEffect>): List<ConsumeEffect> =
        effects.filterNot { it is ConsumeEffect.ApplyStatusEffects }

    private fun setRemovedEffects(effects: List<ConsumeEffect>, removedEffects: List<PotionEffectType>): List<ConsumeEffect> {
        val cleaned = effects.filterNot { it is ConsumeEffect.RemoveStatusEffects }
        return if (removedEffects.isEmpty()) {
            cleaned
        } else {
            cleaned + ConsumeEffect.removeEffects(RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, removedEffects))
        }
    }
}
