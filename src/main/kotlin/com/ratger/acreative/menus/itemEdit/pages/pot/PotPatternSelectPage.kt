package com.ratger.acreative.menus.itemEdit.pages.pot

import com.ratger.acreative.itemedit.trim.TrimPotSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class PotPatternSelectPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)

    fun open(
        player: Player,
        session: ItemEditSession,
        part: DecoratedPotPartDescriptor,
        openBack: (Player, ItemEditSession) -> Unit
    ) {
        val menu = support.buildMenu(
            title = part.patternTitle,
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 40) + PotterySherdCatalog.workSlots,
            session = session
        )

        support.fillBase(menu, 45, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        PotterySherdCatalog.orderedSherds.forEachIndexed { index, sherd ->
            val slot = PotterySherdCatalog.workSlots[index]
            val selected = TrimPotSupport.isSherdSelected(session.editableItem, part.side, sherd.material)

            menu.setButton(
                slot,
                buildPotterySherdButton(
                    material = sherd.material,
                    sherdDisplayName = sherd.displayName,
                    selected = selected,
                    action = {
                        if (!TrimPotSupport.toggleSideSherd(session.editableItem, part.side, sherd.material)) {
                            return@buildPotterySherdButton
                        }
                        support.transition(session) {
                            openBack(player, session)
                        }
                    }
                )
            )
        }

        val isBrickSelected = TrimPotSupport.isBrickSelected(session.editableItem, part.side)
        menu.setButton(40, buttonFactory.actionButton(
            material = Material.BRICK,
            name = if (isBrickSelected) {
                "<!i><#C7A300>◎ <#FFD700>Кирпич"
            } else {
                "<!i><#C7A300>⭘ <#FFD700>Кирпич"
            },
            lore = if (isBrickSelected) {
                listOf(
                    "<!i><#FFF3E0>Выберите нужный черепок",
                    "",
                    "<!i><#00FF40>▍ Выбрано"
                )
            } else {
                listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы выбрать")
            },
            itemModifier = {
                if (isBrickSelected) {
                    glint(true)
                }
                this
            },
            action = {
                if (!TrimPotSupport.applySide(session.editableItem, part.side, null)) {
                    return@actionButton
                }
                support.transition(session) {
                    openBack(player, session)
                }
            }
        ))

        menu.open(player)
    }

    private fun buildPotterySherdButton(
        material: Material,
        sherdDisplayName: String,
        selected: Boolean,
        action: (ClickEvent) -> Unit
    ) = buttonFactory.actionButton(
        material = material,
        name = "<!i><#C7A300>${if (selected) "◎" else "⭘"} <#FFD700>Глиняный черепок «$sherdDisplayName»",
        lore = if (selected) {
            listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы снять",
                "",
                "<!i><#00FF40>▍ Выбрано"
            )
        } else {
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы выбрать")
        },
        itemModifier = {
            if (selected) {
                glint(true)
            }
            this
        },
        action = action
    )
}
