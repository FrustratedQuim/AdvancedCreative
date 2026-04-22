package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerCategory
import com.ratger.acreative.menus.banner.model.BannerColorDescriptor
import com.ratger.acreative.menus.banner.model.BannerPatternDescriptor
import com.ratger.acreative.menus.banner.model.BannerSort
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.PatternType

object BannerCatalog {
    val publishCategories: List<BannerCategory> = listOf(
        BannerCategory.MISCELLANEOUS,
        BannerCategory.SYMBOLS,
        BannerCategory.DECORATION
    )

    val galleryCategories: List<BannerCategory> = listOf(BannerCategory.ALL) + publishCategories

    val sorts: List<BannerSort> = listOf(
        BannerSort.POPULAR,
        BannerSort.NEW,
        BannerSort.OLD
    )

    val colors: List<BannerColorDescriptor> = listOf(
        BannerColorDescriptor(DyeColor.WHITE, Material.WHITE_BANNER, Material.WHITE_DYE, "Белый"),
        BannerColorDescriptor(DyeColor.LIGHT_GRAY, Material.LIGHT_GRAY_BANNER, Material.LIGHT_GRAY_DYE, "Светло серый"),
        BannerColorDescriptor(DyeColor.GRAY, Material.GRAY_BANNER, Material.GRAY_DYE, "Серый"),
        BannerColorDescriptor(DyeColor.BLACK, Material.BLACK_BANNER, Material.BLACK_DYE, "Чёрный"),
        BannerColorDescriptor(DyeColor.YELLOW, Material.YELLOW_BANNER, Material.YELLOW_DYE, "Жёлтый"),
        BannerColorDescriptor(DyeColor.ORANGE, Material.ORANGE_BANNER, Material.ORANGE_DYE, "Оранжевый"),
        BannerColorDescriptor(DyeColor.RED, Material.RED_BANNER, Material.RED_DYE, "Красный"),
        BannerColorDescriptor(DyeColor.PINK, Material.PINK_BANNER, Material.PINK_DYE, "Розовый"),
        BannerColorDescriptor(DyeColor.MAGENTA, Material.MAGENTA_BANNER, Material.MAGENTA_DYE, "Пурпурный"),
        BannerColorDescriptor(DyeColor.PURPLE, Material.PURPLE_BANNER, Material.PURPLE_DYE, "Фиолетовый"),
        BannerColorDescriptor(DyeColor.LIGHT_BLUE, Material.LIGHT_BLUE_BANNER, Material.LIGHT_BLUE_DYE, "Голубой"),
        BannerColorDescriptor(DyeColor.CYAN, Material.CYAN_BANNER, Material.CYAN_DYE, "Бирюзовый"),
        BannerColorDescriptor(DyeColor.BLUE, Material.BLUE_BANNER, Material.BLUE_DYE, "Синий"),
        BannerColorDescriptor(DyeColor.BROWN, Material.BROWN_BANNER, Material.BROWN_DYE, "Коричневый"),
        BannerColorDescriptor(DyeColor.LIME, Material.LIME_BANNER, Material.LIME_DYE, "Лаймовый"),
        BannerColorDescriptor(DyeColor.GREEN, Material.GREEN_BANNER, Material.GREEN_DYE, "Зелёный")
    )

