package com.ratger.acreative.commands.paint

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.world.level.material.MapColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

enum class PaintToolMode {
    COLOR_BRUSH,
    BRUSH,
    CUT,
    FILL,
    SHAPE
}

data class PaintToolDefinition(
    val id: String,
    val slot: Int,
    val material: Material,
    val mode: PaintToolMode,
    val displayName: Component,
    val lore: List<Component>,
    val itemModel: String? = null,
    val brushHex: String? = null,
    val mapColor: Byte? = null
)

object PaintToolCatalog {

    private val loreLine = plain(
        Component.text("Q, ", hex("#FFD700"))
            .append(Component.text("чтобы настроить", hex("#FFE68A")))
    )

    val tools: List<PaintToolDefinition> = listOf(
        colorBrush("brush_black", 0, Material.BLACK_DYE, "Чёрная", "#000000"),
        colorBrush("brush_gray", 1, Material.GRAY_DYE, "Серая", "#808080"),
        colorBrush("brush_light_gray", 2, Material.LIGHT_GRAY_DYE, "Светло серая", "#D3D3D3"),
        colorBrush("brush_white", 3, Material.WHITE_DYE, "Белая", "#FFFFFF"),
        specialBrush(),

        colorBrush("brush_brown", 9, Material.BROWN_DYE, "Коричневая", "#8B4513"),
        colorBrush("brush_green", 10, Material.GREEN_DYE, "Зелёная", "#008000"),
        colorBrush("brush_lime", 11, Material.LIME_DYE, "Лаймовая", "#00FF00"),
        shapeTool(),

        colorBrush("brush_purple", 18, Material.PURPLE_DYE, "Фиолетовая", "#800080"),
        colorBrush("brush_magenta", 19, Material.MAGENTA_DYE, "Пурпурная", "#FF00FF"),
        colorBrush("brush_pink", 20, Material.PINK_DYE, "Розовая", "#FFC0CB"),
        fillTool(),

        colorBrush("brush_red", 27, Material.RED_DYE, "Красная", "#FF0000"),
        colorBrush("brush_orange", 28, Material.ORANGE_DYE, "Оранжевая", "#FFA500"),
        colorBrush("brush_yellow", 29, Material.YELLOW_DYE, "Жёлтая", "#FFFF00"),
        colorBrush("brush_blue", 30, Material.BLUE_DYE, "Синяя", "#0000FF"),
        colorBrush("brush_cyan", 31, Material.CYAN_DYE, "Бирюзовая", "#00FFFF"),
        colorBrush("brush_light_blue", 32, Material.LIGHT_BLUE_DYE, "Голубая", "#55FFFF"),
        cutTool()
    )

    private val byId = tools.associateBy { it.id }

    fun buildLayout(toolKey: NamespacedKey, modeKey: NamespacedKey): Array<ItemStack?> {
        val contents = arrayOfNulls<ItemStack>(36)
        tools.forEach { tool ->
            contents[tool.slot] = buildItem(tool, toolKey, modeKey)
        }
        return contents
    }

    fun resolve(item: ItemStack?, toolKey: NamespacedKey): PaintToolDefinition? {
        val meta = item?.itemMeta ?: return null
        val toolId = meta.persistentDataContainer.get(toolKey, PersistentDataType.STRING) ?: return null
        return byId[toolId]
    }

    private fun buildItem(tool: PaintToolDefinition, toolKey: NamespacedKey, modeKey: NamespacedKey): ItemStack {
        return ItemStack(tool.material).apply {
            editMeta { meta ->
                meta.displayName(tool.displayName)
                meta.lore(tool.lore)
                tool.itemModel?.let {
                    meta.itemModel = NamespacedKey.fromString(it) ?: NamespacedKey.minecraft(it.removePrefix("minecraft:"))
                }
                meta.persistentDataContainer.set(toolKey, PersistentDataType.STRING, tool.id)
                meta.persistentDataContainer.set(modeKey, PersistentDataType.STRING, tool.mode.name)
            }
        }
    }

    private fun colorBrush(id: String, slot: Int, material: Material, title: String, hexValue: String): PaintToolDefinition {
        return PaintToolDefinition(
            id = id,
            slot = slot,
            material = material,
            mode = PaintToolMode.COLOR_BRUSH,
            displayName = plain(
                Component.text("✎", hex("#C7A300"))
                    .append(Component.text(" Кисточка: ", hex("#FFD700")))
                    .append(Component.text(title, hex(hexValue)))
            ),
            lore = listOf(loreLine),
            brushHex = hexValue,
            mapColor = packedBaseColor(resolveBaseBrushColor(material))
        )
    }

