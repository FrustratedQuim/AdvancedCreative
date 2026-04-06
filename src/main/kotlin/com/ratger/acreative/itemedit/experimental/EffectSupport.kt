@file:Suppress("UnstableApiUsage") // Experimental Consumable/DeathProtection

package com.ratger.acreative.itemedit.experimental

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.inventory.ItemStack

object EffectSupport {
    fun consumableEffectIndices(item: ItemStack): List<String> {
        return (consumableEffects(item) ?: emptyList()).indices.map(Int::toString)
    }

    fun deathProtectionEffectIndices(item: ItemStack): List<String> {
        return (deathProtectionEffects(item) ?: emptyList()).indices.map(Int::toString)
    }

    fun consumableEffectCount(item: ItemStack): Int? {
        return consumableEffects(item)?.size
    }

    fun deathProtectionEffectCount(item: ItemStack): Int? {
        return deathProtectionEffects(item)?.size
    }

    private fun consumableEffects(item: ItemStack) =
        item.getData(DataComponentTypes.CONSUMABLE)?.consumeEffects()

    private fun deathProtectionEffects(item: ItemStack) =
        item.getData(DataComponentTypes.DEATH_PROTECTION)?.deathEffects()
}
