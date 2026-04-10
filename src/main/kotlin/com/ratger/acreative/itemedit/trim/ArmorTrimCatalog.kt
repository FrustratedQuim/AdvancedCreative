package com.ratger.acreative.itemedit.trim

import org.bukkit.Material
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern

object ArmorTrimCatalog {
    data class PatternDescriptor(
        val pattern: TrimPattern,
        val slot: Int,
        val icon: Material,
        val displayName: String
    )

    data class MaterialDescriptor(
        val material: TrimMaterial,
        val slot: Int,
        val icon: Material,
        val displayName: String
    )

    val orderedPatterns: List<PatternDescriptor> = listOf(
        PatternDescriptor(TrimPattern.WAYFINDER, 11, Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, "Искатель"),
        PatternDescriptor(TrimPattern.RAISER, 12, Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, "Собиратель"),
        PatternDescriptor(TrimPattern.SHAPER, 13, Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, "Скульптор"),
        PatternDescriptor(TrimPattern.HOST, 14, Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, "Вождь"),
        PatternDescriptor(TrimPattern.RIB, 15, Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, "Ребро"),
        PatternDescriptor(TrimPattern.WARD, 20, Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, "Хранитель"),
        PatternDescriptor(TrimPattern.SENTRY, 21, Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Страж"),
        PatternDescriptor(TrimPattern.VEX, 22, Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, "Вредина"),
        PatternDescriptor(TrimPattern.WILD, 23, Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, "Дебри"),
        PatternDescriptor(TrimPattern.BOLT, 24, Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "Ось"),
        PatternDescriptor(TrimPattern.DUNE, 29, Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, "Дюна"),
        PatternDescriptor(TrimPattern.EYE, 30, Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, "Око"),
        PatternDescriptor(TrimPattern.TIDE, 31, Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, "Прилив"),
        PatternDescriptor(TrimPattern.FLOW, 32, Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "Поток"),
        PatternDescriptor(TrimPattern.SPIRE, 33, Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, "Шпиль"),
        PatternDescriptor(TrimPattern.SILENCE, 39, Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "Тишина"),
        PatternDescriptor(TrimPattern.COAST, 40, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Берег"),
        PatternDescriptor(TrimPattern.SNOUT, 41, Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, "Рыло")
    )

    val orderedMaterials: List<MaterialDescriptor> = listOf(
        MaterialDescriptor(TrimMaterial.IRON, 11, Material.IRON_INGOT, "Железо"),
        MaterialDescriptor(TrimMaterial.COPPER, 12, Material.COPPER_INGOT, "Медь"),
        MaterialDescriptor(TrimMaterial.RESIN, 13, Material.RESIN_BRICK, "Смола"),
        MaterialDescriptor(TrimMaterial.GOLD, 14, Material.GOLD_INGOT, "Золото"),
        MaterialDescriptor(TrimMaterial.NETHERITE, 15, Material.NETHERITE_INGOT, "Незерит"),
        MaterialDescriptor(TrimMaterial.AMETHYST, 22, Material.AMETHYST_SHARD, "Аметист"),
        MaterialDescriptor(TrimMaterial.REDSTONE, 29, Material.REDSTONE, "Редстоун"),
        MaterialDescriptor(TrimMaterial.LAPIS, 30, Material.LAPIS_LAZULI, "Лазурит"),
        MaterialDescriptor(TrimMaterial.QUARTZ, 31, Material.QUARTZ, "Кварц"),
        MaterialDescriptor(TrimMaterial.EMERALD, 32, Material.EMERALD, "Изумруд"),
        MaterialDescriptor(TrimMaterial.DIAMOND, 33, Material.DIAMOND, "Алмаз")
    )

    val patternByKey: Map<TrimPattern, PatternDescriptor> = orderedPatterns.associateBy { it.pattern }
    val materialByKey: Map<TrimMaterial, MaterialDescriptor> = orderedMaterials.associateBy { it.material }
}
