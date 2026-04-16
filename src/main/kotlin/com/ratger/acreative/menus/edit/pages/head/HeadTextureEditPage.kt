package com.ratger.acreative.menus.edit.pages.head

import com.ratger.acreative.menus.edit.head.HeadTextureMutationSupport
import com.ratger.acreative.menus.edit.head.HeadTextureSnapshot
import com.ratger.acreative.menus.edit.head.HeadTextureSource
import com.ratger.acreative.menus.edit.head.HeadTextureValueBookSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class HeadTextureEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val mutationSupport: HeadTextureMutationSupport,
    private val textureValueBookSupport: HeadTextureValueBookSupport,
    private val openBack: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        session.headTextureSectionActive = true
        syncVirtualTextureValue(session, force = session.headTextureValueInputBook == null)
        val isLoading = session.headTextureLoadingToken != null

        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Текстура головы",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = if (isLoading) setOf(18) else setOf(18, 29, 30, 31, 33),
            session = session,
            allowPlayerInventoryClicks = !isLoading
        )

        support.fillBase(menu, 45, setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14))
        menu.setButton(18, buttonFactory.backButton {
            session.headTextureSectionActive = false
            session.headTextureLoadingToken = null
            support.transition(session) { openBack(player, session) }
        })

        if (isLoading) {
            applyLoadingButtons(menu)
            menu.open(player)
            return
        }

        menu.setClickListener(buildClickListener(session))
        refreshDynamicButtons(player, session, menu)
        menu.open(player)
    }

    private fun buildClickListener(session: ItemEditSession) = { event: ClickEvent ->
        if (event.rawSlot in 0 until 45) {
            if (event.rawSlot == 30 && event.isShiftLeft && moveStoredValueBookToPreferredEmptyInventorySlot(event, session)) {
                false
            } else {
                event.rawSlot in setOf(18, 29, 30, 31, 33)
            }
        } else {
            !(event.isShiftLeft && handleShiftLeftFromPlayerInventory(event, session))
        }
    }

    private fun applyLoadingButtons(menu: Menu) {
        val loading = buttonFactory.actionButton(Material.CLOCK, "<!i><#00FF40>Загрузка текстуры..", emptyList())
        menu.setButton(13, loading)
        menu.setButton(29, loading)
        menu.setButton(30, loading)
        menu.setButton(31, loading)
        menu.setButton(33, loading)
    }

    private fun refreshDynamicButtons(player: Player, session: ItemEditSession, menu: Menu) {
        val snapshot = HeadTextureSnapshot.fromItem(session.editableItem, session.headTextureSource)
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(29, onlineButton(player, session, snapshot, menu))
        menu.setButton(30, valueInputSlotButton(session))
        menu.setButton(31, licensedButton(player, session, snapshot, menu))
        menu.setButton(33, getValueButton(player, session, menu))
    }

    private fun getValueButton(player: Player, session: ItemEditSession, menu: Menu): Button = buttonFactory.actionButton(
        material = Material.ENDER_EYE,
        name = "<!i><#00FF40>₪ Получить value",
        lore = emptyList(),
        action = {
            val value = mutationSupport.texturesValue(session.editableItem)
            if (value.isNullOrBlank()) {
                showTemporaryBarrierWarn(
                    menu = menu,
                    slot = 33,
                    title = "<!i><#FF1500>⚠ У этой головы нет текстуры",
                    restore = { getValueButton(player, session, menu) }
                )
                return@actionButton
            }

            val emptySlot = findFirstEmptyInventorySlot(player.inventory)
            if (emptySlot == null) {
                showTemporaryBarrierWarn(
                    menu = menu,
                    slot = 33,
                    title = "<!i><#FF1500>⚠ Освободите инвентарь",
                    restore = { getValueButton(player, session, menu) }
                )
                return@actionButton
            }

            player.inventory.setItem(emptySlot, textureValueBookSupport.createValueBook(value))
        }
    )

    private fun valueInputSlotButton(session: ItemEditSession): Button = buttonFactory.headTextureValueInputSlotButton(
        valueBook = session.headTextureValueInputBook,
        action = { event ->
            val player = event.player
            val currentBook = session.headTextureValueInputBook
            val cursorItem = player.itemOnCursor

            if (currentBook == null) {
                if (isEmpty(cursorItem)) {
                    return@headTextureValueInputSlotButton
                }

                if (!tryApplyAndStoreInputItem(event, session, cursorItem.clone())) {
                    return@headTextureValueInputSlotButton
                }

                player.setItemOnCursor(null)
                return@headTextureValueInputSlotButton
            }

            if (isEmpty(cursorItem)) {
                restoreEditableTextureFromVirtualValue(session)
                player.setItemOnCursor(materializeStoredValueItem(session))
                session.headTextureValueInputBook = null
                refreshDynamicButtons(player, session, event.menu)
                return@headTextureValueInputSlotButton
            }

            if (!tryApplyAndStoreInputItem(event, session, cursorItem.clone())) {
                return@headTextureValueInputSlotButton
            }

            player.setItemOnCursor(currentBook.clone())
        }
    )

    private fun tryApplyAndStoreInputItem(event: ClickEvent, session: ItemEditSession, candidate: ItemStack): Boolean {
        val extracted = extractTextureValueCandidate(candidate) ?: return false
        return when (mutationSupport.applyFromTextureValue(session.editableItem, extracted.value)) {
            is HeadTextureMutationSupport.MutationResult.Failure -> false
            HeadTextureMutationSupport.MutationResult.Success -> {
                session.headTextureSource = HeadTextureSource.TEXTURE_VALUE
                session.headTextureValueInputBook = extracted.storedItem
                refreshDynamicButtons(event.player, session, event.menu)
                true
            }
        }
    }

    private fun handleShiftLeftFromPlayerInventory(event: ClickEvent, session: ItemEditSession): Boolean {
        val clickedInventory = event.clickedInventory ?: return false
        val clickedItem = event.clickedItem ?: return false
        if (!isSupportedValueInputItem(clickedItem)) {
            return false
        }

        val previousItem = session.headTextureValueInputBook?.clone()
        if (!tryApplyAndStoreInputItem(event, session, clickedItem.clone())) {
            return true
        }

        clickedInventory.setItem(event.slot, null)
        if (previousItem != null) {
            giveToInventoryOrDrop(event.player, previousItem.clone())
        }

        return true
    }

    private fun moveStoredValueBookToPreferredEmptyInventorySlot(event: ClickEvent, session: ItemEditSession): Boolean {
        val storedBook = materializeStoredValueItem(session) ?: return false

        giveToInventoryOrDrop(event.player, storedBook.clone())
        restoreEditableTextureFromVirtualValue(session)
        session.headTextureValueInputBook = null
        refreshDynamicButtons(event.player, session, event.menu)
        return true
    }

    private data class TextureValueCandidate(
        val value: String,
        val storedItem: ItemStack
    )

    private fun extractTextureValueCandidate(candidate: ItemStack): TextureValueCandidate? {
        if (textureValueBookSupport.isBookItem(candidate)) {
            val value = textureValueBookSupport.extractTextureValue(candidate) ?: return null
            return TextureValueCandidate(value = value, storedItem = candidate.clone())
        }

        if (candidate.type != Material.PLAYER_HEAD) {
            return null
        }

        val value = mutationSupport.texturesValue(candidate) ?: return null
        if (!textureValueBookSupport.isValidTextureValue(value)) {
            return null
        }

        return TextureValueCandidate(value = value, storedItem = candidate.clone())
    }

    private fun isSupportedValueInputItem(item: ItemStack): Boolean {
        return textureValueBookSupport.isBookItem(item) || item.type == Material.PLAYER_HEAD
    }

    private fun giveToInventoryOrDrop(player: Player, item: ItemStack) {
        val remaining = PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, item)
        if (remaining <= 0) {
            return
        }

        val dropItem = item.clone().apply { amount = remaining }
        player.world.dropItemNaturally(player.location.clone().add(0.0, 1.0, 0.0), dropItem)
    }

    private fun syncVirtualTextureValue(session: ItemEditSession, force: Boolean = false) {
        if (!force && session.headTextureValueInputBook != null) {
            return
        }

        val currentTextureValue = mutationSupport.texturesValue(session.editableItem)
        session.headTextureVirtualValue = currentTextureValue?.takeUnless { it.isBlank() }
    }

    private fun restoreEditableTextureFromVirtualValue(session: ItemEditSession) {
        val virtualValue = session.headTextureVirtualValue
        if (virtualValue.isNullOrBlank()) {
            mutationSupport.clearProfile(session.editableItem)
            session.headTextureSource = HeadTextureSource.NONE
            return
        }

        when (mutationSupport.applyFromTextureValue(session.editableItem, virtualValue)) {
            is HeadTextureMutationSupport.MutationResult.Failure -> {
                mutationSupport.clearProfile(session.editableItem)
                session.headTextureSource = HeadTextureSource.NONE
            }
            HeadTextureMutationSupport.MutationResult.Success -> {
                session.headTextureSource = HeadTextureSource.TEXTURE_VALUE
            }
        }
    }

    private fun materializeStoredValueItem(session: ItemEditSession): ItemStack? {
        return session.headTextureValueInputBook?.clone()
    }

    private fun showTemporaryBarrierWarn(menu: Menu, slot: Int, title: String, restore: () -> Button) {
        support.replaceSlotTemporarily(
            menu = menu,
            slot = slot,
            temporaryButton = buttonFactory.actionButton(
                material = Material.BARRIER,
                name = title,
                lore = emptyList()
            ),
            restoreAfterTicks = 30L,
            restoreButton = restore
        )
    }

    private fun findFirstEmptyInventorySlot(inventory: PlayerInventory): Int? {
        for (slot in 0..35) {
            val item = inventory.getItem(slot)
            if (item == null || item.type == Material.AIR) {
                return slot
            }
        }
        return null
    }

    private fun isEmpty(item: ItemStack?): Boolean {
        return item == null || item.type == Material.AIR
    }

    private fun onlineButton(player: Player, session: ItemEditSession, snapshot: HeadTextureSnapshot, menu: Menu): Button = if (snapshot.canShowOnlineActive) {
        buttonFactory.headTextureSourceButton(
            editedItem = session.editableItem,
            activeName = "<!i><#C7A300>◎ <#FFD700>От игрока: <#00FF40>${snapshot.profileName}",
            lore = sourceLoreOnline(active = true),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_ONLINE_NAME, this::open)
                    }
                }
            }
        )
    } else {
        buttonFactory.actionButton(
            material = Material.LIME_DYE,
            name = "<!i><#C7A300>⭘ <#FFD700>От игрока: <#FF1500>Нет",
            lore = sourceLoreOnline(active = false),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_ONLINE_NAME, this::open)
                    }
                }
            }
        )
    }

    private fun licensedButton(player: Player, session: ItemEditSession, snapshot: HeadTextureSnapshot, menu: Menu): Button = if (snapshot.canShowLicensedActive) {
        buttonFactory.headTextureSourceButton(
            editedItem = session.editableItem,
            activeName = "<!i><#C7A300>◎ <#FFD700>По лицензии: <#00FF40>${snapshot.profileName}",
            lore = sourceLoreLicensed(active = true),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_LICENSED_NAME, this::open)
                    }
                }
            }
        )
    } else {
        buttonFactory.actionButton(
            material = Material.PURPLE_DYE,
            name = "<!i><#C7A300>⭘ <#FFD700>По лицензии: <#FF1500>Нет",
            lore = sourceLoreLicensed(active = false),
            action = { event ->
                if (event.isRight || event.isShiftRight) {
                    clearAndRefresh(player, session, menu)
                } else if (event.isLeft || event.isShiftLeft) {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.HEAD_LICENSED_NAME, this::open)
                    }
                }
            }
        )
    }

    private fun clearAndRefresh(player: Player, session: ItemEditSession, menu: Menu) {
        mutationSupport.clearProfile(session.editableItem)
        session.headTextureSource = HeadTextureSource.NONE
        syncVirtualTextureValue(session, force = true)
        refreshDynamicButtons(player, session, menu)
    }

    private fun sourceLoreOnline(active: Boolean): List<String> = listOf(
        "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
        "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
        "",
        "<!i><#FFD700>Назначение:",
        "<!i><#C7A300> ● <#FFE68A>Берёт голову игрока <#FFF3E0>онлайн. ",
        "",
        "<!i><#FFD700>После нажатия:",
        "<!i><#C7A300> ● <#FFF3E0>/apply <ник><#FFE68A> <#C7A300>- <#FFE68A>задать ",
        if (active) "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отмена " else "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отменить ",
        ""
    )

    private fun sourceLoreLicensed(active: Boolean): List<String> = listOf(
        "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
        "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
        "",
        "<!i><#FFD700>Назначение:",
        "<!i><#C7A300> ● <#FFE68A>Берёт текстуру от ",
        "<!i><#C7A300> ● <#FFF3E0>лицензионного<#FFE68A> ника. ",
        "",
        "<!i><#FFD700>После нажатия:",
        "<!i><#C7A300> ● <#FFF3E0>/apply <ник><#FFE68A> <#C7A300>- <#FFE68A>задать ",
        if (active) "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отмена " else "<!i><#C7A300> ● <#FFF3E0>/apply cancel<#FFE68A> <#C7A300>- <#FFE68A>отменить ",
        ""
    )
}
