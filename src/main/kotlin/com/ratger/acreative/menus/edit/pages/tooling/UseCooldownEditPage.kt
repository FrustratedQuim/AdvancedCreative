package com.ratger.acreative.menus.edit.pages.tooling

import com.ratger.acreative.menus.edit.usecooldown.UseCooldownSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.pages.common.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class UseCooldownEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Задержка",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(30, buildSecondsButton(player, session))
        menu.setButton(32, buildGroupButton(player, session))
    }

    private fun buildSecondsButton(player: Player, session: ItemEditSession): Button {
        val active = UseCooldownSupport.has(session.editableItem)
        val secondsValue = UseCooldownSupport.seconds(session.editableItem) ?: 0f
        val activeLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Задержка в <#FFF3E0>секундах<#FFE68A> перед ",
            "<!i><#C7A300> ● <#FFE68A>следующим использованием. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число><#FFE68A> <#C7A300>-<#FFE68A> задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>-<#FFE68A> отмена ",
            ""
        )
        val inactiveLore = activeLore.toMutableList().also {
            it[it.lastIndex - 1] = "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>-<#FFE68A> отменить "
        }
        return buttonFactory.applyResetButton(
            material = Material.CLOCK,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Задержка: <#00FF40>${UseCooldownSupport.displaySeconds(secondsValue)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Задержка: <#FF1500>Обычная",
            activeLore = activeLore,
            inactiveLore = inactiveLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.USE_COOLDOWN_SECONDS) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = { event ->
                UseCooldownSupport.clear(session.editableItem)
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun buildGroupButton(player: Player, session: ItemEditSession): Button {
        val active = UseCooldownSupport.hasGroup(session.editableItem)
        val commonLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Задержка затрагивает ",
            "<!i><#C7A300> ● <#FFE68A>предметы с <#FFF3E0>одинаковым<#FFE68A> ID. ",
            "<!i><#C7A300> ● <#FFF3E0>Группы<#FFE68A> разделяют задержки ",
            "<!i><#C7A300> ● <#FFE68A>между одинаковыми ID. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <название><#FFE68A> <#C7A300>-<#FFE68A> задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply rand<#FFE68A> <#C7A300>-<#FFE68A> случайная ",
            ""
        )
        return buttonFactory.applyResetButton(
            material = Material.WRITABLE_BOOK,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Группа: <#00FF40>${UseCooldownSupport.displayGroup(UseCooldownSupport.group(session.editableItem))}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Группа: <#FF1500>Отсутствует",
            activeLore = commonLore,
            inactiveLore = commonLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.USE_COOLDOWN_GROUP) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = { event ->
                UseCooldownSupport.clearGroup(session.editableItem)
                refreshButtons(event.menu, player, session)
            }
        )
    }
}
