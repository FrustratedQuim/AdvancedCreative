@file:Suppress("UnstableApiUsage") // Experimental DeathProtection

package com.ratger.acreative.itemedit.effects

import com.ratger.acreative.itemedit.api.EffectApplyEntrySpec
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DeathProtection
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
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

    fun soundKey(item: ItemStack): Key? = ConsumeEffectSetSupport.soundKey(effects(item))

    fun setSound(item: ItemStack, key: Key) {
        mutate(item) { current -> ConsumeEffectSetSupport.withSound(current, key) }
    }

    fun clearSound(item: ItemStack) {
        mutate(item) { current -> ConsumeEffectSetSupport.withoutSound(current) }
    }

    fun hasClearAllEffects(item: ItemStack): Boolean = ConsumeEffectSetSupport.hasClearAllEffects(effects(item))

    fun setClearAllEffects(item: ItemStack, enabled: Boolean) {
        mutate(item) { current -> ConsumeEffectSetSupport.withClearAllEffects(current, enabled) }
    }

    fun removedEffects(item: ItemStack): List<PotionEffectType> = ConsumeEffectSetSupport.removedEffects(effects(item))

    fun addRemovedEffect(item: ItemStack, type: PotionEffectType) {
        mutate(item) { current -> ConsumeEffectSetSupport.addRemovedEffect(current, type) }
    }

    fun removeRemovedEffect(item: ItemStack, type: PotionEffectType) {
        mutate(item) { current -> ConsumeEffectSetSupport.removeRemovedEffect(current, type) }
    }

    fun clearRemovedEffects(item: ItemStack) {
        mutate(item) { current -> ConsumeEffectSetSupport.withoutRemovedEffects(current) }
    }

    fun randomTeleportDiameter(item: ItemStack): Float? = ConsumeEffectSetSupport.randomTeleportDiameter(effects(item))

    fun setRandomTeleportDiameter(item: ItemStack, diameter: Float) {
        mutate(item) { current -> ConsumeEffectSetSupport.withRandomTeleportDiameter(current, diameter) }
    }

    fun clearRandomTeleport(item: ItemStack) {
        mutate(item) { current -> ConsumeEffectSetSupport.withoutRandomTeleport(current) }
    }

    fun applyEffectEntries(item: ItemStack): List<ApplyEffectEntryView> =
        ConsumeEffectSetSupport.applyEffectEntries(effects(item)).map { ApplyEffectEntryView(it.index, it.probability, it.effect) }

    fun addApplyEffect(item: ItemStack, probability: Float, effect: EffectApplyEntrySpec) {
        mutate(item) { current -> ConsumeEffectSetSupport.addApplyEffect(current, probability, effect) }
    }

    fun removeApplyEffect(item: ItemStack, applyIndex: Int) {
        mutate(item) { current -> ConsumeEffectSetSupport.removeApplyEffect(current, applyIndex) }
    }

    fun clearApplyEffects(item: ItemStack) {
        mutate(item) { current -> ConsumeEffectSetSupport.withoutApplyEffects(current) }
    }

    private fun mutate(item: ItemStack, mapper: (List<ConsumeEffect>) -> List<ConsumeEffect>) {
        val normalized = ConsumeEffectSetSupport.normalize(effects(item))
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
        val normalized = ConsumeEffectSetSupport.normalize(deathProtection.deathEffects())
        if (normalized != deathProtection.deathEffects()) {
            item.setData(DataComponentTypes.DEATH_PROTECTION, DeathProtection.deathProtection().addEffects(normalized).build())
        }
        return normalized
    }
}
