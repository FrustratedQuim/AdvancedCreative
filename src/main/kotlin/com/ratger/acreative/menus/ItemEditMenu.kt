package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class ItemEditMenu(
    private val hooker: FunctionHooker,
    private val sessionManager: ItemEditSessionManager,
    private val buttonFactory: MenuButtonFactory,
    private val parser: MiniMessageParser
) {
    private val rootBlackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44)
    private val advancedBlackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44, 45, 53)
    private val editableSlot = 13

    fun openRoot(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val simpleModeSlot = 30
        val advancedModeSlot = 32
        val menu = Menu.newBuilder(hooker.plugin)
            .title(parser.parse("<!i>▍ Редактор предмета"))
            .size(menuSize)
            .rows(MenuRows.FIVE)
            .postClickRefresh(true)
            .clickListener(editorClickListener(menuSize, setOf(simpleModeSlot, advancedModeSlot)))
            .dragListener(editorDragListener(menuSize))
            .openListener { session.isInternalTransition = false }
            .closeListener(editorCloseListener(session))
            .build()

        fillBase(menu, menuSize, rootBlackSlots)
        menu.setButton(editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(simpleModeSlot, buttonFactory.simpleModeButton { transition(session) { openSimple(player, session) } })
        menu.setButton(advancedModeSlot, buttonFactory.advancedModeButton { transition(session) { openAdvancedPageOne(player, session) } })
        menu.open(player)
    }

    fun openSimple(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val menu = Menu.newBuilder(hooker.plugin)
            .title(parser.parse("<!i>▍ Простой редактор"))
            .size(menuSize)
            .rows(MenuRows.FIVE)
            .postClickRefresh(true)
            .clickListener(editorClickListener(menuSize, setOf(18)))
            .dragListener(editorDragListener(menuSize))
            .openListener { session.isInternalTransition = false }
            .closeListener(editorCloseListener(session))
            .build()

        fillBase(menu, menuSize, rootBlackSlots)
        menu.setButton(editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { transition(session) { openRoot(player, session) } })
        menu.setButton(29, buttonFactory.actionButton(Material.SNOWBALL, "<!i><#C7A300>☄ <#FFD700>Сделать предмет кидающимся", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")))
        menu.setButton(30, buttonFactory.actionButton(Material.APPLE, "<!i><#C7A300>🍖 <#FFD700>Сделать предмет съедобным", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"), buttonFactory.zeroFoodPreview()))
        menu.setButton(31, buttonFactory.actionButton(Material.IRON_HELMET, "<!i><#C7A300>🔔 <#FFD700>Позволить надевать на голову", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"), buttonFactory.hideAttributes()))
        menu.setButton(32, buttonFactory.actionButton(Material.NAME_TAG, "<!i><#C7A300>✎ <#FFD700>Изменить название и описание", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(33, buttonFactory.actionButton(Material.LAPIS_LAZULI, "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.open(player)
    }

    fun openAdvancedPageOne(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = Menu.newBuilder(hooker.plugin)
            .title(parser.parse("<!i>▍ Продвинутый редактор [1/2]"))
            .size(menuSize)
            .rows(MenuRows.SIX)
            .postClickRefresh(true)
            .clickListener(editorClickListener(menuSize, setOf(18, 27, 26, 35)))
            .dragListener(editorDragListener(menuSize))
            .openListener { session.isInternalTransition = false }
            .closeListener(editorCloseListener(session))
            .build()

        fillBase(menu, menuSize, advancedBlackSlots)
        menu.setButton(editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { transition(session) { openRoot(player, session) } })
        menu.setButton(27, buttonFactory.backButton { transition(session) { openRoot(player, session) } })
        menu.setButton(26, buttonFactory.forwardButton { transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(35, buttonFactory.forwardButton { transition(session) { openAdvancedPageTwo(player, session) } })

        val itemId = session.editableItem.type.key.key
        val amount = session.editableItem.amount
        menu.setButton(29, buttonFactory.specialParameterButton(session.editableItem, player))
        menu.setButton(30, buttonFactory.actionButton(Material.NAME_TAG, "<!i><#C7A300>✎ <#FFD700>Изменить название и описание", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(31, buttonFactory.actionButton(session.editableItem.type, "<!i><#C7A300>◎ <#FFD700>ID предмета: <#00FF40>$itemId", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300>● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id",
            "<!i><#C7A300>● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки",
            ""
        ), buttonFactory.hideEverythingExceptTooltip()))
        menu.setButton(32, buttonFactory.actionButton(Material.STRUCTURE_VOID, "<!i><#C7A300>⭘ <#FFD700>Модель: <#FF1500>Обычная", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300>● <#FFE68A>Задаёт <#FFF3E0>внешний <#FFE68A>вид предмета.",
            "<!i><#C7A300>● <#FFE68A>Не влияет на его <#FFF3E0>поведение.",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300>● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id",
            "<!i><#C7A300>● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки",
            ""
        ), buttonFactory.hideEverythingExceptTooltip()))
        menu.setButton(33, buttonFactory.actionButton(Material.BUNDLE, "<!i><#C7A300>◎ <#FFD700>Количество: <#00FF40>$amount", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300>● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300>● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            ""
        ), buttonFactory.hideAdditionalTooltip()))
        menu.setButton(38, buttonFactory.actionButton(Material.BRICK, "<!i><#C7A300>⭘ <#FFD700>Размер стака: <#FF1500>Обычный", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300>● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300>● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум",
            ""
        )))
        menu.setButton(39, buttonFactory.actionButton(Material.PAINTING, "<!i><#C7A300>① <#FFD700>Тултип: <#FF1500>Обычный", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#00FF40>» Обычный",
            "<!i><#C7A300>» Сломанный",
            ""
        )))
        menu.setButton(40, buttonFactory.actionButton(Material.NETHERITE_INGOT, "<!i><#C7A300>⭘ <#FFD700>Неразрушимость: <#FF1500>Выкл", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")))
        menu.setButton(41, buttonFactory.actionButton(Material.ELYTRA, "<!i><#C7A300>⭘ <#FFD700>Парение: <#FF1500>Выкл", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300>● <#FFE68A>Позволяет <#FFF3E0>парить, <#FFE68A>как на элитрах.",
            ""
        )))
        menu.setButton(42, buttonFactory.actionButton(Material.BRUSH, "<!i><#C7A300>✂ <#FFD700>Скрытие информации", listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
            "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
            "",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>] <#00FF40>» Скрыть зачарования",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть атрибуты",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>] <#C7A300>» Скрыть неразрушимость",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть разное",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть цвет брони",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>] <#C7A300>» Скрыть ограничения ломания",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть ограничения установки",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть отделку брони",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть музыку",
            "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>] <#C7A300>» Скрыть всё",
            "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>] <#C7A300>» Скрыть само отображение",
            ""
        )))
        menu.open(player)
    }

    fun openAdvancedPageTwo(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = Menu.newBuilder(hooker.plugin)
            .title(parser.parse("<!i>▍ Продвинутый редактор [2/2]"))
            .size(menuSize)
            .rows(MenuRows.SIX)
            .postClickRefresh(true)
            .clickListener(editorClickListener(menuSize, setOf(18, 27)))
            .dragListener(editorDragListener(menuSize))
            .openListener { session.isInternalTransition = false }
            .closeListener(editorCloseListener(session))
            .build()

        fillBase(menu, menuSize, advancedBlackSlots)
        menu.setButton(editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(27, buttonFactory.backButton { transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(29, buttonFactory.actionButton(Material.IRON_CHESTPLATE, "<!i><#C7A300>🛡 <#FFD700>Параметры экипировки", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.hideAttributes()))
        menu.setButton(30, buttonFactory.actionButton(Material.IRON_PICKAXE, "<!i><#C7A300>⛏ <#FFD700>Параметры инструмента", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.hideAttributes()))
        menu.setButton(31, buttonFactory.actionButton(Material.LAPIS_LAZULI, "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(32, buttonFactory.actionButton(Material.PRISMARINE_CRYSTALS, "<!i><#C7A300>⭘ <#FFD700>Атрибуты: <#FF1500>Нет", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")))
        menu.setButton(33, buttonFactory.actionButton(MenuButtonFactory.ADVANCED_RESTRICTIONS_ICON_MATERIAL, "<!i><#C7A300>🔥 <#FFD700>Ограничения", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300>● <#FFE68A>Ограничивает действия в <#FFF3E0>/gm 2",
            ""
        )))
        menu.setButton(38, buttonFactory.actionButton(Material.APPLE, "<!i><#C7A300>🍖 <#FFD700>Съедобность", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.zeroFoodPreview()))
        menu.setButton(39, buttonFactory.actionButton(Material.TOTEM_OF_UNDYING, "<!i><#C7A300>☠ <#FFD700>Защита от смерти", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(40, buttonFactory.actionButton(Material.CLOCK, "<!i><#C7A300>⌚ <#FFD700>Задержка использования", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(41, buttonFactory.actionButton(Material.RESIN_CLUMP, "<!i><#C7A300>⚡ <#FFD700>После использования", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(42, buttonFactory.actionButton(Material.DIAMOND, "<!i><#C7A300>① <#FFD700>Редкость: <gray>Обычное", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#00FF40>» Обычное",
            "<!i><#C7A300>» Необычное",
            "<!i><#C7A300>» Редкое",
            "<!i><#C7A300>» Эпическое",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300>● <#FFE68A>Влияет на цвет",
            "<!i><#C7A300>● <#FFF3E0>обычного <#FFE68A>названия.",
            ""
        )))
        menu.open(player)
    }

    private fun transition(session: ItemEditSession, action: () -> Unit) {
        session.isInternalTransition = true
        action()
    }

    private fun fillBase(menu: Menu, menuSize: Int, blackSlots: Set<Int>) {
        val blackFiller = buttonFactory.blackFillerButton()
        val grayFiller = buttonFactory.grayFillerButton()
        for (slot in 0 until menuSize) {
            menu.setButton(slot, if (slot in blackSlots) blackFiller else grayFiller)
        }
    }

    private fun editorClickListener(menuSize: Int, interactiveTopSlots: Set<Int>) = { event: ru.violence.coreapi.bukkit.api.menu.event.ClickEvent ->
        if (event.rawSlot == editableSlot) {
            false
        } else if (event.rawSlot in 0 until menuSize) {
            event.rawSlot in interactiveTopSlots
        } else {
            true
        }
    }

    private fun editorDragListener(menuSize: Int) = { event: ru.violence.coreapi.bukkit.api.menu.event.DragEvent ->
        event.rawSlots.none { it in 0 until menuSize }
    }

    private fun editorCloseListener(session: ItemEditSession) = { event: ru.violence.coreapi.bukkit.api.menu.event.CloseEvent ->
        if (event.player.uniqueId == session.playerId && !session.isInternalTransition) {
            sessionManager.updateEditableItem(event.player, session.editableItem)
            val closedSession = sessionManager.closeSession(event.player)
            if (closedSession != null) {
                hooker.menuService.syncEditedItemBack(event.player, closedSession)
            }
        }
    }
}
