@file:Suppress("UnstableApiUsage") // Experimental Consumable

package com.ratger.acreative.itemedit.effects

import com.ratger.acreative.itemedit.api.EffectApplyEntrySpec
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

object ConsumableComponentSupport {
    fun isEnabled(item: ItemStack): Boolean = item.getData(DataComponentTypes.CONSUMABLE) != null

    fun ensureEnabled(item: ItemStack) {
        if (!isEnabled(item)) {
            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().build())
        }
    }

    fun setEnabled(item: ItemStack, enabled: Boolean) {
        if (enabled) {
            ensureEnabled(item)
        } else {
            item.unsetData(DataComponentTypes.CONSUMABLE)
        }
    }

    fun animation(item: ItemStack): ItemUseAnimation =
        item.getData(DataComponentTypes.CONSUMABLE)?.animation() ?: ItemUseAnimation.EAT

    fun setAnimation(item: ItemStack, animation: ItemUseAnimation) {
        mutate(item) { builder -> builder.animation(animation) }
    }

    fun hasParticles(item: ItemStack): Boolean = item.getData(DataComponentTypes.CONSUMABLE)?.hasConsumeParticles() ?: false

    fun setHasParticles(item: ItemStack, value: Boolean) {
        mutate(item) { builder -> builder.hasConsumeParticles(value) }
    }

    fun soundKey(item: ItemStack): Key? {
        val effects = effects(item)
        return ConsumeEffectSetSupport.soundKey(effects)
    }

    fun setSound(item: ItemStack, key: Key) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withSound(effects, key) }
    }

    fun clearSound(item: ItemStack) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withoutSound(effects) }
    }

    fun hasClearAllEffects(item: ItemStack): Boolean = ConsumeEffectSetSupport.hasClearAllEffects(effects(item))

    fun setClearAllEffects(item: ItemStack, enabled: Boolean) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withClearAllEffects(effects, enabled) }
    }

    fun removedEffects(item: ItemStack): List<PotionEffectType> = ConsumeEffectSetSupport.removedEffects(effects(item))

    fun addRemovedEffect(item: ItemStack, type: PotionEffectType) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.addRemovedEffect(effects, type) }
    }

    fun removeRemovedEffect(item: ItemStack, type: PotionEffectType) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.removeRemovedEffect(effects, type) }
    }

    fun clearRemovedEffects(item: ItemStack) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withoutRemovedEffects(effects) }
    }

    fun randomTeleportDiameter(item: ItemStack): Float? = ConsumeEffectSetSupport.randomTeleportDiameter(effects(item))

    fun setRandomTeleportDiameter(item: ItemStack, diameter: Float) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withRandomTeleportDiameter(effects, diameter) }
    }

    fun clearRandomTeleport(item: ItemStack) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withoutRandomTeleport(effects) }
    }

    fun applyEffectEntries(item: ItemStack): List<ConsumeEffectSetSupport.ApplyEffectEntryView> =
        ConsumeEffectSetSupport.applyEffectEntries(effects(item))

    fun addApplyEffect(item: ItemStack, probability: Float, effect: EffectApplyEntrySpec) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.addApplyEffect(effects, probability, effect) }
    }

    fun removeApplyEffect(item: ItemStack, applyIndex: Int) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.removeApplyEffect(effects, applyIndex) }
    }

    fun clearApplyEffects(item: ItemStack) {
        mutateEffects(item) { effects -> ConsumeEffectSetSupport.withoutApplyEffects(effects) }
    }

    fun clearAll(item: ItemStack) {
        item.unsetData(DataComponentTypes.CONSUMABLE)
    }

    private fun effects(item: ItemStack): List<ConsumeEffect> {
        val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return emptyList()
        val consumeEffects = consumable.consumeEffects()
        val normalized = ConsumeEffectSetSupport.normalize(consumeEffects)
        if (normalized != consumeEffects) {
            item.setData(DataComponentTypes.CONSUMABLE, rebuildConsumable(consumable, normalized))
        }
        return normalized
    }

    private fun mutateEffects(item: ItemStack, mapper: (List<ConsumeEffect>) -> List<ConsumeEffect>) {
        val normalized = ConsumeEffectSetSupport.normalize(effects(item))
        val updated = mapper(normalized)
        val current = item.getData(DataComponentTypes.CONSUMABLE) ?: Consumable.consumable().build()
        item.setData(DataComponentTypes.CONSUMABLE, rebuildConsumable(current, updated))
    }

    private fun mutate(item: ItemStack, update: (Consumable.Builder) -> Unit) {
        ensureEnabled(item)
        val builder = consumableBuilder(item)
        update(builder)
        item.setData(DataComponentTypes.CONSUMABLE, builder.build())
    }

    private fun consumableBuilder(item: ItemStack): Consumable.Builder =
        item.getData(DataComponentTypes.CONSUMABLE)?.toBuilder() ?: Consumable.consumable()

    private fun rebuildConsumable(current: Consumable, effects: List<ConsumeEffect>): Consumable {
        return Consumable.consumable()
            .consumeSeconds(current.consumeSeconds())
            .animation(current.animation())
            .hasConsumeParticles(current.hasConsumeParticles())
            .sound(current.sound())
            .addEffects(effects)
            .build()
    }
}
