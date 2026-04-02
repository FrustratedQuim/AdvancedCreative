package com.ratger.acreative.menus

import com.ratger.acreative.menus.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class AdvancedItemEditMenuPageOne(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRoot: (Player, ItemEditSession) -> Unit,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = support.buildMenu(
            title = "<!i>▍ Продвинутый редактор [1/2]",
            menuSize = menuSize,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 26, 35, 31, 33),
            session = session
        )

        support.fillBase(menu, menuSize, support.advancedBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(26, buttonFactory.forwardButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(35, buttonFactory.forwardButton { support.transition(session) { openAdvancedPageTwo(player, session) } })

        val itemId = session.editableItem.type.key.key
        val amount = session.editableItem.amount
        menu.setButton(29, buttonFactory.specialParameterButton(session.editableItem, player))
        menu.setButton(30, buttonFactory.actionButton(Material.NAME_TAG, "<!i><#C7A300>✎ <#FFD700>Изменить название и описание", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(31, buttonFactory.actionButton(session.editableItem.type, "<!i><#C7A300>◎ <#FFD700>ID предмета: <#00FF40>$itemId", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        ), buttonFactory.hideEverythingExceptTooltip(), action = {
            support.transition(session) {
                requestApplyInput(player, session, EditorApplyKind.ITEM_ID) { reopenPlayer, reopenSession ->
                    open(reopenPlayer, reopenSession)
                }
            }
        }))
        menu.setButton(32, buttonFactory.actionButton(Material.STRUCTURE_VOID, "<!i><#C7A300>⭘ <#FFD700>Модель: <#FF1500>Обычная", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Задаёт <#FFF3E0>внешний <#FFE68A>вид предмета. ",
            "<!i><#C7A300> ● <#FFE68A>Не влияет на его <#FFF3E0>поведение. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
            "<!i><#C7A300> ● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки ",
            ""
        ), buttonFactory.hideEverythingExceptTooltip()))
        menu.setButton(33, buttonFactory.actionButton(Material.BUNDLE, "<!i><#C7A300>◎ <#FFD700>Количество: <#00FF40>$amount", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        ), buttonFactory.hideAdditionalTooltip(), action = {
            support.transition(session) {
                requestApplyInput(player, session, EditorApplyKind.AMOUNT) { reopenPlayer, reopenSession ->
                    open(reopenPlayer, reopenSession)
                }
            }
        }))
        menu.setButton(38, buttonFactory.actionButton(Material.BRICK, "<!i><#C7A300>⭘ <#FFD700>Размер стака: <#FF1500>Обычный", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
            ""
        )))
        menu.setButton(39, buttonFactory.actionButton(Material.PAINTING, "<!i><#C7A300>① <#FFD700>Тултип: <#FF1500>Обычный", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i>  <#00FF40>» Обычный",
            "<!i><b> </b><#C7A300>» Сломанный",
            ""
        )))
        menu.setButton(40, buttonFactory.actionButton(Material.NETHERITE_INGOT, "<!i><#C7A300>⭘ <#FFD700>Неразрушимость: <#FF1500>Выкл", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")))
        menu.setButton(41, buttonFactory.actionButton(Material.ELYTRA, "<!i><#C7A300>⭘ <#FFD700>Парение: <#FF1500>Выкл", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Позволяет <#FFF3E0>парить, <#FFE68A>как на элитрах. ",
            ""
        )))
        menu.setButton(42, buttonFactory.actionButton(Material.BRUSH, "<!i><#C7A300>✂ <#FFD700>Скрытие информации", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
            "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
            "",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]  <#00FF40>» Скрыть зачарования ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть атрибуты ",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<b> </b><#C7A300>» Скрыть неразрушимость ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть разное ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть цвет брони ",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<b> </b><#C7A300>» Скрыть ограничения ломания ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть ограничения установки ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть отделку брони ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть музыку ",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<b> </b><#C7A300>» Скрыть всё ",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<b> </b><#C7A300>» Скрыть само отображение ",
            ""
        )))
        menu.open(player)
    }
}
