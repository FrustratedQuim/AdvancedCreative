package com.ratger.acreative.menus.paint

import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.paint.model.PaintBinaryBrushSettings
import com.ratger.acreative.paint.model.PaintBrushSettings
import com.ratger.acreative.paint.model.PaintFillSettings
import com.ratger.acreative.paint.model.PaintSession
import com.ratger.acreative.paint.model.PaintShade
import com.ratger.acreative.paint.model.PaintShapeSettings
import com.ratger.acreative.paint.model.PaintToolMode
import com.ratger.acreative.paint.palette.PaintPalette
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

data class PaintToolDefinition(
    val id: String,
    val slot: Int,
    val material: Material,
    val mode: PaintToolMode,
    val fixedPaletteKey: String? = null
)

object PaintToolMarker {
    private const val KEY_NAME = "paint_tool_id"

    fun key(plugin: JavaPlugin): NamespacedKey = NamespacedKey(plugin, KEY_NAME)

    fun isPaintTool(plugin: JavaPlugin, item: ItemStack?): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.has(key(plugin), PersistentDataType.STRING)
    }
}

object PaintToolCatalog {
    val tools: List<PaintToolDefinition> = listOf(
        PaintToolDefinition("brush_black", 0, Material.BLACK_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_BLACK.key),
        PaintToolDefinition("brush_gray", 1, Material.GRAY_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_GRAY.key),
        PaintToolDefinition("brush_light_gray", 2, Material.LIGHT_GRAY_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_LIGHT_GRAY.key),
        PaintToolDefinition("brush_white", 3, Material.WHITE_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.SNOW.key),
        PaintToolDefinition("tool_eraser", 7, Material.RESIN_BRICK, PaintToolMode.ERASER),
        PaintToolDefinition("tool_custom_brush", 8, Material.BRUSH, PaintToolMode.CUSTOM_BRUSH),
        PaintToolDefinition("brush_brown", 9, Material.BROWN_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_BROWN.key),
        PaintToolDefinition("brush_green", 10, Material.GREEN_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_GREEN.key),
        PaintToolDefinition("brush_lime", 11, Material.LIME_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_LIGHT_GREEN.key),
        PaintToolDefinition("tool_shape", 17, Material.SLIME_BALL, PaintToolMode.SHAPE),
        PaintToolDefinition("brush_purple", 18, Material.PURPLE_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_PURPLE.key),
        PaintToolDefinition("brush_magenta", 19, Material.MAGENTA_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_MAGENTA.key),
        PaintToolDefinition("brush_pink", 20, Material.PINK_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_PINK.key),
        PaintToolDefinition("tool_easel", 25, Material.PAPER, PaintToolMode.EASEL),
        PaintToolDefinition("tool_fill", 26, Material.POWDER_SNOW_BUCKET, PaintToolMode.FILL),
        PaintToolDefinition("brush_red", 27, Material.RED_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_RED.key),
        PaintToolDefinition("brush_orange", 28, Material.ORANGE_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_ORANGE.key),
        PaintToolDefinition("brush_yellow", 29, Material.YELLOW_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_YELLOW.key),
        PaintToolDefinition("brush_blue", 30, Material.BLUE_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_BLUE.key),
        PaintToolDefinition("brush_cyan", 31, Material.CYAN_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_CYAN.key),
        PaintToolDefinition("brush_light_blue", 32, Material.LIGHT_BLUE_DYE, PaintToolMode.BASIC_COLOR_BRUSH, PaintPalette.COLOR_LIGHT_BLUE.key),
        PaintToolDefinition("tool_shears", 35, Material.SHEARS, PaintToolMode.SHEARS)
    )

    private val byId = tools.associateBy { it.id }

    fun buildLayout(
        toolKey: NamespacedKey,
        parser: MiniMessageParser,
        session: PaintSession
    ): Array<ItemStack?> {
        val contents = arrayOfNulls<ItemStack>(36)
        tools.forEach { tool ->
            contents[tool.slot] = buildItem(tool, toolKey, parser, session)
        }
        return contents
    }

    fun resolve(item: ItemStack?, toolKey: NamespacedKey): PaintToolDefinition? {
        val meta = item?.itemMeta ?: return null
        val toolId = meta.persistentDataContainer.get(toolKey, PersistentDataType.STRING) ?: return null
        return byId[toolId]
    }

    fun buildItem(
        tool: PaintToolDefinition,
        toolKey: NamespacedKey,
        parser: MiniMessageParser,
        session: PaintSession
    ): ItemStack {
        val item = ItemStack(tool.material)
        item.editMeta { meta ->
            meta.displayName(parser.parse(displayName(tool, session)))
            meta.lore(lore(tool, session).map(parser::parse))
            meta.persistentDataContainer.set(toolKey, PersistentDataType.STRING, tool.id)
        }
        return item
    }

