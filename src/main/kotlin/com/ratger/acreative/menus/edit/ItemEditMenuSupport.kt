package com.ratger.acreative.menus.edit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.decorationheads.support.TemporaryMenuButtonOverrideSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.CloseEvent

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
        allowPlayerInventoryClicks: Boolean = false,
        blockShiftClickFromPlayerInventory: Boolean = false
    ): Menu = MenuUiSupport.buildMenu(
        plugin = hooker.plugin,
        parser = parser,
        title = title,
        rows = rows,
        menuTopRange = 0 until menuSize,
        interactiveTopSlots = interactiveTopSlots,
        allowPlayerInventoryClicks = allowPlayerInventoryClicks,
        blockShiftClickFromPlayerInventory = blockShiftClickFromPlayerInventory,
        onOpen = { session.isInternalTransition = false },
        onClose = editorCloseListener(session)
    )

    fun buildStandaloneMenu(
        title: String,
        menuSize: Int,
        rows: MenuRows,
        interactiveTopSlots: Set<Int>,
        allowPlayerInventoryClicks: Boolean = true,
        blockShiftClickFromPlayerInventory: Boolean = true,
        onClose: ((CloseEvent) -> Unit)? = null
    ): Menu = MenuUiSupport.buildMenu(
        plugin = hooker.plugin,
        parser = parser,
        title = title,
        rows = rows,
        menuTopRange = 0 until menuSize,
        interactiveTopSlots = interactiveTopSlots,
        allowPlayerInventoryClicks = allowPlayerInventoryClicks,
        blockShiftClickFromPlayerInventory = blockShiftClickFromPlayerInventory,
        onClose = onClose
    )

    fun fillBase(menu: Menu, menuSize: Int, blackSlots: Set<Int>) {
        MenuUiSupport.fillByMask(
            menu = menu,
            menuSize = menuSize,
            primarySlots = blackSlots,
            primaryButton = buttonFactory.blackFillerButton(),
            secondaryButton = buttonFactory.grayFillerButton()
        )
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
