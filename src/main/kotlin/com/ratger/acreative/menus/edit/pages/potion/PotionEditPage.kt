package com.ratger.acreative.menus.edit.pages.potion

import com.ratger.acreative.itemedit.potion.PotionColorSupport
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.pages.common.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class PotionEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openEffectsPage: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Зелье",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 29, 31, 33),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        val customColor = PotionItemSupport.color(session.editableItem)
        val hexColor = customColor?.let(PotionColorSupport::normalizeHex)
        val activeColorLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <#hex> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
        val inactiveColorLore = activeColorLore.toMutableList().also {
            it[it.lastIndex - 1] = "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отменить "
        }
        menu.setButton(29, buttonFactory.applyResetButton(
            material = Material.BRUSH,
            active = hexColor != null,
            activeName = "<!i><#C7A300>◎ <#FFD700>Цвет: <$hexColor>$hexColor",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Цвет: <#FF1500>Обычный",
            activeLore = activeColorLore,
            inactiveLore = inactiveColorLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.POTION_COLOR) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = {
                PotionItemSupport.setColor(session.editableItem, null)
                support.transition(session) { open(player, session, openBack) }
            }
        ))

        fun installFormButton(selectedIndex: Int) {
            menu.setButton(31, buttonFactory.listButton(
                material = session.editableItem.type,
                options = PotionItemSupport.forms.map { MenuButtonFactory.ListButtonOption(it, it.label) },
                selectedIndex = selectedIndex,
                titleBuilder = { selected, _ ->
                    "<!i><#C7A300>${selected.value.titlePrefix} <#FFD700>Тип: <#FFF3E0>${selected.value.label}"
                },
                beforeOptionsLore = listOf(
                    "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                    ""
                ),
                afterOptionsLore = listOf(""),
                itemModifier = {
                    buttonFactory.hideAdditionalTooltip().invoke(this)
                    this
                },
                action = { _, newIndex ->
                    val selected = PotionItemSupport.forms[newIndex]
                    session.editableItem = PotionItemSupport.setForm(session.editableItem, selected)
                    menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
                    installFormButton(newIndex)
                }
            ))
        }
        installFormButton(PotionItemSupport.currentFormIndex(session.editableItem))

        val entries = PotionItemSupport.effectEntries(session.editableItem)
        val selectedLore = entries.map { entry ->
            if (entry.displayLevel <= 1) {
                "<!i><#C7A300> ● <#FFE68A>${entry.displayName} "
            } else {
                "<!i><#C7A300> ● <#FFE68A>${entry.displayName} <#FFF3E0>${entry.displayLevel} "
            }
        }
        menu.setButton(33, buttonFactory.statefulSummaryButton(
            material = Material.BREWING_STAND,
            active = entries.isNotEmpty(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Эффекты: <#00FF40>${entries.size}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Эффекты: <#FF1500>Нет",
            selectedEntriesLore = selectedLore,
            action = {
            support.transition(session) { openEffectsPage(player, session) }
        }))

        menu.open(player)
    }
}
