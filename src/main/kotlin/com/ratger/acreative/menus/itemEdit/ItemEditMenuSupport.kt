package com.ratger.acreative.menus.itemEdit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.menu.event.CloseEvent
import ru.violence.coreapi.bukkit.api.menu.event.DragEvent

class ItemEditMenuSupport(
    private val hooker: FunctionHooker,
    private val sessionManager: ItemEditSessionManager,
    private val buttonFactory: MenuButtonFactory,
    private val parser: MiniMessageParser
) {
    val rootBlackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44)
    val advancedBlackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44, 45, 53)
    val editableSlot = 13

    fun buildMenu(
        title: String,
        menuSize: Int,
        rows: MenuRows,
        interactiveTopSlots: Set<Int>,
        session: ItemEditSession
    ): Menu = Menu.newBuilder(hooker.plugin)
        .title(parser.parse(title))
        .size(menuSize)
        .rows(rows)
        .postClickRefresh(false)
        .clickListener(editorClickListener(menuSize, interactiveTopSlots))
        .dragListener(editorDragListener(menuSize))
        .openListener { session.isInternalTransition = false }
        .closeListener(editorCloseListener(session))
        .build()

    fun fillBase(menu: Menu, menuSize: Int, blackSlots: Set<Int>) {
        val blackFiller = buttonFactory.blackFillerButton()
        val grayFiller = buttonFactory.grayFillerButton()
        for (slot in 0 until menuSize) {
            menu.setButton(slot, if (slot in blackSlots) blackFiller else grayFiller)
        }
    }

    fun transition(session: ItemEditSession, action: () -> Unit) {
        session.isInternalTransition = true
        action()
    }

    private fun editorClickListener(menuSize: Int, interactiveTopSlots: Set<Int>) = { event: ClickEvent ->
        when (event.rawSlot) {
            event.rawSlot -> {
                false
            }
            in 0 until menuSize -> {
                event.rawSlot in interactiveTopSlots
            }
            else -> {
                true
            }
        }
    }

    private fun editorDragListener(menuSize: Int) = { event: DragEvent ->
        event.rawSlots.none { it in 0 until menuSize }
    }

    private fun editorCloseListener(session: ItemEditSession) = { event: CloseEvent ->
        if (event.player.uniqueId == session.playerId && !session.isInternalTransition) {
            sessionManager.updateEditableItem(event.player, session.editableItem)
            val closedSession = sessionManager.closeSession(event.player)
            if (closedSession != null) {
                hooker.menuService.syncEditedItemBack(event.player, closedSession)
            }
        }
    }
}
