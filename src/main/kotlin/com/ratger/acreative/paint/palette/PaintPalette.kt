package com.ratger.acreative.paint.palette

import com.ratger.acreative.paint.map.MapColorMatcher
import com.ratger.acreative.paint.model.PaintShade
import net.minecraft.world.level.material.MapColor
import org.bukkit.Material

data class PaintPaletteEntry(
    val key: String,
    val displayName: String,
    val itemMaterial: Material,
    val hexColor: String,
    val mapColor: MapColor? = null
) {
    val isTransparent: Boolean
        get() = mapColor == null

    fun packed(shade: PaintShade): Byte {
        return mapColor?.getPackedId(MapColor.Brightness.byId(shade.actualId)) ?: MapColorMatcher.TRANSPARENT_COLOR_ID
    }
}

object PaintPalette {
    val NONE = PaintPaletteEntry("NONE", "Прозрачный", Material.STRUCTURE_VOID, "#FFD700")
    val FIRE = entry("FIRE", "Ярко-красный", Material.REDSTONE_BLOCK, "#FF0000", MapColor.FIRE)
    val CRIMSON_NYLIUM = entry("CRIMSON_NYLIUM", "Тёмно-малиновый", Material.CRIMSON_NYLIUM, "#BD3031", MapColor.CRIMSON_NYLIUM)
    val COLOR_RED = entry("COLOR_RED", "Красный", Material.RED_DYE, "#993333", MapColor.COLOR_RED)
    val TERRACOTTA_RED = entry("TERRACOTTA_RED", "Кирпично-красный", Material.RED_TERRACOTTA, "#8E3C2E", MapColor.TERRACOTTA_RED)
    val NETHER = entry("NETHER", "Тёмно-бордовый", Material.NETHERRACK, "#700200", MapColor.NETHER)
    val CRIMSON_HYPHAE = entry("CRIMSON_HYPHAE", "Тёмный бордовый", Material.CRIMSON_HYPHAE, "#5C191D", MapColor.CRIMSON_HYPHAE)
    val TERRACOTTA_PINK = entry("TERRACOTTA_PINK", "Кораллово-розовый", Material.PINK_TERRACOTTA, "#A04D4E", MapColor.TERRACOTTA_PINK)
    val COLOR_PINK = entry("COLOR_PINK", "Розовый", Material.PINK_DYE, "#F27FA5", MapColor.COLOR_PINK)
    val CRIMSON_STEM = entry("CRIMSON_STEM", "Винный", Material.CRIMSON_STEM, "#943F61", MapColor.CRIMSON_STEM)
    val TERRACOTTA_MAGENTA = entry("TERRACOTTA_MAGENTA", "Пыльно-розовый", Material.MAGENTA_TERRACOTTA, "#95576C", MapColor.TERRACOTTA_MAGENTA)
    val COLOR_MAGENTA = entry("COLOR_MAGENTA", "Малиновый", Material.MAGENTA_DYE, "#B24CD8", MapColor.COLOR_MAGENTA)
    val COLOR_PURPLE = entry("COLOR_PURPLE", "Фиолетовый", Material.PURPLE_DYE, "#7F3FB2", MapColor.COLOR_PURPLE)
    val TERRACOTTA_PURPLE = entry("TERRACOTTA_PURPLE", "Сливовый", Material.PURPLE_TERRACOTTA, "#7A4958", MapColor.TERRACOTTA_PURPLE)
    val WARPED_HYPHAE = entry("WARPED_HYPHAE", "Тёмная слива", Material.WARPED_HYPHAE, "#562C3E", MapColor.WARPED_HYPHAE)
    val TERRACOTTA_BLUE = entry("TERRACOTTA_BLUE", "Тёмно-фиолетовый", Material.BLUE_TERRACOTTA, "#4C3E5C", MapColor.TERRACOTTA_BLUE)
    val COLOR_BLUE = entry("COLOR_BLUE", "Тёмно-синий", Material.BLUE_DYE, "#334CB2", MapColor.COLOR_BLUE)
    val WATER = entry("WATER", "Яркий синий", Material.WATER_BUCKET, "#4040FF", MapColor.WATER)
    val LAPIS = entry("LAPIS", "Сапфировый синий", Material.LAPIS_BLOCK, "#4A80FF", MapColor.LAPIS)
    val COLOR_LIGHT_BLUE = entry("COLOR_LIGHT_BLUE", "Голубой", Material.LIGHT_BLUE_DYE, "#6699D8", MapColor.COLOR_LIGHT_BLUE)
    val ICE = entry("ICE", "Лавандово-голубой", Material.ICE, "#A0A0FF", MapColor.ICE)
    val TERRACOTTA_LIGHT_BLUE = entry("TERRACOTTA_LIGHT_BLUE", "Приглушённый голубой", Material.LIGHT_BLUE_TERRACOTTA, "#706C8A", MapColor.TERRACOTTA_LIGHT_BLUE)
    val DIAMOND = entry("DIAMOND", "Аквамариновый", Material.DIAMOND_BLOCK, "#5CDBD5", MapColor.DIAMOND)
    val WARPED_WART_BLOCK = entry("WARPED_WART_BLOCK", "Яркая бирюза", Material.WARPED_WART_BLOCK, "#14B485", MapColor.WARPED_WART_BLOCK)
    val WARPED_STEM = entry("WARPED_STEM", "Сине-зелёный", Material.WARPED_STEM, "#3A8E8C", MapColor.WARPED_STEM)
    val COLOR_CYAN = entry("COLOR_CYAN", "Бирюзовый", Material.CYAN_DYE, "#4C7F99", MapColor.COLOR_CYAN)
    val WARPED_NYLIUM = entry("WARPED_NYLIUM", "Тёмная бирюза", Material.WARPED_NYLIUM, "#167E86", MapColor.WARPED_NYLIUM)
    val TERRACOTTA_CYAN = entry("TERRACOTTA_CYAN", "Тёмная морская волна", Material.CYAN_TERRACOTTA, "#575C5C", MapColor.TERRACOTTA_CYAN)
    val EMERALD = entry("EMERALD", "Изумрудный", Material.EMERALD_BLOCK, "#00D93A", MapColor.EMERALD)
    val PLANT = entry("PLANT", "Насыщенный зелёный", Material.SHORT_GRASS, "#007C00", MapColor.PLANT)
    val COLOR_LIGHT_GREEN = entry("COLOR_LIGHT_GREEN", "Салатовый", Material.LIME_DYE, "#7FCC19", MapColor.COLOR_LIGHT_GREEN)
    val GRASS = entry("GRASS", "Травяной зелёный", Material.GRASS_BLOCK, "#7FB238", MapColor.GRASS)
    val GLOW_LICHEN = entry("GLOW_LICHEN", "Бледно-зелёный", Material.GLOW_LICHEN, "#7FA796", MapColor.GLOW_LICHEN)
    val COLOR_GREEN = entry("COLOR_GREEN", "Тёмно-зелёный", Material.GREEN_DYE, "#667F33", MapColor.COLOR_GREEN)
    val TERRACOTTA_LIGHT_GREEN = entry("TERRACOTTA_LIGHT_GREEN", "Оливковый", Material.LIME_TERRACOTTA, "#677535", MapColor.TERRACOTTA_LIGHT_GREEN)
    val TERRACOTTA_GREEN = entry("TERRACOTTA_GREEN", "Болотный зелёный", Material.GREEN_TERRACOTTA, "#4C522A", MapColor.TERRACOTTA_GREEN)
    val GOLD = entry("GOLD", "Золотой", Material.GOLD_BLOCK, "#FAEE4D", MapColor.GOLD)
    val COLOR_YELLOW = entry("COLOR_YELLOW", "Жёлтый", Material.YELLOW_DYE, "#E5E533", MapColor.COLOR_YELLOW)
    val SAND = entry("SAND", "Бежевый", Material.SAND, "#F7E9A3", MapColor.SAND)
    val TERRACOTTA_WHITE = entry("TERRACOTTA_WHITE", "Кремовый", Material.WHITE_TERRACOTTA, "#D1B1A1", MapColor.TERRACOTTA_WHITE)
    val RAW_IRON = entry("RAW_IRON", "Ржаво-бежевый", Material.RAW_IRON_BLOCK, "#D8AF93", MapColor.RAW_IRON)
    val TERRACOTTA_YELLOW = entry("TERRACOTTA_YELLOW", "Охристый", Material.YELLOW_TERRACOTTA, "#BA8524", MapColor.TERRACOTTA_YELLOW)
    val COLOR_ORANGE = entry("COLOR_ORANGE", "Оранжевый", Material.ORANGE_DYE, "#D87F33", MapColor.COLOR_ORANGE)
    val TERRACOTTA_ORANGE = entry("TERRACOTTA_ORANGE", "Терракотовый оранжевый", Material.ORANGE_TERRACOTTA, "#9F5224", MapColor.TERRACOTTA_ORANGE)
    val WOOD = entry("WOOD", "Древесный коричневый", Material.OAK_PLANKS, "#8F7748", MapColor.WOOD)
    val DIRT = entry("DIRT", "Земляной коричневый", Material.DIRT, "#976D4D", MapColor.DIRT)
    val TERRACOTTA_LIGHT_GRAY = entry("TERRACOTTA_LIGHT_GRAY", "Серо-коричневый", Material.LIGHT_GRAY_TERRACOTTA, "#876B62", MapColor.TERRACOTTA_LIGHT_GRAY)
    val PODZOL = entry("PODZOL", "Грязно-коричневый", Material.PODZOL, "#815631", MapColor.PODZOL)
    val COLOR_BROWN = entry("COLOR_BROWN", "Коричневый", Material.BROWN_DYE, "#664C33", MapColor.COLOR_BROWN)
    val TERRACOTTA_BROWN = entry("TERRACOTTA_BROWN", "Кофейный", Material.BROWN_TERRACOTTA, "#4C3223", MapColor.TERRACOTTA_BROWN)
    val TERRACOTTA_GRAY = entry("TERRACOTTA_GRAY", "Тёмный шоколад", Material.GRAY_TERRACOTTA, "#392923", MapColor.TERRACOTTA_GRAY)
    val QUARTZ = entry("QUARTZ", "Молочный белый", Material.QUARTZ_BLOCK, "#FFFCF5", MapColor.QUARTZ)
    val SNOW = entry("SNOW", "Белый", Material.SNOW_BLOCK, "#FFFFFF", MapColor.SNOW)
    val WOOL = entry("WOOL", "Светло-серый", Material.WHITE_WOOL, "#C7C7C7", MapColor.WOOL)
    val CLAY = entry("CLAY", "Серо-голубой", Material.CLAY, "#A4A8B8", MapColor.CLAY)
    val METAL = entry("METAL", "Серебристый", Material.IRON_BLOCK, "#A7A7A7", MapColor.METAL)
    val COLOR_LIGHT_GRAY = entry("COLOR_LIGHT_GRAY", "Серый", Material.LIGHT_GRAY_DYE, "#999999", MapColor.COLOR_LIGHT_GRAY)
    val STONE = entry("STONE", "Каменный серый", Material.STONE, "#707070", MapColor.STONE)
    val DEEPSLATE = entry("DEEPSLATE", "Графитовый", Material.DEEPSLATE, "#646464", MapColor.DEEPSLATE)
    val COLOR_GRAY = entry("COLOR_GRAY", "Тёмно-серый", Material.GRAY_DYE, "#4C4C4C", MapColor.COLOR_GRAY)
    val TERRACOTTA_BLACK = entry("TERRACOTTA_BLACK", "Почти чёрный", Material.BLACK_TERRACOTTA, "#251610", MapColor.TERRACOTTA_BLACK)
    val COLOR_BLACK = entry("COLOR_BLACK", "Чёрный", Material.BLACK_DYE, "#191919", MapColor.COLOR_BLACK)

