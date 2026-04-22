package com.ratger.acreative.menus.banner.editor

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.decorationheads.support.TemporaryMenuButtonOverrideSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.CloseEvent

class BannerEditorMenuSupport(
    private val hooker: FunctionHooker,
    private val sessionManager: BannerEditorSessionManager,
    private val buttonFactory: MenuButtonFactory,
    private val parser: MiniMessageParser,
    private val syncEditedBannerBack: (org.bukkit.entity.Player, BannerEditorSession) -> Unit
) {
    private val temporaryOverrideSupport = TemporaryMenuButtonOverrideSupport(hooker.tickScheduler)

    fun buildMenu(
        title: String,
        menuSize: Int,
        rows: MenuRows,
        interactiveTopSlots: Set<Int>,
        session: BannerEditorSession,
        allowPlayerInventoryClicks: Boolean = true,
        blockShiftClickFromPlayerInventory: Boolean = true
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

    fun transition(session: BannerEditorSession, action: () -> Unit) {
        session.isInternalTransition = true
        action()
    }

    private fun editorCloseListener(session: BannerEditorSession) = { event: CloseEvent ->
        if (event.player.uniqueId == session.playerId && !session.isInternalTransition) {
            val closedSession = sessionManager.closeSession(event.player)
            if (closedSession != null) {
                syncEditedBannerBack(event.player, closedSession)
            }
        }
    }
}
