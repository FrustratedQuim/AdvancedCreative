package com.ratger.acreative.menus

import com.ratger.acreative.itemedit.meta.MiniMessageParser
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.util.ItemBuilder
import org.bukkit.Material

class MenuButtonFactory(
    private val parser: MiniMessageParser
) {
    fun blackFillerButton() = Button.simple(
        ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(parser.parse("<!i>"))
            .hideTooltip(true)
            .build()
    ).build()

    fun grayFillerButton() = Button.simple(
        ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(parser.parse("<!i>"))
            .hideTooltip(true)
            .build()
    ).build()

    fun simpleModeButton(action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit) = Button.simple(
        ItemBuilder(Material.ENDER_PEARL)
            .name(parser.parse("<!i><#C7A300>⏺ <#FFD700>Простой режим"))
            .lore(listOf(parser.parse("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
            .build()
    ).action(action).build()

    fun advancedModeButton(action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit) = Button.simple(
        ItemBuilder(Material.ENDER_EYE)
            .name(parser.parse("<!i><#C7A300>⭐ <#FFD700>Продвинутый режим"))
            .lore(
                listOf(
                    parser.parse("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
                    parser.parse("<!i>"),
                    parser.parse("<!i><dark_red>▍ <#FF1500>Если разбираетесь")
                )
            )
            .build()
    ).action(action).build()
}
