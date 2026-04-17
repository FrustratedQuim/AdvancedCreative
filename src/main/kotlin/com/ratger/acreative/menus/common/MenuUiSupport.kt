package com.ratger.acreative.menus.common

import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.menu.event.CloseEvent

object MenuUiSupport {
    fun buildMenu(
        plugin: org.bukkit.plugin.Plugin,
        parser: MiniMessageParser,
        title: String,
        rows: MenuRows,
        menuTopRange: IntRange,
        interactiveTopSlots: Set<Int>,
        allowPlayerInventoryClicks: Boolean = false,
        blockShiftClickFromPlayerInventory: Boolean = false,
        postClickRefresh: Boolean = false,
        onOpen: (() -> Unit)? = null,
        onClose: ((CloseEvent) -> Unit)? = null
    ): Menu = Menu.newBuilder(plugin)
        .title(parser.parse(title))
        .rows(rows)
        .postClickRefresh(postClickRefresh)
        .clickListener { event ->
            allowClick(
                event = event,
                menuTopRange = menuTopRange,
                interactiveTopSlots = interactiveTopSlots,
                allowPlayerInventoryClicks = allowPlayerInventoryClicks,
                blockShiftClickFromPlayerInventory = blockShiftClickFromPlayerInventory
            )
        }
        .dragListener { event -> event.rawSlots.none { it in menuTopRange } }
        .apply {
            if (onOpen != null) openListener { onOpen() }
            if (onClose != null) closeListener { onClose(it) }
        }
        .build()

    fun fillByMask(
        menu: Menu,
        menuSize: Int,
        primarySlots: Set<Int>,
        primaryButton: Button,
        secondaryButton: Button
    ) {
        for (slot in 0 until menuSize) {
            menu.setButton(slot, if (slot in primarySlots) primaryButton else secondaryButton)
        }
    }

    private fun allowClick(
        event: ClickEvent,
        menuTopRange: IntRange,
        interactiveTopSlots: Set<Int>,
        allowPlayerInventoryClicks: Boolean,
        blockShiftClickFromPlayerInventory: Boolean
    ): Boolean {
        if (
            blockShiftClickFromPlayerInventory &&
            (event.isShiftLeft || event.isShiftRight) &&
            event.rawSlot !in menuTopRange
        ) {
            return false
        }

        return if (event.rawSlot in menuTopRange) {
            event.rawSlot in interactiveTopSlots
        } else {
            allowPlayerInventoryClicks
        }
    }
}
