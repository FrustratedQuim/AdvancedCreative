@file:Suppress("UnstableApiUsage") // Experimental DeathProtection

package com.ratger.acreative.itemedit.effects

import com.ratger.acreative.itemedit.api.EffectApplyEntrySpec
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object DeathProtectionMenuSupport {
    data class ApplyEffectEntryView(
        val index: Int,
        val probability: Float,
        val effect: EffectApplyEntrySpec
    )

    fun isEnabled(item: ItemStack): Boolean = item.getData(DataComponentTypes.DEATH_PROTECTION) != null

    fun setEnabled(item: ItemStack, enabled: Boolean) {
        if (enabled) {
            ensureEnabled(item)
            return
        }
        item.unsetData(DataComponentTypes.DEATH_PROTECTION)
    }

    fun clearAll(item: ItemStack) {
        item.unsetData(DataComponentTypes.DEATH_PROTECTION)
    }

    fun soundKey(item: ItemStack): Key? {
        val effect = effects(item).firstOrNull { it is ConsumeEffect.PlaySound } as? ConsumeEffect.PlaySound
        return effect?.sound()
    }

    fun setSound(item: ItemStack, key: Key) {
        mutate(item) { current ->
            val cleaned = current.filterNot { it is ConsumeEffect.PlaySound }
            cleaned + ConsumeEffect.playSoundConsumeEffect(key)
        }
    }

    fun clearSound(item: ItemStack) {
        mutate(item) { current -> current.filterNot { it is ConsumeEffect.PlaySound } }
    }

    fun hasClearAllEffects(item: ItemStack): Boolean = effects(item).any { it is ConsumeEffect.ClearAllStatusEffects }

    fun setClearAllEffects(item: ItemStack, enabled: Boolean) {
        mutate(item) { current ->
            val without = current.filterNot { it is ConsumeEffect.ClearAllStatusEffects }
            if (enabled) without + ConsumeEffect.clearAllStatusEffects() else without
        }
    }

    fun removedEffects(item: ItemStack): List<PotionEffectType> {
        val effect = effects(item).firstOrNull { it is ConsumeEffect.RemoveStatusEffects } as? ConsumeEffect.RemoveStatusEffects
        return effect?.removeEffects()?.resolve(Registry.MOB_EFFECT)?.distinctBy { it.key.key } ?: emptyList()
    }

    fun addRemovedEffect(item: ItemStack, type: PotionEffectType) {
        val updated = (removedEffects(item) + type).distinctBy { it.key.key }
        setRemovedEffects(item, updated)
    }

    fun removeRemovedEffect(item: ItemStack, type: PotionEffectType) {
        val updated = removedEffects(item).filterNot { it == type }
        setRemovedEffects(item, updated)
    }

    fun clearRemovedEffects(item: ItemStack) {
        mutate(item) { current -> current.filterNot { it is ConsumeEffect.RemoveStatusEffects } }
    }

    fun randomTeleportDiameter(item: ItemStack): Float? {
        val effect = effects(item).firstOrNull { it is ConsumeEffect.TeleportRandomly } as? ConsumeEffect.TeleportRandomly
        return effect?.diameter()
    }

    fun setRandomTeleportDiameter(item: ItemStack, diameter: Float) {
        mutate(item) { current ->
            val cleaned = current.filterNot { it is ConsumeEffect.TeleportRandomly }
            cleaned + ConsumeEffect.teleportRandomlyEffect(diameter)
        }
    }

    fun clearRandomTeleport(item: ItemStack) {
        mutate(item) { current -> current.filterNot { it is ConsumeEffect.TeleportRandomly } }
    }

    fun applyEffectEntries(item: ItemStack): List<ApplyEffectEntryView> {
        return effects(item)
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

    fun addApplyEffect(item: ItemStack, probability: Float, effect: EffectApplyEntrySpec) {
        mutate(item) { current ->
            current + ConsumeEffect.applyStatusEffects(
                listOf(PotionEffect(effect.type, effect.duration, effect.amplifier, false, effect.showParticles, effect.showIcon)),
                probability
            )
        }
    }

    fun removeApplyEffect(item: ItemStack, applyIndex: Int) {
        mutate(item) { current ->
            val applyIndices = current.withIndex().filter { it.value is ConsumeEffect.ApplyStatusEffects }.map { it.index }
            val rawIndex = applyIndices.getOrNull(applyIndex) ?: return@mutate current
            current.filterIndexed { index, _ -> index != rawIndex }
        }
    }

    fun clearApplyEffects(item: ItemStack) {
        mutate(item) { current -> current.filterNot { it is ConsumeEffect.ApplyStatusEffects } }
    }

    private fun setRemovedEffects(item: ItemStack, effects: List<PotionEffectType>) {
        mutate(item) { current ->
            val cleaned = current.filterNot { it is ConsumeEffect.RemoveStatusEffects }
            if (effects.isEmpty()) {
                cleaned
            } else {
                cleaned + ConsumeEffect.removeEffects(RegistrySet.keySetFromValues(RegistryKey.MOB_EFFECT, effects))
            }
        }
    }

    private fun mutate(item: ItemStack, mapper: (List<ConsumeEffect>) -> List<ConsumeEffect>) {
        val normalized = normalizeEffects(effects(item))
        val updated = mapper(normalized)
        ensureEnabled(item)
        item.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection().addEffects(updated).build())
    }

    private fun ensureEnabled(item: ItemStack) {
        if (item.getData(DataComponentTypes.DEATH_PROTECTION) == null) {
            item.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection().build())
        }
    }

    private fun effects(item: ItemStack): List<ConsumeEffect> {
        val deathProtection = item.getData(DataComponentTypes.DEATH_PROTECTION) ?: return emptyList()
        val normalized = normalizeEffects(deathProtection.deathEffects())
        if (normalized != deathProtection.deathEffects()) {
            item.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection().addEffects(normalized).build())
        }
        return normalized
    }

    private fun normalizeEffects(source: List<ConsumeEffect>): List<ConsumeEffect> {
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
}
