package com.ratger.acreative.menus.edit.pages.restrictions

import com.ratger.acreative.menus.edit.restrictions.ItemRestrictionSupport
import com.ratger.acreative.menus.edit.restrictions.RestrictionMode
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.pages.common.ItemEditPageLayouts
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class RestrictionsRootPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val openRestrictionsList: (Player, ItemEditSession, RestrictionMode, Int) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Ограничения",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)

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
    ): Button {
        val entries = ItemRestrictionSupport.entries(session.editableItem, mode)
        return buttonFactory.statefulSummaryButton(
            material = mode.itemMaterialFallback,
            active = entries.isNotEmpty(),
            activeName = mode.summaryFilledName(entries.size),
            inactiveName = mode.summaryEmptyName,
            selectedHeader = mode.summaryListTitle,
            selectedEntriesLore = entries.map { entry -> "<!i><#C7A300> ● <#FFE68A>${entry.displayId} " },
            action = { support.transition(session) { openRestrictionsList(player, session, mode, 0) } }
        )
    }
}
