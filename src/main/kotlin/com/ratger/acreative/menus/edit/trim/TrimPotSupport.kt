package com.ratger.acreative.menus.edit.trim

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.DecoratedPot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object TrimPotSupport {

    val potDecorationMaterialIds: List<String> = run {
        val ids = linkedSetOf<String>()
        ids += Material.BRICK.key.asString()
        Tag.ITEMS_DECORATED_POT_SHERDS.values
            .map { it.key.asString() }
            .sorted()
            .forEach(ids::add)
        ids.toList()
    }


    fun applyDecorations(item: ItemStack, back: Material?, left: Material?, right: Material?, front: Material?): Boolean {
        if (item.type != Material.DECORATED_POT) return false
        val meta = item.itemMeta as? BlockStateMeta ?: return false
        val state = meta.blockState as? DecoratedPot ?: return false
        state.setSherd(DecoratedPot.Side.BACK, back)
        state.setSherd(DecoratedPot.Side.LEFT, left)
        state.setSherd(DecoratedPot.Side.RIGHT, right)
        state.setSherd(DecoratedPot.Side.FRONT, front)
        meta.blockState = state
        item.itemMeta = meta
        return true
    }

    fun applySide(item: ItemStack, side: DecoratedPotSide, material: Material?): Boolean {
        if (item.type != Material.DECORATED_POT) return false
        val meta = item.itemMeta as? BlockStateMeta ?: return false
        val state = meta.blockState as? DecoratedPot ?: return false
        state.setSherd(side.toBukkit(), material)
        meta.blockState = state
        item.itemMeta = meta
        return true
    }

    fun sherd(item: ItemStack, side: DecoratedPotSide): Material? {
        if (item.type != Material.DECORATED_POT) return null
        val meta = item.itemMeta as? BlockStateMeta ?: return null
        val state = meta.blockState as? DecoratedPot ?: return null
        return state.getSherd(side.toBukkit())
    }

    fun isSherdSelected(item: ItemStack, side: DecoratedPotSide, material: Material): Boolean {
        return sherd(item, side) == material
    }

    fun isBrickSelected(item: ItemStack, side: DecoratedPotSide): Boolean {
        val current = sherd(item, side)
        return current == null || current == Material.BRICK
    }

    fun toggleSideSherd(item: ItemStack, side: DecoratedPotSide, material: Material): Boolean {
        val current = sherd(item, side)
        val next = if (current == material) null else material
        return applySide(item, side, next)
    }

}

enum class DecoratedPotSide {
    BACK,
    LEFT,
    RIGHT,
    FRONT;

    fun toBukkit(): DecoratedPot.Side = when (this) {
        BACK -> DecoratedPot.Side.BACK
        LEFT -> DecoratedPot.Side.LEFT
        RIGHT -> DecoratedPot.Side.RIGHT
        FRONT -> DecoratedPot.Side.FRONT
    }
}
