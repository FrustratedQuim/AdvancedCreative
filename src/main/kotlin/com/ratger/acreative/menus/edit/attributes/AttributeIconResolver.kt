package com.ratger.acreative.menus.edit.attributes

import org.bukkit.Material
import org.bukkit.attribute.Attribute

object AttributeIconResolver {
    private val byKey = mapOf(
        "max_health" to Material.GOLDEN_APPLE,
        "max_absorption" to Material.ENCHANTED_GOLDEN_APPLE,
        "armor" to Material.IRON_CHESTPLATE,
        "armor_toughness" to Material.NETHERITE_CHESTPLATE,
        "knockback_resistance" to Material.SHIELD,
        "explosion_knockback_resistance" to Material.TNT_MINECART,
        "safe_fall_distance" to Material.HAY_BLOCK,
        "fall_damage_multiplier" to Material.FEATHER,
        "burning_time" to Material.BLAZE_POWDER,
        "oxygen_bonus" to Material.TURTLE_HELMET,
        "water_movement_efficiency" to Material.WATER_BUCKET,
        "movement_efficiency" to Material.LEATHER_BOOTS,
        "movement_speed" to Material.SUGAR,
        "flying_speed" to Material.ELYTRA,
        "sneaking_speed" to Material.SCULK,
        "step_height" to Material.RABBIT_FOOT,
        "jump_strength" to Material.SLIME_BALL,
        "gravity" to Material.ANVIL,
        "scale" to Material.SPYGLASS,
        "attack_damage" to Material.GOLDEN_SWORD,
        "attack_speed" to Material.GOLDEN_AXE,
        "attack_knockback" to Material.STICK,
        "sweeping_damage_ratio" to Material.NETHERITE_SWORD,
        "luck" to Material.EMERALD,
        "follow_range" to Material.SPYGLASS,
        "tempt_range" to Material.CARROT_ON_A_STICK,
        "entity_interaction_range" to Material.LEAD,
        "block_interaction_range" to Material.BRUSH,
        "block_break_speed" to Material.GOLDEN_PICKAXE,
        "mining_efficiency" to Material.NETHERITE_PICKAXE,
        "submerged_mining_speed" to Material.PRISMARINE_SHARD,
        "spawn_reinforcements" to Material.TOTEM_OF_UNDYING
    )

    fun resolve(attribute: Attribute): Material {
        return byKey[attribute.key.key] ?: Material.NETHER_STAR
    }
}

