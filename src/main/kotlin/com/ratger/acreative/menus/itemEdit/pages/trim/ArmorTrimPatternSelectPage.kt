package com.ratger.acreative.menus.itemEdit.pages.trim

import com.ratger.acreative.itemedit.trim.ArmorTrimCatalog
import com.ratger.acreative.itemedit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class ArmorTrimPatternSelectPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)

    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Отделка брони → Тип",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18) + ArmorTrimCatalog.orderedPatterns.map { it.slot },
            session = session
        )

        support.fillBase(menu, 45, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        val currentPattern = ArmorTrimSupport.currentPattern(session.editableItem)
        ArmorTrimCatalog.orderedPatterns.forEach { descriptor ->
            menu.setButton(
                descriptor.slot,
                buttonFactory.armorTrimPatternOptionButton(
                    icon = descriptor.icon,
                    displayName = descriptor.displayName,
                    selected = currentPattern == descriptor.pattern
                ) {
                    if (!ArmorTrimSupport.togglePattern(session.editableItem, descriptor.pattern)) {
                        return@armorTrimPatternOptionButton
                    }
                    support.transition(session) {
                        openBack(player, session)
                    }
                }
            )
        }

        menu.open(player)
    }
}
