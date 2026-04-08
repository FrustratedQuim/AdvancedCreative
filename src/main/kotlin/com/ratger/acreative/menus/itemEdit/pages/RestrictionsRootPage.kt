package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.restrictions.ItemRestrictionSupport
import com.ratger.acreative.itemedit.restrictions.RestrictionMode
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class RestrictionsRootPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val openRestrictionsList: (Player, ItemEditSession, RestrictionMode, Int) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Ограничения",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, blackSlots)

        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(30, buildSummaryButton(player, session, RestrictionMode.CAN_PLACE_ON))
        menu.setButton(32, buildSummaryButton(player, session, RestrictionMode.CAN_BREAK))

        menu.open(player)
    }

    private fun buildSummaryButton(
        player: Player,
        session: ItemEditSession,
        mode: RestrictionMode
    ): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val entries = ItemRestrictionSupport.entries(session.editableItem, mode)
        if (entries.isEmpty()) {
            return buttonFactory.actionButton(
                material = mode.itemMaterialFallback,
                name = mode.summaryEmptyName,
                lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
                action = { support.transition(session) { openRestrictionsList(player, session, mode, 0) } }
            )
        }

        val lore = mutableListOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            mode.summaryListTitle
        )
        entries.forEach { entry ->
            lore += "<!i><#C7A300> ● <#FFE68A>${entry.displayId}"
        }
        lore += ""

        return buttonFactory.actionButton(
            material = mode.itemMaterialFallback,
            name = mode.summaryFilledName(entries.size),
            lore = lore,
            itemModifier = { glint(true) },
            action = { support.transition(session) { openRestrictionsList(player, session, mode, 0) } }
        )
    }
}
