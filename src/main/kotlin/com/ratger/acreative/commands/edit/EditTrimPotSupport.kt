package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.item.PotDecorations
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern

object EditTrimPotSupport {
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

    val potDecorationMaterialIds: List<String> = listOf(
        "minecraft:brick",
        "minecraft:explorer_pottery_sherd",
        "minecraft:flow_pottery_sherd",
        "minecraft:danger_pottery_sherd",
        "minecraft:burn_pottery_sherd",
        "minecraft:brewer_pottery_sherd",
        "minecraft:blade_pottery_sherd",
        "minecraft:archer_pottery_sherd",
        "minecraft:angler_pottery_sherd",
        "minecraft:arms_up_pottery_sherd",
        "minecraft:scrape_pottery_sherd",
        "minecraft:friend_pottery_sherd",
        "minecraft:guster_pottery_sherd",
        "minecraft:heart_pottery_sherd",
        "minecraft:heartbreak_pottery_sherd",
        "minecraft:howl_pottery_sherd",
        "minecraft:miner_pottery_sherd",
        "minecraft:mourner_pottery_sherd",
        "minecraft:plenty_pottery_sherd",
        "minecraft:prize_pottery_sherd",
        "minecraft:snort_pottery_sherd",
        "minecraft:skull_pottery_sherd",
        "minecraft:shelter_pottery_sherd",
        "minecraft:sheaf_pottery_sherd"
    )

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

    fun potDecorations(back: Material?, left: Material?, right: Material?, front: Material?): PotDecorations {
        return PotDecorations.potDecorations(back?.asItemType(), left?.asItemType(), right?.asItemType(), front?.asItemType())
    }
}

enum class DecoratedPotSide {
    BACK,
    LEFT,
    RIGHT,
    FRONT
}