    private fun displayName(tool: PaintToolDefinition, session: PaintSession): String {
        return when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val entry = PaintPalette.entry(requireNotNull(tool.fixedPaletteKey))
                "<!i><#C7A300>✎<#FFD700> Кисточка: <${entry.hexColor}>${basicBrushTitle(entry.key)}"
            }

            PaintToolMode.CUSTOM_BRUSH -> {
                val entry = PaintPalette.entry(session.toolSettings.customBrush.paletteKey)
                "<!i><#C7A300>✎<#FFD700> Кисточка: <${entry.hexColor}>${entry.displayName}"
            }

            PaintToolMode.ERASER ->
                "<!i><#C7A300>⚡<#FFD700> Ластик"

            PaintToolMode.SHEARS ->
                "<!i><#C7A300>✂<#FFD700> Ножницы"

            PaintToolMode.FILL -> {
                val entry = PaintPalette.entry(session.toolSettings.fill.paletteKey)
                "<!i><#C7A300>🌧<#FFD700> Заливка: <${entry.hexColor}>${entry.displayName}"
            }

            PaintToolMode.SHAPE -> {
                val entry = PaintPalette.entry(session.toolSettings.shape.paletteKey)
                "<!i><#C7A300>⭐<#FFD700> Фигура: <#FFF3E0>${session.toolSettings.shape.shapeType.displayName} <#C7A300>[<${entry.hexColor}>${entry.displayName}<#C7A300>]"
            }

