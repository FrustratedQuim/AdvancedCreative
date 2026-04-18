package com.ratger.acreative.menus.edit.enchant

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

object EnchantmentIconResolver {
    private val byKey = mapOf(
        "unbreaking" to Material.ANVIL,
        "mending" to Material.EXPERIENCE_BOTTLE,

        "efficiency" to Material.GOLDEN_PICKAXE,
        "fortune" to Material.DIAMOND,
        "silk_touch" to Material.BRUSH,

        "sharpness" to Material.GOLDEN_SWORD,
        "smite" to Material.GOLDEN_AXE,
        "bane_of_arthropods" to Material.SPIDER_EYE,
        "knockback" to Material.STICK,
        "fire_aspect" to Material.BLAZE_POWDER,
        "looting" to Material.RABBIT_FOOT,
        "sweeping_edge" to Material.NETHERITE_SWORD,

        "power" to Material.BOW,
        "punch" to Material.WIND_CHARGE,
        "flame" to Material.FIRE_CHARGE,
        "infinity" to Material.ENDER_EYE,

        "protection" to Material.IRON_CHESTPLATE,
        "fire_protection" to Material.MAGMA_CREAM,
        "blast_protection" to Material.TNT,
        "projectile_protection" to Material.ARROW,
        "feather_falling" to Material.FEATHER,
        "respiration" to Material.TURTLE_HELMET,
        "aqua_affinity" to Material.WATER_BUCKET,
        "thorns" to Material.CACTUS,
        "depth_strider" to Material.PRISMARINE_SHARD,
        "frost_walker" to Material.BLUE_ICE,
        "soul_speed" to Material.SOUL_SOIL,
        "swift_sneak" to Material.SCULK,

        "impaling" to Material.TRIDENT,
        "riptide" to Material.HEART_OF_THE_SEA,
        "loyalty" to Material.LEAD,
        "channeling" to Material.LIGHTNING_ROD,

        "lure" to Material.FISHING_ROD,
        "luck_of_the_sea" to Material.NAUTILUS_SHELL,

        "quick_charge" to Material.CROSSBOW,
        "multishot" to Material.FIREWORK_ROCKET,
        "piercing" to Material.SPECTRAL_ARROW,

        "breach" to Material.MACE,
        "density" to Material.HEAVY_CORE,
        "wind_burst" to Material.WIND_CHARGE,

        "binding_curse" to Material.CHAIN,
        "vanishing_curse" to Material.BARRIER
    )

    fun resolve(enchantment: Enchantment): Material {
        val key = EnchantmentSupport.keyPath(enchantment)
        return byKey[key] ?: Material.ENCHANTED_BOOK
    }
}
