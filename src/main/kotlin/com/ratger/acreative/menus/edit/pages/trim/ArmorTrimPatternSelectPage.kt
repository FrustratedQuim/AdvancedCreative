package com.ratger.acreative.menus.edit.pages.trim

import com.ratger.acreative.menus.edit.trim.ArmorTrimCatalog
import com.ratger.acreative.menus.edit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

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
                buildPatternOptionButton(
                    icon = descriptor.icon,
                    displayName = descriptor.displayName,
                    selected = currentPattern == descriptor.pattern
                ) {
                    if (!ArmorTrimSupport.togglePattern(session.editableItem, descriptor.pattern)) {
                        return@buildPatternOptionButton
                    }
                    support.transition(session) {
                        openBack(player, session)
                    }
                }
            )
        }

        menu.open(player)
    }

    private fun buildPatternOptionButton(
        icon: Material,
        displayName: String,
        selected: Boolean,
        action: (ClickEvent) -> Unit
    ) = buttonFactory.actionButton(
        material = icon,
        name = if (selected) {
            "<!i><#C7A300>◎ <#FFD700>Отделка «$displayName»"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Отделка «$displayName»"
        },
        lore = listOf(
            if (selected) {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы снять"
            } else {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы выбрать"
            }
        ),
        itemModifier = {
            buttonFactory.hideAdditionalTooltip().invoke(this)
            if (selected) {
                glint(true)
            }
            this
        },
        action = action
    )
}