    val patterns: List<BannerPatternDescriptor> = listOf(
        BannerPatternDescriptor(PatternType.SQUARE_BOTTOM_LEFT, "Белый нижний правый крыж"),
        BannerPatternDescriptor(PatternType.SQUARE_BOTTOM_RIGHT, "Белый нижний левый крыж"),
        BannerPatternDescriptor(PatternType.SQUARE_TOP_LEFT, "Белый верхний правый крыж"),
        BannerPatternDescriptor(PatternType.SQUARE_TOP_RIGHT, "Белый верхний левый крыж"),
        BannerPatternDescriptor(PatternType.STRIPE_BOTTOM, "Белое основание"),
        BannerPatternDescriptor(PatternType.STRIPE_TOP, "Белая глава"),
        BannerPatternDescriptor(PatternType.STRIPE_LEFT, "Белый правый столб"),
        BannerPatternDescriptor(PatternType.STRIPE_RIGHT, "Белый левый столб"),
        BannerPatternDescriptor(PatternType.STRIPE_CENTER, "Белый столб"),
        BannerPatternDescriptor(PatternType.STRIPE_MIDDLE, "Белый пояс"),
        BannerPatternDescriptor(PatternType.STRIPE_DOWNLEFT, "Белая левая перевязь"),
        BannerPatternDescriptor(PatternType.STRIPE_DOWNRIGHT, "Белая правая перевязь"),
        BannerPatternDescriptor(PatternType.SMALL_STRIPES, "Белые столбы"),
        BannerPatternDescriptor(PatternType.CROSS, "Белый косой крест"),
        BannerPatternDescriptor(PatternType.STRAIGHT_CROSS, "Белый прямой крест"),
        BannerPatternDescriptor(PatternType.TRIANGLE_BOTTOM, "Белое остриё"),
        BannerPatternDescriptor(PatternType.TRIANGLE_TOP, "Белое опрокинутое остриё"),
        BannerPatternDescriptor(PatternType.TRIANGLES_BOTTOM, "Белый зубчатый низ"),
        BannerPatternDescriptor(PatternType.TRIANGLES_TOP, "Белый зубчатый верх"),
        BannerPatternDescriptor(PatternType.DIAGONAL_LEFT, "Белая правая половина"),
        BannerPatternDescriptor(PatternType.DIAGONAL_RIGHT, "Белая левая половина"),
        BannerPatternDescriptor(PatternType.DIAGONAL_UP_LEFT, "Белая перевёрнутая правая половина"),
        BannerPatternDescriptor(PatternType.DIAGONAL_UP_RIGHT, "Белая перевёрнутая левая половина"),
        BannerPatternDescriptor(PatternType.HALF_VERTICAL, "Белая правая половина"),
        BannerPatternDescriptor(PatternType.HALF_VERTICAL_RIGHT, "Белая левая половина"),
        BannerPatternDescriptor(PatternType.HALF_HORIZONTAL, "Белая верхняя половина"),
        BannerPatternDescriptor(PatternType.HALF_HORIZONTAL_BOTTOM, "Белая нижняя половина"),
        BannerPatternDescriptor(PatternType.CIRCLE, "Белый круг"),
        BannerPatternDescriptor(PatternType.RHOMBUS, "Белый ромб"),
        BannerPatternDescriptor(PatternType.BORDER, "Белая кайма"),
        BannerPatternDescriptor(PatternType.CURLY_BORDER, "Белая фигурная кайма"),
        BannerPatternDescriptor(PatternType.GRADIENT, "Белый градиент"),
        BannerPatternDescriptor(PatternType.GRADIENT_UP, "Белый перевёрнутый градиент"),
        BannerPatternDescriptor(PatternType.BRICKS, "Белая муровка"),
        BannerPatternDescriptor(PatternType.GLOBE, "Белый шар"),
        BannerPatternDescriptor(PatternType.CREEPER, "Белый крипер"),
        BannerPatternDescriptor(PatternType.SKULL, "Белый череп"),
        BannerPatternDescriptor(PatternType.FLOWER, "Белый цветок"),
        BannerPatternDescriptor(PatternType.MOJANG, "Белый знак"),
        BannerPatternDescriptor(PatternType.PIGLIN, "Белое рыло"),
        BannerPatternDescriptor(PatternType.FLOW, "Белый поток"),
        BannerPatternDescriptor(PatternType.GUSTER, "Белый порыв")
    )

    fun colorByDye(color: DyeColor): BannerColorDescriptor? = colors.firstOrNull { it.dyeColor == color }

    fun colorByBannerMaterial(material: Material): BannerColorDescriptor? =
        colors.firstOrNull { it.bannerMaterial == material }

    fun patternByType(type: PatternType): BannerPatternDescriptor? = patterns.firstOrNull { it.patternType == type }
}