            PaintToolMode.EASEL ->
                "<!i><#C7A300>🛡<#FFD700> Параметры мальберта"
        }
    }

    private fun lore(tool: PaintToolDefinition, session: PaintSession): List<String> {
        return when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val settings = session.toolSettings.basicBrush(requireNotNull(tool.fixedPaletteKey))
                buildBrushLore(settings, "Обычная кисть для рисования")
            }
            PaintToolMode.CUSTOM_BRUSH -> buildBrushLore(session.toolSettings.customBrush, "Дополнительные цвета")
            PaintToolMode.ERASER -> buildBinaryBrushLore(session.toolSettings.eraser, "Стирает нарисованное")
            PaintToolMode.SHEARS -> buildBinaryBrushLore(session.toolSettings.shears, "Вырезают пиксели")
            PaintToolMode.FILL -> buildFillLore(session.toolSettings.fill)
            PaintToolMode.SHAPE -> buildShapeLore(session.toolSettings.shape)
            PaintToolMode.EASEL -> listOf(
                "<!i><#C7A300>➥ <#FFE68A>Размер и очистка",
                "",
                "<!i><#FFD700>Управление:",
                "<!i><#C7A300> ● <#FFF3E0>Q, чтобы настроить",
                "",
                "<!i><#FFD700>Параметры:",
                "<!i><#C7A300> ● <#FFE68A>Размер: <#FFF3E0>${logicalCanvasLabel(session)}",
                "<!i><#C7A300> ● <#FFE68A>Увеличение: <#FFF3E0>${zoomLabel(session)}",
                ""
            )
        }
    }

    private fun logicalCanvasLabel(session: PaintSession): String {
        val bounds = session.logicalBounds() ?: return session.initialSize.normalized()
        return "${bounds.width}x${bounds.height}"
    }

    private fun zoomLabel(session: PaintSession): String {
        return when (session.selectedZoom) {
            1 -> "Нет"
            else -> "В ${session.selectedZoom} раза"
        }
    }

    private fun buildBrushLore(settings: PaintBrushSettings, description: String): List<String> {
        return listOf(
            "<!i><#C7A300>➥ <#FFE68A>$description",
            "",
            "<!i><#FFD700>Управление:",
            "<!i><#C7A300> ● <#FFF3E0>ПКМ, чтобы использовать ",
            "<!i><#C7A300> ● <#FFF3E0>Q, чтобы настроить",
            "<!i><#C7A300> ● <#FFF3E0>Ctrl+Q, чтобы отменить",
            "",
            "<!i><#FFD700>Параметры:",
            "<!i><#C7A300> ● <#FFE68A>Размер: <#FFF3E0>${settings.normalizedSize()}",
            "<!i><#C7A300> ● <#FFE68A>Заполнение: <#FFF3E0>${settings.normalizedFillPercent()}%",
            "<!i><#C7A300> ● <#FFE68A>Оттенок: <#FFF3E0>${settings.shade.displayName}",
            "<!i><#C7A300> ● <#FFE68A>Смешение: <#FFF3E0>${mixDescription(settings.normalizedShadeMix())}",
            ""
        )
    }

    private fun buildBinaryBrushLore(settings: PaintBinaryBrushSettings, description: String): List<String> {
        return listOf(
            "<!i><#C7A300>➥ <#FFE68A>$description",
            "",
            "<!i><#FFD700>Управление:",
            "<!i><#C7A300> ● <#FFF3E0>ПКМ, чтобы использовать ",
            "<!i><#C7A300> ● <#FFF3E0>Q, чтобы настроить",
            "<!i><#C7A300> ● <#FFF3E0>Ctrl+Q, чтобы отменить",
            "",
            "<!i><#FFD700>Параметры:",
            "<!i><#C7A300> ● <#FFE68A>Размер: <#FFF3E0>${settings.normalizedSize()}",
            "<!i><#C7A300> ● <#FFE68A>Заполнение: <#FFF3E0>${settings.normalizedFillPercent()}%",
            ""
        )
    }

    private fun buildFillLore(settings: PaintFillSettings): List<String> {
        return listOf(
            "<!i><#C7A300>➥ <#FFE68A>Окрашивает область",
            "",
            "<!i><#FFD700>Управление:",
            "<!i><#C7A300> ● <#FFF3E0>ПКМ, чтобы использовать ",
            "<!i><#C7A300> ● <#FFF3E0>Q, чтобы настроить",
            "<!i><#C7A300> ● <#FFF3E0>Ctrl+Q, чтобы отменить",
            "",
            "<!i><#FFD700>Параметры:",
            "<!i><#C7A300> ● <#FFE68A>Заполнение: <#FFF3E0>${settings.normalizedFillPercent()}%",
            "<!i><#C7A300> ● <#FFE68A>Оттенок: <#FFF3E0>${settings.baseShade.displayName}",
            "<!i><#C7A300> ● <#FFE68A>Смешение: <#FFF3E0>${mixDescription(settings.normalizedShadeMix())}",
            "<!i><#C7A300> ● <#FFE68A>Вне оттенков: <#FFF3E0>${if (settings.ignoreShade) "Да" else "Нет"}",
            ""
        )
    }

    private fun buildShapeLore(settings: PaintShapeSettings): List<String> {
        return listOf(
            "<!i><#C7A300>➥ <#FFE68A>Рисует фигурами",
            "",
            "<!i><#FFD700>Управление:",
            "<!i><#C7A300> ● <#FFF3E0>ПКМ, чтобы использовать ",
            "<!i><#C7A300> ● <#FFF3E0>Q, чтобы настроить",
            "<!i><#C7A300> ● <#FFF3E0>Ctrl+Q, чтобы отменить",
            "",
            "<!i><#FFD700>Параметры:",
            "<!i><#C7A300> ● <#FFE68A>Размер: <#FFF3E0>${settings.normalizedSize()}",
            "<!i><#C7A300> ● <#FFE68A>Заполнение: <#FFF3E0>${settings.normalizedFillPercent()}%",
            "<!i><#C7A300> ● <#FFE68A>Оттенок: <#FFF3E0>${settings.shade.displayName}",
            "<!i><#C7A300> ● <#FFE68A>Смешение: <#FFF3E0>${mixDescription(settings.normalizedShadeMix())}",
            ""
        )
    }

    private fun mixDescription(shades: Set<PaintShade>): String {
        return if (shades.isEmpty()) {
            "Нет"
        } else {
            shades.sortedBy { it.mixNumber }.joinToString(", ") { it.mixNumber.toString() }
        }
    }

    private fun basicBrushTitle(paletteKey: String): String = when (paletteKey) {
        PaintPalette.COLOR_BLACK.key -> "Чёрная"
        PaintPalette.COLOR_GRAY.key -> "Тёмно-серая"
        PaintPalette.COLOR_LIGHT_GRAY.key -> "Серая"
        PaintPalette.SNOW.key -> "Белая"
        PaintPalette.COLOR_BROWN.key -> "Коричневая"
        PaintPalette.COLOR_GREEN.key -> "Тёмно-зелёная"
        PaintPalette.COLOR_LIGHT_GREEN.key -> "Салатовая"
        PaintPalette.COLOR_PURPLE.key -> "Фиолетовая"
        PaintPalette.COLOR_MAGENTA.key -> "Малиновая"
        PaintPalette.COLOR_PINK.key -> "Розовая"
        PaintPalette.COLOR_RED.key -> "Красная"
        PaintPalette.COLOR_ORANGE.key -> "Оранжевая"
        PaintPalette.COLOR_YELLOW.key -> "Жёлтая"
        PaintPalette.COLOR_BLUE.key -> "Тёмно-синяя"
        PaintPalette.COLOR_CYAN.key -> "Бирюзовая"
        PaintPalette.COLOR_LIGHT_BLUE.key -> "Голубая"
        else -> PaintPalette.entry(paletteKey).displayName
    }
}