    private fun resolveBaseBrushColor(material: Material): MapColor = when (material) {
        Material.WHITE_DYE -> MapColor.SNOW
        Material.ORANGE_DYE -> MapColor.COLOR_ORANGE
        Material.MAGENTA_DYE -> MapColor.COLOR_MAGENTA
        Material.LIGHT_BLUE_DYE -> MapColor.COLOR_LIGHT_BLUE
        Material.YELLOW_DYE -> MapColor.COLOR_YELLOW
        Material.LIME_DYE -> MapColor.COLOR_LIGHT_GREEN
        Material.PINK_DYE -> MapColor.COLOR_PINK
        Material.GRAY_DYE -> MapColor.COLOR_GRAY
        Material.LIGHT_GRAY_DYE -> MapColor.COLOR_LIGHT_GRAY
        Material.CYAN_DYE -> MapColor.COLOR_CYAN
        Material.PURPLE_DYE -> MapColor.COLOR_PURPLE
        Material.BLUE_DYE -> MapColor.COLOR_BLUE
        Material.BROWN_DYE -> MapColor.COLOR_BROWN
        Material.GREEN_DYE -> MapColor.COLOR_GREEN
        Material.RED_DYE -> MapColor.COLOR_RED
        Material.BLACK_DYE -> MapColor.COLOR_BLACK
        else -> error("Unknown material: $material")
    }

    private fun packedBaseColor(color: MapColor): Byte = color.getPackedId(MapColor.Brightness.NORMAL)

    private fun specialBrush(): PaintToolDefinition {
        val hexValue = "#FFFFFF"
        return PaintToolDefinition(
            id = "tool_brush",
            slot = 8,
            material = Material.WHITE_DYE,
            mode = PaintToolMode.BRUSH,
            displayName = plain(
                Component.text("✎", hex("#C7A300"))
                    .append(Component.text(" Кисточка: ", hex("#FFD700")))
                    .append(Component.text(hexValue, hex("#FFFFFF")))
            ),
            lore = listOf(loreLine),
            itemModel = "minecraft:brush",
            brushHex = hexValue
        )
    }

    private fun cutTool(): PaintToolDefinition {
        return PaintToolDefinition(
            id = "tool_cut",
            slot = 35,
            material = Material.WHITE_DYE,
            mode = PaintToolMode.CUT,
            displayName = plain(
                Component.text("✂", hex("#C7A300"))
                    .append(Component.text(" Вырезать", hex("#FFD700")))
            ),
            lore = listOf(loreLine),
            itemModel = "minecraft:shears",
            mapColor = MapColorMatcher.TRANSPARENT_COLOR_ID
        )
    }

    private fun fillTool(): PaintToolDefinition {
        val hexValue = "#FFFFFF"
        return PaintToolDefinition(
            id = "tool_fill",
            slot = 26,
            material = Material.WHITE_DYE,
            mode = PaintToolMode.FILL,
            displayName = plain(
                Component.text("🌧", hex("#C7A300"))
                    .append(Component.text(" Заливка: ", hex("#FFD700")))
                    .append(Component.text(hexValue, hex("#FFFFFF")))
            ),
            lore = listOf(loreLine),
            itemModel = "minecraft:powder_snow_bucket",
            brushHex = hexValue
        )
    }

    private fun shapeTool(): PaintToolDefinition {
        return PaintToolDefinition(
            id = "tool_shape",
            slot = 17,
            material = Material.WHITE_DYE,
            mode = PaintToolMode.SHAPE,
            displayName = plain(
                Component.text("⭐", hex("#C7A300"))
                    .append(Component.text(" Фигура: ", hex("#FFD700")))
                    .append(Component.text("Круг ", hex("#FFF3E0")))
                    .append(Component.text("[", hex("#C7A300")))
                    .append(Component.text("#F46567", hex("#F46567")))
                    .append(Component.text("]", hex("#C7A300")))
            ),
            lore = listOf(loreLine),
            itemModel = "minecraft:slime_ball"
        )
    }

    private fun plain(component: Component): Component = component.decoration(TextDecoration.ITALIC, false)

    private fun hex(value: String): TextColor = TextColor.fromHexString(value) ?: NamedTextColor.WHITE
}
