package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.remainder.UseRemainderSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class UseRemainderEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Использование",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 26, 30, 32),
            session = session,
            allowPlayerInventoryClicks = true
        )

        support.fillBase(menu, 45, setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14))
        menu.setClickListener(buildClickListener(session))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(26, buildInfoButton())
        menu.setButton(32, buildResetButton(session))

        refreshDynamicButtons(menu, session)
        menu.open(player)
    }


    private fun buildClickListener(session: ItemEditSession) = { event: ru.violence.coreapi.bukkit.api.menu.event.ClickEvent ->
        if (event.rawSlot in 0 until 45) {
            if (event.rawSlot == 30 && event.isShiftLeft && moveRemainderToPreferredEmptyInventorySlot(event, session)) {
                false
            } else {
                event.rawSlot in setOf(18, 26, 30, 32)
            }
        } else {
            !(event.isShiftLeft && handleShiftLeftFromPlayerInventory(event, session))
        }
    }


    private fun moveRemainderToPreferredEmptyInventorySlot(
        event: ru.violence.coreapi.bukkit.api.menu.event.ClickEvent,
        session: ItemEditSession
    ): Boolean {
        val remainder = UseRemainderSupport.get(session.editableItem) ?: return false
        val playerInventory = event.player.inventory
        val emptySlot = findPreferredShiftTargetSlot(playerInventory) ?: return false

        playerInventory.setItem(emptySlot, remainder.clone())
        UseRemainderSupport.clear(session.editableItem)
        refreshDynamicButtons(event.menu, session)
        return true
    }


    private fun findPreferredShiftTargetSlot(inventory: PlayerInventory): Int? {
        for (slot in 8 downTo 0) {
            if (UseRemainderSupport.isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        for (slot in 35 downTo 9) {
            if (UseRemainderSupport.isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        return null
    }

    private fun handleShiftLeftFromPlayerInventory(
        event: ru.violence.coreapi.bukkit.api.menu.event.ClickEvent,
        session: ItemEditSession
    ): Boolean {
        val clickedInventory = event.clickedInventory ?: return false
        val clickedItem = event.clickedItem ?: return false
        if (UseRemainderSupport.isEmpty(clickedItem)) {
            return false
        }

        val previousRemainder = UseRemainderSupport.get(session.editableItem)
        UseRemainderSupport.set(session.editableItem, clickedItem.clone())

        if (previousRemainder == null) {
            clickedInventory.setItem(event.slot, null)
        } else {
            clickedInventory.setItem(event.slot, previousRemainder.clone())
        }

        refreshDynamicButtons(event.menu, session)
        return true
    }

    private fun refreshDynamicButtons(menu: Menu, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(30, buildUseRemainderSlotButton(session))
    }

    private fun buildInfoButton() = buttonFactory.actionButton(
        material = Material.OAK_HANGING_SIGN,
        name = "<!i><#C7A300>ℹ <#FFD700>Что это такое?",
        lore = listOf(
            "",
            "<!i><#C7A300> ◆ <#FFE68A>Вложенный в <#FFF3E0>нужный слот<#FFE68A> предмет ",
            "<!i><b>  </b> <#FFE68A>появится в инвентаре после",
            "<!i><b>  </b> <#FFF3E0>использования <#FFE68A>основного.",
            "",
            "<!i><#C7A300> ◆ <#FFE68A>Например: после <#FFF3E0>броска<#FFE68A> или после ",
            "<!i><b>  </b> <#FFF3E0>поедания.",
            ""
        )
    )

    private fun buildResetButton(session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.RED_DYE,
        name = "<!i><#FF1500>⚠ Сбросить предмет",
        lore = emptyList(),
        action = { event ->
            UseRemainderSupport.clear(session.editableItem)
            refreshDynamicButtons(event.menu, session)
        }
    )

    private fun buildUseRemainderSlotButton(session: ItemEditSession) = buttonFactory.useRemainderSlotButton(
        UseRemainderSupport.get(session.editableItem)
    ) { event ->
        val player = event.player
        val currentRemainder = UseRemainderSupport.get(session.editableItem)
        val cursorItem = player.itemOnCursor

        if (currentRemainder == null) {
            if (UseRemainderSupport.isEmpty(cursorItem)) {
                return@useRemainderSlotButton
            }

            UseRemainderSupport.set(session.editableItem, cursorItem.clone())
            player.setItemOnCursor(null)
            refreshDynamicButtons(event.menu, session)
            return@useRemainderSlotButton
        }

        if (UseRemainderSupport.isEmpty(cursorItem)) {
            player.setItemOnCursor(currentRemainder.clone())
            UseRemainderSupport.clear(session.editableItem)
            refreshDynamicButtons(event.menu, session)
            return@useRemainderSlotButton
        }

        val nextRemainder = cursorItem.clone()
        player.setItemOnCursor(currentRemainder.clone())
        UseRemainderSupport.set(session.editableItem, nextRemainder)
        refreshDynamicButtons(event.menu, session)
    }
}
