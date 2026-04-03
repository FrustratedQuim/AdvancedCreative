package com.ratger.acreative.menus.pages

import com.ratger.acreative.menus.ItemEditMenuSupport
import com.ratger.acreative.menus.ItemEditSession
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class AdvancedItemEditMenuPageTwo(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageOne: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = support.buildMenu(
            title = "<!i>▍ Продвинутый редактор [2/2]",
            menuSize = menuSize,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27),
            session = session
        )

        support.fillBase(menu, menuSize, support.advancedBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(29, buttonFactory.actionButton(Material.IRON_CHESTPLATE, "<!i><#C7A300>🛡 <#FFD700>Параметры экипировки", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.hideAttributes()))
        menu.setButton(30, buttonFactory.actionButton(Material.IRON_PICKAXE, "<!i><#C7A300>⛏ <#FFD700>Параметры инструмента", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.hideAttributes()))
        menu.setButton(31, buttonFactory.actionButton(Material.LAPIS_LAZULI, "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(32, buttonFactory.actionButton(Material.PRISMARINE_CRYSTALS, "<!i><#C7A300>⭘ <#FFD700>Атрибуты: <#FF1500>Нет", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")))
        menu.setButton(33, buttonFactory.actionButton(
            MenuButtonFactory.Companion.ADVANCED_RESTRICTIONS_ICON_MATERIAL, "<!i><#C7A300>🔥 <#FFD700>Ограничения", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Ограничивает действия в <#FFF3E0>/gm 2 ",
            ""
        )))
        menu.setButton(38, buttonFactory.actionButton(Material.APPLE, "<!i><#C7A300>🍖 <#FFD700>Съедобность", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.zeroFoodPreview()))
        menu.setButton(39, buttonFactory.actionButton(Material.TOTEM_OF_UNDYING, "<!i><#C7A300>☠ <#FFD700>Защита от смерти", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(40, buttonFactory.actionButton(Material.CLOCK, "<!i><#C7A300>⌚ <#FFD700>Задержка использования", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(41, buttonFactory.actionButton(Material.RESIN_CLUMP, "<!i><#C7A300>⚡ <#FFD700>После использования", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(42, buttonFactory.actionButton(Material.DIAMOND, "<!i><#C7A300>① <#FFD700>Редкость: <gray>Обычное", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i>  <#00FF40>» Обычное",
            "<!i><b> </b><#C7A300>» Необычное ",
            "<!i><b> </b><#C7A300>» Редкое ",
            "<!i><b> </b><#C7A300>» Эпическое ",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Влияет на цвет ",
            "<!i><#C7A300> ● <#FFF3E0>обычного <#FFE68A>названия. ",
            ""
        )))
        menu.open(player)
    }
}
