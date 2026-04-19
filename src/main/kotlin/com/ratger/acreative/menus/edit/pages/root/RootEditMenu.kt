package com.ratger.acreative.menus.edit.pages.root

import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class RootEditMenu(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openSimple: (Player, ItemEditSession) -> Unit,
    private val openAdvancedPageOne: (Player, ItemEditSession) -> Unit,
    private val openMyItems: (Player, ItemEditSession) -> Unit,
    private val myItemsCountProvider: (Player) -> Int
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val simpleModeSlot = 29
        val myItemsSlot = 31
        val advancedModeSlot = 33
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор предмета",
            menuSize = menuSize,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(simpleModeSlot, myItemsSlot, advancedModeSlot),
            session = session
        )

        support.fillBase(menu, menuSize, support.rootBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(simpleModeSlot, buildSimpleModeButton { support.transition(session) { openSimple(player, session) } })
        menu.setButton(myItemsSlot, buildMyItemsButton(myItemsCountProvider(player)) { support.transition(session) { openMyItems(player, session) } })
        menu.setButton(advancedModeSlot, buildAdvancedModeButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.open(player)
    }

    private fun buildSimpleModeButton(action: (ClickEvent) -> Unit) = buttonFactory.actionButton(
        material = Material.ENDER_PEARL,
        name = "<!i><#C7A300>⏺ <#FFD700>Простой режим",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
        action = action
    )

    private fun buildAdvancedModeButton(action: (ClickEvent) -> Unit) = buttonFactory.actionButton(
        material = Material.ENDER_EYE,
        name = "<!i><#C7A300>\uD83D\uDD25 <#FFD700>Продвинутый режим",
        lore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть",
            "",
            "<!i><dark_red>▍ <#FF1500>Если разбираетесь"
        ),
        action = action
    )

    private fun buildMyItemsButton(count: Int, action: (ClickEvent) -> Unit) = buttonFactory.actionButton(
        material = Material.CHEST_MINECART,
        name = "<!i><#C7A300>⭐ <#FFD700>Мои предметы <#C7A300>[<#FFF3E0>$count<#C7A300>]",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
        action = action
    )
}
