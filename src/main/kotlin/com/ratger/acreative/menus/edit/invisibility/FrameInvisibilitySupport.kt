package com.ratger.acreative.menus.edit.invisibility

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import org.bukkit.Material
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack

object FrameInvisibilitySupport {
    fun isEnabled(item: ItemStack): Boolean {
        if (item.type != Material.ITEM_FRAME && item.type != Material.GLOW_ITEM_FRAME) {
            return false
        }

        val nms = CraftItemStack.asNMSCopy(item)
        val entityTag = nms.get(DataComponents.ENTITY_DATA)?.copyTag() ?: return false
        return entityTag.getBoolean("Invisible")
    }

    fun toggle(item: ItemStack): ItemStack? = apply(item, !isEnabled(item))

    fun apply(item: ItemStack, invisible: Boolean): ItemStack? {
        val entityId = when (item.type) {
            Material.ITEM_FRAME -> "minecraft:item_frame"
            Material.GLOW_ITEM_FRAME -> "minecraft:glow_item_frame"
            else -> return null
        }

        val nms = CraftItemStack.asNMSCopy(item)
        val current = nms.get(DataComponents.ENTITY_DATA)
        val entityTag = current?.copyTag() ?: CompoundTag().apply {
            putString("id", entityId)
        }

        if (!entityTag.contains("id")) {
            entityTag.putString("id", entityId)
        }

        if (invisible) {
            entityTag.putBoolean("Invisible", true)
        } else {
            entityTag.remove("Invisible")
        }

        nms.set(DataComponents.ENTITY_DATA, CustomData.of(entityTag))
        return CraftItemStack.asBukkitCopy(nms)
    }
}
