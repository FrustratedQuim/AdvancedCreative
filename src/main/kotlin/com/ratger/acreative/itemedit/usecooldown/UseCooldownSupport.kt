@file:Suppress("UnstableApiUsage") // Experimental UseCooldown

package com.ratger.acreative.itemedit.usecooldown

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.UseCooldown
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack

object UseCooldownSupport {

    fun has(item: ItemStack): Boolean = item.getData(DataComponentTypes.USE_COOLDOWN) != null

    fun seconds(item: ItemStack): Float? = item.getData(DataComponentTypes.USE_COOLDOWN)?.seconds()

    fun group(item: ItemStack): Key? = item.getData(DataComponentTypes.USE_COOLDOWN)?.cooldownGroup()

    fun hasGroup(item: ItemStack): Boolean = group(item) != null

    fun setSeconds(item: ItemStack, seconds: Float) {
        val builder = UseCooldown.useCooldown(seconds)
        builder.cooldownGroup(group(item))
        item.setData(DataComponentTypes.USE_COOLDOWN, builder.build())
    }

    fun setGroup(item: ItemStack, group: Key): Boolean {
        val currentSeconds = seconds(item) ?: return false
        val builder = UseCooldown.useCooldown(currentSeconds)
        builder.cooldownGroup(group)
        item.setData(DataComponentTypes.USE_COOLDOWN, builder.build())
        return true
    }

    fun clear(item: ItemStack) {
        item.unsetData(DataComponentTypes.USE_COOLDOWN)
    }

    fun clearGroup(item: ItemStack): Boolean {
        val currentSeconds = seconds(item) ?: return false
        val builder = UseCooldown.useCooldown(currentSeconds)
        builder.cooldownGroup(null)
        item.setData(DataComponentTypes.USE_COOLDOWN, builder.build())
        return true
    }

    fun displaySeconds(value: Float): String {
        return if (value % 1f == 0f) value.toInt().toString() else value.toString()
    }

    fun displayGroup(key: Key?): String {
        if (key == null) return "Отсутствует"
        return if (key.namespace() == "minecraft") key.value() else key.asString()
    }
}
