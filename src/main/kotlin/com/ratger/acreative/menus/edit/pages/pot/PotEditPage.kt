package com.ratger.acreative.menus.edit.pages.pot

import com.ratger.acreative.itemedit.trim.TrimPotSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.pages.common.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

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
                buildDecoratedPotPartButton(
                    partLabel = descriptor.partLabel,
                    material = sherd,
                    materialDisplayName = sherdName
                ) { event ->
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
        }

        menu.open(player)
    }

    private fun buildDecoratedPotPartButton(
        partLabel: String,
        material: Material?,
        materialDisplayName: String?,
        action: (ClickEvent) -> Unit
    ) = buttonFactory.actionButton(
        material = material ?: Material.BRICK,
        name = "<!i><#C7A300>$partLabel <#FFD700>Часть: <#FFF3E0>${materialDisplayName ?: "Кирпич"}",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = action
    )
}
