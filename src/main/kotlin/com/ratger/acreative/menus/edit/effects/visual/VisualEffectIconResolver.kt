package com.ratger.acreative.menus.edit.effects.visual

import org.bukkit.Material
import org.bukkit.potion.PotionEffectType

object VisualEffectIconResolver {
    private val byKey = mapOf(
        "speed" to Material.SUGAR,
        "slowness" to Material.COBWEB,
        "haste" to Material.GOLDEN_PICKAXE,
        "mining_fatigue" to Material.STONE_PICKAXE,
        "strength" to Material.IRON_SWORD,
        "instant_health" to Material.GLISTERING_MELON_SLICE,
        "instant_damage" to Material.FERMENTED_SPIDER_EYE,
        "jump_boost" to Material.RABBIT_FOOT,
        "nausea" to Material.SPIDER_EYE,
        "regeneration" to Material.GHAST_TEAR,
        "resistance" to Material.SHIELD,
        "fire_resistance" to Material.MAGMA_CREAM,
        "water_breathing" to Material.PUFFERFISH,
        "invisibility" to Material.GLASS,
        "blindness" to Material.INK_SAC,
        "night_vision" to Material.GOLDEN_CARROT,
        "hunger" to Material.ROTTEN_FLESH,
        "weakness" to Material.WOODEN_SWORD,
        "poison" to Material.POISONOUS_POTATO,
        "wither" to Material.WITHER_ROSE,
        "health_boost" to Material.GOLDEN_APPLE,
        "absorption" to Material.ENCHANTED_GOLDEN_APPLE,
        "saturation" to Material.COOKED_BEEF,
        "glowing" to Material.GLOW_INK_SAC,
        "levitation" to Material.SHULKER_SHELL,
        "luck" to Material.EMERALD,
        "unluck" to Material.BLACK_DYE,
        "slow_falling" to Material.FEATHER,
        "conduit_power" to Material.CONDUIT,
        "dolphins_grace" to Material.DOLPHIN_SPAWN_EGG,
        "bad_omen" to Material.OMINOUS_BOTTLE,
        "hero_of_the_village" to Material.BELL,
        "darkness" to Material.SCULK,
        "trial_omen" to Material.TRIAL_KEY,
        "raid_omen" to Material.OMINOUS_TRIAL_KEY,
        "wind_charged" to Material.WIND_CHARGE,
        "weaving" to Material.COBWEB,
        "oozing" to Material.SLIME_BALL,
        "infested" to Material.SILVERFISH_SPAWN_EGG
    )

    fun resolve(type: PotionEffectType): Material {
        val key = type.key.key
        return byKey[key] ?: Material.POTION
    }
}
