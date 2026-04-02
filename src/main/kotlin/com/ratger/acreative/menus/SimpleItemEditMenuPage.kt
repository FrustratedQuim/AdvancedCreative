package com.ratger.acreative.menus

import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class SimpleItemEditMenuPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRoot: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val menu = support.buildMenu(
            title = "<!i>▍ Простой редактор",
            menuSize = menuSize,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18),
            session = session
        )

        support.fillBase(menu, menuSize, support.rootBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(29, buttonFactory.actionButton(Material.SNOWBALL, "<!i><#C7A300>☄ <#FFD700>Сделать предмет кидающимся", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")))
        menu.setButton(30, buttonFactory.actionButton(Material.APPLE, "<!i><#C7A300>🍖 <#FFD700>Сделать предмет съедобным", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"), buttonFactory.zeroFoodPreview()))
        menu.setButton(31, buttonFactory.actionButton(Material.IRON_HELMET, "<!i><#C7A300>🔔 <#FFD700>Позволить надевать на голову", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"), buttonFactory.hideAttributes()))
        menu.setButton(32, buttonFactory.actionButton(Material.NAME_TAG, "<!i><#C7A300>✎ <#FFD700>Изменить название и описание", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(33, buttonFactory.actionButton(Material.LAPIS_LAZULI, "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.open(player)
    }
}
