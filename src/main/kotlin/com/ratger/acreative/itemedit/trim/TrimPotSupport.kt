package com.ratger.acreative.itemedit.trim

import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.Tag
import org.bukkit.block.DecoratedPot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern

object TrimPotSupport {
    val trimPatternByTemplateId: Map<String, TrimPattern> = linkedMapOf(
        "minecraft:sentry_armor_trim_smithing_template" to TrimPattern.SENTRY,
        "minecraft:vex_armor_trim_smithing_template" to TrimPattern.VEX,
        "minecraft:wild_armor_trim_smithing_template" to TrimPattern.WILD,
        "minecraft:coast_armor_trim_smithing_template" to TrimPattern.COAST,
        "minecraft:dune_armor_trim_smithing_template" to TrimPattern.DUNE,
        "minecraft:wayfinder_armor_trim_smithing_template" to TrimPattern.WAYFINDER,
        "minecraft:raiser_armor_trim_smithing_template" to TrimPattern.RAISER,
        "minecraft:shaper_armor_trim_smithing_template" to TrimPattern.SHAPER,
        "minecraft:host_armor_trim_smithing_template" to TrimPattern.HOST,
        "minecraft:ward_armor_trim_smithing_template" to TrimPattern.WARD,
        "minecraft:silence_armor_trim_smithing_template" to TrimPattern.SILENCE,
        "minecraft:tide_armor_trim_smithing_template" to TrimPattern.TIDE,
        "minecraft:snout_armor_trim_smithing_template" to TrimPattern.SNOUT,
        "minecraft:rib_armor_trim_smithing_template" to TrimPattern.RIB,
        "minecraft:eye_armor_trim_smithing_template" to TrimPattern.EYE,
        "minecraft:spire_armor_trim_smithing_template" to TrimPattern.SPIRE,
        "minecraft:flow_armor_trim_smithing_template" to TrimPattern.FLOW,
        "minecraft:bolt_armor_trim_smithing_template" to TrimPattern.BOLT
    )

    val trimMaterialByItemId: Map<String, TrimMaterial> = linkedMapOf(
        "minecraft:amethyst_shard" to TrimMaterial.AMETHYST,
        "minecraft:copper_ingot" to TrimMaterial.COPPER,
        "minecraft:diamond" to TrimMaterial.DIAMOND,
        "minecraft:emerald" to TrimMaterial.EMERALD,
        "minecraft:gold_ingot" to TrimMaterial.GOLD,
        "minecraft:iron_ingot" to TrimMaterial.IRON,
        "minecraft:lapis_lazuli" to TrimMaterial.LAPIS,
        "minecraft:netherite_ingot" to TrimMaterial.NETHERITE,
        "minecraft:quartz" to TrimMaterial.QUARTZ,
        "minecraft:redstone" to TrimMaterial.REDSTONE,
        "minecraft:resin_brick" to TrimMaterial.RESIN
    )

    val potDecorationMaterialIds: List<String> = run {
        val ids = linkedSetOf<String>()
        ids += Material.BRICK.key.asString()
        Tag.ITEMS_DECORATED_POT_SHERDS.values
            .map { it.key.asString() }
            .sorted()
            .forEach(ids::add)
        ids.toList()
    }


    private val potMaterialById: Map<String, Material> = potDecorationMaterialIds.associateWith { id ->
        val material = Registry.MATERIAL.get(NamespacedKey.fromString(id) ?: NamespacedKey.minecraft(id.removePrefix("minecraft:")))
            ?: error("Missing material in registry for pot decoration id: $id")
        material
    }

    fun trimPatternIds(): List<String> = trimPatternByTemplateId.keys.toList()
    fun trimMaterialIds(): List<String> = trimMaterialByItemId.keys.toList()

    fun parseTrimPatternTemplateId(input: String): TrimPattern? = trimPatternByTemplateId[input.lowercase()]
    fun parseTrimMaterialItemId(input: String): TrimMaterial? = trimMaterialByItemId[input.lowercase()]

    fun parsePotDecorationMaterial(input: String): Material? = potMaterialById[input.lowercase()]

    fun parsePotSide(input: String): DecoratedPotSide? = when (input.lowercase()) {
        "back" -> DecoratedPotSide.BACK
        "left" -> DecoratedPotSide.LEFT
        "right" -> DecoratedPotSide.RIGHT
        "front" -> DecoratedPotSide.FRONT
        else -> null
    }

    fun sideOptions(): List<String> = listOf("back", "left", "right", "front")

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