    val entries: List<PaintPaletteEntry> = listOf(
        NONE,
        FIRE, CRIMSON_NYLIUM, COLOR_RED, TERRACOTTA_RED, NETHER, CRIMSON_HYPHAE,
        TERRACOTTA_PINK, COLOR_PINK, CRIMSON_STEM, TERRACOTTA_MAGENTA, COLOR_MAGENTA,
        COLOR_PURPLE, TERRACOTTA_PURPLE, WARPED_HYPHAE, TERRACOTTA_BLUE,
        COLOR_BLUE, WATER, LAPIS, COLOR_LIGHT_BLUE, ICE, TERRACOTTA_LIGHT_BLUE,
        DIAMOND, WARPED_WART_BLOCK, WARPED_STEM, COLOR_CYAN, WARPED_NYLIUM, TERRACOTTA_CYAN,
        EMERALD, PLANT, COLOR_LIGHT_GREEN, GRASS, GLOW_LICHEN, COLOR_GREEN, TERRACOTTA_LIGHT_GREEN, TERRACOTTA_GREEN,
        GOLD, COLOR_YELLOW, SAND, TERRACOTTA_WHITE, RAW_IRON, TERRACOTTA_YELLOW,
        COLOR_ORANGE, TERRACOTTA_ORANGE,
        WOOD, DIRT, TERRACOTTA_LIGHT_GRAY, PODZOL, COLOR_BROWN, TERRACOTTA_BROWN, TERRACOTTA_GRAY,
        QUARTZ, SNOW, WOOL, CLAY, METAL, COLOR_LIGHT_GRAY, STONE, DEEPSLATE, COLOR_GRAY, TERRACOTTA_BLACK, COLOR_BLACK
    )

    private val byKey = entries.associateBy { it.key }

    fun entry(key: String): PaintPaletteEntry = byKey[key] ?: SNOW

    private fun entry(
        key: String,
        displayName: String,
        itemMaterial: Material,
        hexColor: String,
        mapColor: MapColor
    ): PaintPaletteEntry {
        return PaintPaletteEntry(
            key = key,
            displayName = displayName,
            itemMaterial = itemMaterial,
            hexColor = hexColor,
            mapColor = mapColor
        )
    }
}
