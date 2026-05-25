package com.ratger.acreative.menus.edit.effects

import net.kyori.adventure.key.Key
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

object EdibleMenuSupport {
    data class Snapshot(
        val enabled: Boolean,
        val nutrition: Int,
        val saturation: Float,
        val canAlwaysEat: Boolean,
        val hasParticles: Boolean,
        val animation: ItemUseAnimation,
        val soundKey: Key?,
        val clearAllEffects: Boolean,
        val removedEffects: List<PotionEffectType>,
        val randomTeleportDiameter: Float?,
        val applyEffects: List<ConsumeEffectSetSupport.ApplyEffectEntryView>,
        val consumeSeconds: Float?
    )

    fun isEnabled(item: ItemStack): Boolean = ConsumableComponentSupport.isEnabled(item)

    fun captureSnapshot(item: ItemStack): Snapshot {
        return Snapshot(
            enabled = isEnabled(item),
            nutrition = FoodComponentSupport.nutrition(item),
            saturation = FoodComponentSupport.saturation(item),
            canAlwaysEat = FoodComponentSupport.canAlwaysEat(item),
            hasParticles = ConsumableComponentSupport.hasParticles(item),
            animation = ConsumableComponentSupport.animation(item),
            soundKey = ConsumableComponentSupport.soundKey(item),
            clearAllEffects = ConsumableComponentSupport.hasClearAllEffects(item),
            removedEffects = ConsumableComponentSupport.removedEffects(item),
            randomTeleportDiameter = ConsumableComponentSupport.randomTeleportDiameter(item),
            applyEffects = ConsumableComponentSupport.applyEffectEntries(item),
            consumeSeconds = ConsumableComponentSupport.consumeSeconds(item)
        )
    }

    fun ensureEnabledWithDefaults(item: ItemStack) {
        if (isEnabled(item)) return
        ConsumableComponentSupport.setEnabled(item, true)
        FoodComponentSupport.setNutrition(item, 0)
        FoodComponentSupport.setSaturation(item, 0f)
        FoodComponentSupport.setCanAlwaysEat(item, false)
        ConsumableComponentSupport.setHasParticles(item, false)
        ConsumableComponentSupport.setAnimation(item, ItemUseAnimation.EAT)
        ConsumableComponentSupport.clearSound(item)
        ConsumableComponentSupport.setClearAllEffects(item, false)
        ConsumableComponentSupport.clearRemovedEffects(item)
        ConsumableComponentSupport.clearRandomTeleport(item)
        ConsumableComponentSupport.clearApplyEffects(item)
    }

    fun clearAll(item: ItemStack) {
        FoodComponentSupport.clear(item)
        ConsumableComponentSupport.clearAll(item)
    }

    fun restoreSnapshot(item: ItemStack, snapshot: Snapshot) {
        if (!snapshot.enabled) {
            clearAll(item)
            return
        }

        ensureEnabledWithDefaults(item)
        FoodComponentSupport.setNutrition(item, snapshot.nutrition)
        FoodComponentSupport.setSaturation(item, snapshot.saturation)
        FoodComponentSupport.setCanAlwaysEat(item, snapshot.canAlwaysEat)
        ConsumableComponentSupport.setHasParticles(item, snapshot.hasParticles)
        ConsumableComponentSupport.setAnimation(item, snapshot.animation)
        if (snapshot.consumeSeconds != null) {
            ConsumableComponentSupport.setConsumeSeconds(item, snapshot.consumeSeconds)
        } else {
            ConsumableComponentSupport.resetConsumeSeconds(item)
        }
        if (snapshot.soundKey != null) {
            ConsumableComponentSupport.setSound(item, snapshot.soundKey)
        } else {
            ConsumableComponentSupport.clearSound(item)
        }
        ConsumableComponentSupport.setClearAllEffects(item, snapshot.clearAllEffects)
        ConsumableComponentSupport.clearRemovedEffects(item)
        snapshot.removedEffects.forEach { effectType ->
            ConsumableComponentSupport.addRemovedEffect(item, effectType)
        }
        if (snapshot.randomTeleportDiameter != null) {
            ConsumableComponentSupport.setRandomTeleportDiameter(item, snapshot.randomTeleportDiameter)
        } else {
            ConsumableComponentSupport.clearRandomTeleport(item)
        }
        ConsumableComponentSupport.clearApplyEffects(item)
        snapshot.applyEffects.forEach { entry ->
            ConsumableComponentSupport.addApplyEffect(item, entry.probability, entry.effect)
        }
    }
}
