package com.ratger.acreative.menus.itemEdit.pages.pot

import com.ratger.acreative.itemedit.trim.TrimPotSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.pages.common.ItemEditPageLayouts
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class PotEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openPatternSelect: (Player, ItemEditSession, DecoratedPotPartDescriptor, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Ваза",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18) + DecoratedPotPartDescriptor.entries.map { it.rootSlot },
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        DecoratedPotPartDescriptor.entries.forEach { descriptor ->
            val sherd = TrimPotSupport.sherd(session.editableItem, descriptor.side)
            val sherdName = sherd?.let { PotterySherdCatalog.descriptor(it)?.displayName }

            menu.setButton(
                descriptor.rootSlot,
                buttonFactory.decoratedPotPartButton(
                    partLabel = descriptor.partLabel,
                    material = sherd,
                    materialDisplayName = sherdName,
                    action = { event ->
                        support.transition(session) {
                            if ((event.isRight || event.isShiftRight) && !TrimPotSupport.isBrickSelected(session.editableItem, descriptor.side)) {
                                if (TrimPotSupport.applySide(session.editableItem, descriptor.side, null)) {
                                    open(player, session, openBack)
                                }
                                return@transition
                            }

                            if (event.isRight || event.isShiftRight) {
                                return@transition
                            }

                            openPatternSelect(player, session, descriptor) { reopenPlayer, reopenSession ->
                                open(reopenPlayer, reopenSession, openBack)
                            }
                        }
                    }
                )
            )
        }

        menu.open(player)
    }
}
