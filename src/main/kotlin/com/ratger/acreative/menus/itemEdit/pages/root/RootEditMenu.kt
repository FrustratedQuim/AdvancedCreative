package com.ratger.acreative.menus.itemEdit.pages.root

import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class RootEditMenu(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openSimple: (Player, ItemEditSession) -> Unit,
    private val openAdvancedPageOne: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val simpleModeSlot = 30
        val advancedModeSlot = 32
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор предмета",
            menuSize = menuSize,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(simpleModeSlot, advancedModeSlot),
            session = session
        )

        support.fillBase(menu, menuSize, support.rootBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(simpleModeSlot, buttonFactory.simpleModeButton { support.transition(session) { openSimple(player, session) } })
        menu.setButton(advancedModeSlot, buttonFactory.advancedModeButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.open(player)
    }
}
