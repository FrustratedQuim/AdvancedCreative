package com.ratger.acreative.menus.edit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.decorationheads.support.TemporaryMenuButtonOverrideSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
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
    private val temporaryOverrideSupport = TemporaryMenuButtonOverrideSupport(hooker.tickScheduler)

    val rootBlackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44)
    val advancedBlackSlots = setOf(0, 8, 9, 12, 14, 17, 18, 26, 27, 35, 36, 44, 45, 53)
    val editableSlot = 13

    fun buildMenu(
        title: String,
        menuSize: Int,
        rows: MenuRows,
        interactiveTopSlots: Set<Int>,
        session: ItemEditSession,
        allowPlayerInventoryClicks: Boolean = false
    ): Menu = Menu.newBuilder(hooker.plugin)
        .title(parser.parse(title))
        .rows(rows)
        .postClickRefresh(false)
        .clickListener(editorClickListener(menuSize, interactiveTopSlots, allowPlayerInventoryClicks))
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


    fun replaceSlotTemporarily(
        menu: Menu,
        slot: Int,
        temporaryButton: ru.violence.coreapi.bukkit.api.menu.button.Button,
        restoreAfterTicks: Long,
        restoreButton: () -> ru.violence.coreapi.bukkit.api.menu.button.Button
    ) {
        temporaryOverrideSupport.replaceSlotTemporarily(menu, slot, temporaryButton, restoreAfterTicks, restoreButton)
    }

    fun transition(session: ItemEditSession, action: () -> Unit) {
        session.isInternalTransition = true
        action()
    }

    private fun editorClickListener(
        menuSize: Int,
        interactiveTopSlots: Set<Int>,
        allowPlayerInventoryClicks: Boolean
    ) = { event: ClickEvent ->
        val rawSlot = event.rawSlot
        if (rawSlot in 0 until menuSize) {
            rawSlot in interactiveTopSlots
        } else {
            allowPlayerInventoryClicks
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
