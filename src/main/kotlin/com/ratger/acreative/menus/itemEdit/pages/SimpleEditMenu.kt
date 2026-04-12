package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class SimpleEditMenu(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRoot: (Player, ItemEditSession) -> Unit,
    private val openEnchantments: (Player, ItemEditSession) -> Unit,
    private val openFoodPage: (Player, ItemEditSession) -> Unit,
    private val openTextAppearance: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val menu = support.buildMenu(
            title = "<!i>▍ Простой редактор",
            menuSize = menuSize,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 33),
            session = session
        )

        support.fillBase(menu, menuSize, support.rootBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(29, buttonFactory.actionButton(Material.SNOWBALL, "<!i><#C7A300>☄ <#FFD700>Сделать предмет кидающимся", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")))
        menu.setButton(30, buttonFactory.actionButton(
            material = Material.APPLE,
            name = "<!i><#C7A300>🍖 <#FFD700>Сделать предмет съедобным",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"),
            itemModifier = buttonFactory.zeroFoodPreview(),
            action = { support.transition(session) { openFoodPage(player, session) } }
        ))
        menu.setButton(31, buttonFactory.actionButton(Material.IRON_HELMET, "<!i><#C7A300>🔔 <#FFD700>Позволить надевать на голову", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"), buttonFactory.hideAttributes()))
        menu.setButton(32, buttonFactory.actionButton(
            Material.NAME_TAG,
            "<!i><#C7A300>✎ <#FFD700>Изменить название и описание",
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openTextAppearance(player, session) } }
        ))
        menu.setButton(33, buttonFactory.actionButton(
            material = Material.LAPIS_LAZULI,
            name = "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openEnchantments(player, session) } }
        ))
        menu.open(player)
    }
}
