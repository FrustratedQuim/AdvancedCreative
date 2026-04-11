package com.ratger.acreative.itemedit.effects

import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import org.bukkit.inventory.ItemStack

object EdibleMenuSupport {
    fun isEnabled(item: ItemStack): Boolean = ConsumableComponentSupport.isEnabled(item)

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
}
