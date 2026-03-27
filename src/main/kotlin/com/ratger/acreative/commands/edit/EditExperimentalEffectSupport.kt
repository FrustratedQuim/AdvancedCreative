@file:Suppress("UnstableApiUsage")

package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.inventory.ItemStack

object EditExperimentalEffectSupport {
    fun consumableEffectCount(item: ItemStack): Int? {
        val consumable = item.getData(DataComponentTypes.CONSUMABLE) ?: return null
        return consumable.consumeEffects().size
    }

    fun deathProtectionEffectCount(item: ItemStack): Int? {
        val deathProtection = item.getData(DataComponentTypes.DEATH_PROTECTION) ?: return null
        return deathProtection.deathEffects().size
    }
}
