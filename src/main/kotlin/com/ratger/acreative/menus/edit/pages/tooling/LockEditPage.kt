package com.ratger.acreative.menus.edit.pages.tooling

import com.ratger.acreative.itemedit.container.LockItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class LockEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageOne: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Сейф",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 26, 30, 32),
            session = session,
            allowPlayerInventoryClicks = true
        )

        support.fillBase(menu, 45, setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14))
        menu.setClickListener(buildClickListener(session))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(26, buildInfoButton())
        menu.setButton(32, buildResetButton(session))

        refreshDynamicButtons(menu, session)
        menu.open(player)
    }

    private fun buildClickListener(session: ItemEditSession) = { event: ClickEvent ->
        if (event.rawSlot in 0 until 45) {
            if (event.rawSlot == 30 && event.isShiftLeft && moveKeyToPreferredEmptyInventorySlot(event, session)) {
                false
            } else {
                event.rawSlot in setOf(18, 26, 30, 32)
            }
        } else {
            !(event.isShiftLeft && handleShiftLeftFromPlayerInventory(event, session))
        }
    }

    private fun moveKeyToPreferredEmptyInventorySlot(
        event: ClickEvent,
        session: ItemEditSession
    ): Boolean {
        val key = LockItemSupport.preview(session.editableItem) ?: return false
        val playerInventory = event.player.inventory
        val emptySlot = findPreferredShiftTargetSlot(playerInventory) ?: return false

        playerInventory.setItem(emptySlot, key.clone())
        LockItemSupport.clear(session.editableItem)
        refreshDynamicButtons(event.menu, session)
        return true
    }

    private fun findPreferredShiftTargetSlot(inventory: PlayerInventory): Int? {
        for (slot in 8 downTo 0) {
            if (LockItemSupport.isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        for (slot in 35 downTo 9) {
            if (LockItemSupport.isEmpty(inventory.getItem(slot))) {
                return slot
            }
        }
        return null
    }

    private fun handleShiftLeftFromPlayerInventory(
        event: ClickEvent,
        session: ItemEditSession
    ): Boolean {
        val clickedInventory = event.clickedInventory ?: return false
        val clickedItem = event.clickedItem ?: return false
        if (LockItemSupport.isEmpty(clickedItem)) {
            return false
        }

        val previousKey = LockItemSupport.preview(session.editableItem)
        LockItemSupport.set(session.editableItem, clickedItem.clone())

        if (previousKey == null) {
            clickedInventory.setItem(event.slot, null)
        } else {
            clickedInventory.setItem(event.slot, previousKey.clone())
        }

        refreshDynamicButtons(event.menu, session)
        return true
    }

    private fun refreshDynamicButtons(menu: Menu, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(30, buildLockSlotButton(session))
    }

    private fun buildInfoButton() = buttonFactory.actionButton(
        material = Material.OAK_HANGING_SIGN,
        name = "<!i><#C7A300>ℹ <#FFD700>Что это такое?",
        lore = listOf(
            "",
            "<!i><#C7A300> ◆ <#FFE68A>Шалкер можно будет открыть лишь",
            "<!i><b>  </b><#FFE68A> <#FFF3E0>предметом-ключом<#FFE68A>, который находится",
            "<!i><b>  </b><#FFE68A>в определённом слоте ниже.",
            ""
        )
    )

    private fun buildResetButton(session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.RED_DYE,
        name = "<!i><#FF1500>⚠ Сбросить ключ",
        lore = emptyList(),
        action = { event ->
            LockItemSupport.clear(session.editableItem)
            refreshDynamicButtons(event.menu, session)
        }
    )

    private fun buildLockSlotButton(session: ItemEditSession) = buttonFactory.lockKeySlotButton(
        LockItemSupport.preview(session.editableItem)
    ) { event ->
        val player = event.player
        val currentKey = LockItemSupport.preview(session.editableItem)
        val cursorItem = player.itemOnCursor

        if (currentKey == null) {
            if (LockItemSupport.isEmpty(cursorItem)) {
                return@lockKeySlotButton
            }

            LockItemSupport.set(session.editableItem, cursorItem.clone())
            player.setItemOnCursor(null)
            refreshDynamicButtons(event.menu, session)
            return@lockKeySlotButton
        }

        if (LockItemSupport.isEmpty(cursorItem)) {
            player.setItemOnCursor(currentKey.clone())
            LockItemSupport.clear(session.editableItem)
            refreshDynamicButtons(event.menu, session)
            return@lockKeySlotButton
        }

        val nextKey = cursorItem.clone()
        player.setItemOnCursor(currentKey.clone())
        LockItemSupport.set(session.editableItem, nextKey)
        refreshDynamicButtons(event.menu, session)
    }
}
