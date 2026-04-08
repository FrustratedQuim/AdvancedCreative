package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.usecooldown.UseCooldownSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class UseCooldownEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Задержка",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(30, buildSecondsButton(player, session))
        menu.setButton(32, buildGroupButton(player, session))
    }

    private fun buildSecondsButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.CLOCK,
        name = UseCooldownSupport.seconds(session.editableItem)?.let {
            "<!i><#C7A300>◎ <#FFD700>Задержка: <#00FF40>${UseCooldownSupport.displaySeconds(it)}"
        } ?: "<!i><#C7A300>⭘ <#FFD700>Задержка: <#FF1500>Обычная",
        lore = if (UseCooldownSupport.has(session.editableItem)) {
            listOf(
                "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Задержка в <#FFF3E0>секундах<#FFE68A> перед",
                "<!i><#C7A300> ● <#FFE68A>следующим использованием. ",
                "",
                "<!i><#FFD700>После нажатия:",
                "<!i><#C7A300> ● <#FFF3E0>/apply <число><#FFE68A> <#C7A300>-<#FFE68A> задать",
                "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>-<#FFE68A> отмена",
                ""
            )
        } else {
            listOf(
                "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Задержка в <#FFF3E0>секундах<#FFE68A> перед",
                "<!i><#C7A300> ● <#FFE68A>следующим использованием. ",
                "",
                "<!i><#FFD700>После нажатия:",
                "<!i><#C7A300> ● <#FFF3E0>/apply <число><#FFE68A> <#C7A300>-<#FFE68A> задать",
                "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>-<#FFE68A> отменить",
                ""
            )
        },
        itemModifier = {
            if (UseCooldownSupport.has(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (event.isRight || event.isShiftRight) {
                UseCooldownSupport.clear(session.editableItem)
                refreshButtons(event.menu, player, session)
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.USE_COOLDOWN_SECONDS) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }
    )

    private fun buildGroupButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.WRITABLE_BOOK,
        name = if (UseCooldownSupport.hasGroup(session.editableItem)) {
            "<!i><#C7A300>◎ <#FFD700>Группа: <#00FF40>${UseCooldownSupport.displayGroup(UseCooldownSupport.group(session.editableItem))}"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Группа: <#FF1500>Отсутствует"
        },
        lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Задержка затрагивает",
            "<!i><#C7A300> ● <#FFE68A>предметы с <#FFF3E0>одинаковым<#FFE68A> ID.",
            "<!i><#C7A300> ● <#FFF3E0>Группы<#FFE68A> разделяют задержки ",
            "<!i><#C7A300> ● <#FFE68A>между одинаковыми ID.",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <название><#FFE68A> <#C7A300>-<#FFE68A> задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply rand<#FFE68A> <#C7A300>-<#FFE68A> случайная",
            ""
        ),
        itemModifier = {
            if (UseCooldownSupport.hasGroup(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (event.isRight || event.isShiftRight) {
                UseCooldownSupport.clearGroup(session.editableItem)
                refreshButtons(event.menu, player, session)
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.USE_COOLDOWN_GROUP) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }
    )
}
