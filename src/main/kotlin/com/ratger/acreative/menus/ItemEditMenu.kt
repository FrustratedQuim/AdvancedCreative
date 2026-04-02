package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class ItemEditMenu(
    private val hooker: FunctionHooker,
    private val sessionManager: ItemEditSessionManager,
    private val buttonFactory: MenuButtonFactory,
    private val parser: MiniMessageParser
) {
    private val blackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44)
    private val menuSize = 45
    private val editableSlot = 13
    private val simpleModeSlot = 30
    private val advancedModeSlot = 32

    fun open(player: Player, session: ItemEditSession) {
        val menu = Menu.newBuilder(hooker.plugin)
            .title(parser.parse("<!i>▍ Редактор предмета"))
            .size(menuSize)
            .rows(MenuRows.FIVE)
            .postClickRefresh(true)
            .clickListener { event ->
                if (event.rawSlot == editableSlot) return@clickListener false
                if (event.rawSlot in 0 until menuSize) {
                    return@clickListener event.rawSlot == simpleModeSlot || event.rawSlot == advancedModeSlot
                }
                true
            }
            .dragListener { event ->
                event.rawSlots.none { it in 0 until menuSize }
            }
            .closeListener { event ->
                if (event.player.uniqueId != session.playerId) return@closeListener
                sessionManager.updateEditableItem(event.player, session.editableItem)
                val closedSession = sessionManager.closeSession(event.player) ?: return@closeListener
                hooker.menuService.syncEditedItemBack(event.player, closedSession)
            }
            .build()

        val blackFiller = buttonFactory.blackFillerButton()
        val grayFiller = buttonFactory.grayFillerButton()

        for (slot in 0 until menuSize) {
            when {
                slot == editableSlot -> menu.setItem(slot, session.editableItem.clone())
                slot == simpleModeSlot -> menu.setButton(slot, buttonFactory.simpleModeButton { })
                slot == advancedModeSlot -> menu.setButton(slot, buttonFactory.advancedModeButton { })
                slot in blackSlots -> menu.setButton(slot, blackFiller)
                else -> menu.setButton(slot, grayFiller)
            }
        }

        menu.open(player)
    }
}
