package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.potion.PotionColorSupport
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class PotionEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openEffectsPage: (Player, ItemEditSession) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Зелье",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 29, 31, 33),
            session = session
        )

        support.fillBase(menu, 45, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        val customColor = PotionItemSupport.color(session.editableItem)
        val hexColor = customColor?.let(PotionColorSupport::normalizeHex)
        menu.setButton(29, buttonFactory.potionColorButton(hexColor) { event ->
            if (event.isRight || event.isShiftRight) {
                PotionItemSupport.setColor(session.editableItem, null)
                support.transition(session) { open(player, session, openBack) }
                return@potionColorButton
            }
            if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.POTION_COLOR) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            }
        })

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
                    "<!i>"
                ),
                afterOptionsLore = listOf("<!i>"),
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
                "<!i><#C7A300> ● <#FFE68A>${entry.displayName}"
            } else {
                "<!i><#C7A300> ● <#FFE68A>${entry.displayName} <#FFF3E0>${entry.displayLevel}"
            }
        }
        menu.setButton(33, buttonFactory.potionEffectsSummaryButton(entries.size, selectedLore) {
            support.transition(session) { openEffectsPage(player, session) }
        })

        menu.open(player)
    }
}
