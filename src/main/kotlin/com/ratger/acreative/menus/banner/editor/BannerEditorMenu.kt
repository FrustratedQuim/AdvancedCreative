package com.ratger.acreative.menus.banner.editor

import com.ratger.acreative.menus.banner.BannerButtonFactory
import com.ratger.acreative.menus.banner.service.BannerCatalog
import com.ratger.acreative.menus.banner.service.BannerPatternSupport
import com.ratger.acreative.menus.common.MenuUiSupport
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class BannerEditorMenu(
    private val support: BannerEditorMenuSupport,
    private val buttonFactory: BannerButtonFactory,
    private val requestSignInput: (
        player: Player,
        templateLines: Array<String>,
        onSubmit: (Player, String?) -> Unit,
        onLeave: (Player) -> Unit
    ) -> Unit,
    private val openMainMenu: (Player) -> Unit
) {
    private companion object {
        const val EDITOR_MENU_SIZE = 45
        const val PICKER_MENU_SIZE = 54
        val EDITOR_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
        val EDITOR_PATTERN_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)
        val BASE_PICKER_SLOTS = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)
        val PICKER_BLACK_SLOTS = setOf(0, 1, 3, 5, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 52, 53)
        val PICKER_PATTERN_SLOTS = listOf(19, 20, 21, 22, 23, 24, 25)
        val PICKER_COLOR_SLOTS = listOf(28, 29, 30, 31, 32, 33, 34)
    }

    fun open(player: Player, session: BannerEditorSession) {
        val interactiveTopSlots = buildSet {
            add(4)
            add(39)
            add(41)
            addAll(EDITOR_PATTERN_SLOTS.take(BannerPatternSupport.visiblePatterns(session.editableBanner).size).toSet())
            if (session.openedFromMainMenu) {
                add(18)
            }
        }

        val menu = support.buildMenu(
            title = "<!i>▍ Редактор флага",
            menuSize = EDITOR_MENU_SIZE,
            rows = MenuRows.FIVE,
            interactiveTopSlots = interactiveTopSlots,
            session = session,
            allowPlayerInventoryClicks = true,
            blockShiftClickFromPlayerInventory = true
        )

        support.fillBase(menu, EDITOR_MENU_SIZE, EDITOR_BLACK_SLOTS)
        menu.setClickListener(buildEditorClickListener(session))
        refreshEditorButtons(player, session, menu)
        menu.open(player)
    }

    private fun refreshEditorButtons(player: Player, session: BannerEditorSession, menu: Menu) {
        if (session.openedFromMainMenu) {
            menu.setButton(18, buttonFactory.backButton {
                support.finishSession(player, session) { openMainMenu(player) }
            })
        }

        menu.setButton(4, buttonFactory.editorInsertSlotButton(session.editableBanner) { event ->
            handleEditorInsertSlot(event, session)
        })
        menu.setButton(39, buildEditorAddPatternButton(player, session, menu))
        menu.setButton(41, buttonFactory.editorClearPatternsButton {
            BannerPatternSupport.clearPatterns(session.editableBanner)
            reopenEditor(player, session)
        })

        val visiblePatterns = BannerPatternSupport.visiblePatterns(session.editableBanner)
        EDITOR_PATTERN_SLOTS.forEach { menu.setButton(it, buttonFactory.airButton()) }
        visiblePatterns.forEachIndexed { index, visiblePattern ->
            val slot = EDITOR_PATTERN_SLOTS.getOrNull(index) ?: return@forEachIndexed
            menu.setButton(
                slot,
                buttonFactory.editorPatternEntryButton(
                    baseMaterial = session.editableBanner?.type ?: Material.RED_BANNER,
                    pattern = visiblePattern.pattern,
                    displayName = visiblePattern.descriptor.displayName
                ) { event ->
                    when {
                        event.isLeft || event.isShiftLeft -> {
                            BannerPatternSupport.removePatternAt(session.editableBanner, visiblePattern.actualIndex)
                            reopenEditor(player, session)
                        }

                        event.isRight || event.isShiftRight -> {
                            session.selectedPatternType = visiblePattern.pattern.pattern
                            session.selectedColor = visiblePattern.pattern.color
                            session.editingPatternActualIndex = visiblePattern.actualIndex
                            session.pickerPage = 0
                            support.transition(session) { openPatternPicker(player, session) }
                        }

                        MenuUiSupport.isDropClick(event) -> {
                            requestPatternMoveInput(player, session, visiblePattern.actualIndex)
                        }
                    }
                }
            )
        }
    }

    fun openBasePicker(player: Player, session: BannerEditorSession) {
        val interactiveTopSlots = buildSet {
            add(18)
            addAll(BASE_PICKER_SLOTS.take(BannerCatalog.colors.size))
        }

        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Выбор флага",
            menuSize = EDITOR_MENU_SIZE,
            rows = MenuRows.FIVE,
            interactiveTopSlots = interactiveTopSlots,
            session = session,
            allowPlayerInventoryClicks = true,
            blockShiftClickFromPlayerInventory = true
        )

        support.fillBase(menu, EDITOR_MENU_SIZE, EDITOR_BLACK_SLOTS)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { open(player, session) } })
        BASE_PICKER_SLOTS.forEach { menu.setButton(it, buttonFactory.airButton()) }
        BannerCatalog.colors.forEachIndexed { index, color ->
            val slot = BASE_PICKER_SLOTS.getOrNull(index) ?: return@forEachIndexed
            menu.setButton(slot, buttonFactory.baseBannerChoiceButton(color) {
                val amount = session.editableBanner?.amount?.coerceAtLeast(1) ?: 1
                session.editableBanner = ItemStack(color.bannerMaterial, amount)
                reopenEditor(player, session)
            })
        }
        menu.open(player)
    }

    fun openPatternPicker(player: Player, session: BannerEditorSession) {
        val totalPatternPages = pagesFor(BannerCatalog.patterns.size, PICKER_PATTERN_SLOTS.size)
        val totalColorPages = pagesFor(BannerCatalog.colors.size, PICKER_COLOR_SLOTS.size)
        val totalPages = maxOf(totalPatternPages, totalColorPages, 1)
        session.pickerPage = session.pickerPage.coerceIn(0, totalPages - 1)

        val interactiveTopSlots = mutableSetOf(18, 27, 49)
        if (session.pickerPage < totalPages - 1) {
            interactiveTopSlots += setOf(26, 35)
        }
        interactiveTopSlots += PICKER_PATTERN_SLOTS
        interactiveTopSlots += PICKER_COLOR_SLOTS

        val menu = support.buildMenu(
            title = "<!i>${pickerTitle(session.pickerPage + 1, totalPages)}",
            menuSize = PICKER_MENU_SIZE,
            rows = MenuRows.SIX,
            interactiveTopSlots = interactiveTopSlots,
            session = session,
            allowPlayerInventoryClicks = true,
            blockShiftClickFromPlayerInventory = true
        )

        support.fillBase(menu, PICKER_MENU_SIZE, PICKER_BLACK_SLOTS)
        refreshPatternPickerButtons(player, session, menu, totalPages)
        menu.open(player)
    }

    private fun refreshPatternPickerButtons(
        player: Player,
        session: BannerEditorSession,
        menu: Menu,
        totalPages: Int
    ) {
        val page = session.pickerPage
        val patternStart = page * PICKER_PATTERN_SLOTS.size
        val colorStart = page * PICKER_COLOR_SLOTS.size

        menu.setButton(2, session.selectedColor?.let(BannerCatalog::colorByDye)?.let(buttonFactory::pickerSelectedColorButton) ?: buttonFactory.blackFiller())
        menu.setButton(4, session.editableBanner?.let {
            buttonFactory.previewButton(
                BannerPatternSupport.previewWithPattern(
                    item = it,
                    color = session.selectedColor,
                    type = session.selectedPatternType,
                    replaceActualIndex = session.editingPatternActualIndex
                )
            )
        } ?: buttonFactory.editorInsertSlotButton(null) { })
        menu.setButton(6, session.selectedPatternType?.let(BannerCatalog::patternByType)?.let(buttonFactory::pickerSelectedPatternButton) ?: buttonFactory.blackFiller())

        val onBack = {
            if (page > 0) {
                session.pickerPage = page - 1
                support.transition(session) { openPatternPicker(player, session) }
            } else {
                session.editingPatternActualIndex = null
                support.transition(session) { open(player, session) }
            }
        }
        menu.setButton(18, buttonFactory.backButton(onBack))
        menu.setButton(27, buttonFactory.backButton(onBack))

        if (page < totalPages - 1) {
            val onForward = {
                session.pickerPage = page + 1
                support.transition(session) { openPatternPicker(player, session) }
            }
            menu.setButton(26, buttonFactory.forwardButton(onForward))
            menu.setButton(35, buttonFactory.forwardButton(onForward))
        }

        PICKER_PATTERN_SLOTS.forEach { menu.setButton(it, buttonFactory.airButton()) }
        BannerCatalog.patterns.drop(patternStart).take(PICKER_PATTERN_SLOTS.size).forEachIndexed { index, descriptor ->
            val slot = PICKER_PATTERN_SLOTS[index]
            menu.setButton(slot, buttonFactory.pickerPatternButton(Material.BLACK_BANNER, descriptor) {
                session.selectedPatternType = descriptor.patternType
                support.transition(session) { openPatternPicker(player, session) }
            })
        }

        PICKER_COLOR_SLOTS.forEach { menu.setButton(it, buttonFactory.airButton()) }
        BannerCatalog.colors.drop(colorStart).take(PICKER_COLOR_SLOTS.size).forEachIndexed { index, descriptor ->
            val slot = PICKER_COLOR_SLOTS[index]
            menu.setButton(slot, buttonFactory.pickerColorButton(descriptor) {
                session.selectedColor = descriptor.dyeColor
                support.transition(session) { openPatternPicker(player, session) }
            })
        }

        menu.setButton(49, buttonFactory.pickerConfirmButton {
            if (session.selectedPatternType == null) {
                showTemporaryWarning(
                    menu = menu,
                    slot = 49,
                    title = "<!i><#FF1500>⚠ Выберите тип",
                    restore = { buttonFactory.pickerConfirmButton { confirmPatternSelection(player, session, menu) } }
                )
                return@pickerConfirmButton
            }
            if (session.selectedColor == null) {
                showTemporaryWarning(
                    menu = menu,
                    slot = 49,
                    title = "<!i><#FF1500>⚠ Выберите цвет",
                    restore = { buttonFactory.pickerConfirmButton { confirmPatternSelection(player, session, menu) } }
                )
                return@pickerConfirmButton
            }
            confirmPatternSelection(player, session, menu)
        })
    }

    private fun confirmPatternSelection(player: Player, session: BannerEditorSession, menu: Menu) {
        val selectedPattern = session.selectedPatternType
        val selectedColor = session.selectedColor
        val editableBanner = session.editableBanner

        if (editableBanner == null) {
            showTemporaryWarning(
                menu = menu,
                slot = 49,
                title = "<!i><#FF1500>⚠ Вложите флаг",
                restore = { buttonFactory.pickerConfirmButton { confirmPatternSelection(player, session, menu) } }
            )
            return
        }

        if (selectedPattern == null) {
            showTemporaryWarning(
                menu = menu,
                slot = 49,
                title = "<!i><#FF1500>⚠ Выберите тип",
                restore = { buttonFactory.pickerConfirmButton { confirmPatternSelection(player, session, menu) } }
            )
            return
        }

        if (selectedColor == null) {
            showTemporaryWarning(
                menu = menu,
                slot = 49,
                title = "<!i><#FF1500>⚠ Выберите цвет",
                restore = { buttonFactory.pickerConfirmButton { confirmPatternSelection(player, session, menu) } }
            )
            return
        }

        val editingPatternActualIndex = session.editingPatternActualIndex
        if (editingPatternActualIndex != null) {
            BannerPatternSupport.replacePatternAt(
                item = editableBanner,
                actualIndex = editingPatternActualIndex,
                color = selectedColor,
                type = selectedPattern
            )
            session.editingPatternActualIndex = null
            support.transition(session) { open(player, session) }
            return
        }

        if (BannerPatternSupport.patternCount(editableBanner) >= BannerPatternSupport.EDITOR_VISIBLE_PATTERN_LIMIT) {
            showTemporaryWarning(
                menu = menu,
                slot = 49,
                title = "<!i><#FF1500>⚠ Превышен лимит",
                restore = { buttonFactory.pickerConfirmButton { confirmPatternSelection(player, session, menu) } }
            )
            return
        }

        BannerPatternSupport.addPattern(editableBanner, selectedColor, selectedPattern)
        support.transition(session) { open(player, session) }
    }

    private fun buildEditorClickListener(session: BannerEditorSession): (ClickEvent) -> Boolean = { event ->
        if (event.rawSlot in 0 until EDITOR_MENU_SIZE) {
            when {
                event.rawSlot == 4 -> true
                else -> event.rawSlot in interactiveEditorSlots(session)
            }
        } else {
            !(event.isShiftLeft && handleShiftLeftFromPlayerInventory(event, event.player, session))
        }
    }

    private fun interactiveEditorSlots(session: BannerEditorSession): Set<Int> {
        return buildSet {
            add(4)
            add(39)
            add(41)
            if (session.openedFromMainMenu) {
                add(18)
            }
            addAll(EDITOR_PATTERN_SLOTS.take(BannerPatternSupport.visiblePatterns(session.editableBanner).size))
        }
    }

    private fun handleEditorInsertSlot(event: ClickEvent, session: BannerEditorSession) {
        val player = event.player
        val menu = event.menu
        val currentBanner = session.editableBanner
        val cursorItem = player.itemOnCursor

        if (currentBanner == null) {
            if (!isBanner(cursorItem)) {
                if (isEmpty(cursorItem) && (event.isLeft || event.isShiftLeft)) {
                    support.transition(session) { openBasePicker(player, session) }
                }
                return
            }

            session.editableBanner = cursorItem.clone()
            player.setItemOnCursor(null)
            refreshEditorButtons(player, session, menu)
            return
        }

        if (isEmpty(cursorItem)) {
            if (event.isShiftLeft) {
                giveToInventoryOrDrop(player, currentBanner.clone())
                session.editableBanner = null
                refreshEditorButtons(player, session, menu)
                return
            }

            player.setItemOnCursor(currentBanner.clone())
            session.editableBanner = null
            refreshEditorButtons(player, session, menu)
            return
        }

        if (!isBanner(cursorItem)) {
            return
        }

        session.editableBanner = cursorItem.clone()
        player.setItemOnCursor(currentBanner.clone())
        refreshEditorButtons(player, session, menu)
    }

    private fun handleShiftLeftFromPlayerInventory(event: ClickEvent, player: Player, session: BannerEditorSession): Boolean {
        val clickedInventory = event.clickedInventory ?: return false
        val clickedItem = event.clickedItem ?: return false
        if (!isBanner(clickedItem)) {
            return false
        }

        val previousBanner = session.editableBanner?.clone()
        session.editableBanner = clickedItem.clone()

        if (previousBanner == null) {
            clickedInventory.setItem(event.slot, null)
        } else {
            clickedInventory.setItem(event.slot, previousBanner)
        }

        reopenEditor(player, session)
        return true
    }

    private fun giveToInventoryOrDrop(player: Player, item: ItemStack) {
        val remainingAmount = com.ratger.acreative.utils.PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, item)
        if (remainingAmount > 0) {
            player.world.dropItemNaturally(
                player.location.clone().add(0.0, 1.0, 0.0),
                item.clone().apply { amount = remainingAmount }
            )
        }
    }

    private fun reopenEditor(player: Player, session: BannerEditorSession) {
        support.transition(session) { open(player, session) }
    }

    private fun buildEditorAddPatternButton(
        player: Player,
        session: BannerEditorSession,
        menu: Menu
    ): Button = buttonFactory.editorAddPatternButton {
        session.editingPatternActualIndex = null
        if (session.editableBanner == null) {
            showTemporaryWarning(
                menu = menu,
                slot = 39,
                title = "<!i><#FF1500>⚠ Вложите флаг",
                restore = { buildEditorAddPatternButton(player, session, menu) }
            )
            return@editorAddPatternButton
        }
        if (BannerPatternSupport.patternCount(session.editableBanner) >= BannerPatternSupport.EDITOR_VISIBLE_PATTERN_LIMIT) {
            showTemporaryWarning(
                menu = menu,
                slot = 39,
                title = "<!i><#FF1500>⚠ Превышен лимит",
                restore = { buildEditorAddPatternButton(player, session, menu) }
            )
            return@editorAddPatternButton
        }
        support.transition(session) { openPatternPicker(player, session) }
    }

    private fun requestPatternMoveInput(player: Player, session: BannerEditorSession, sourceActualIndex: Int) {
        val editableBanner = session.editableBanner
        val maxIndex = BannerPatternSupport.patternCount(editableBanner) - 1
        if (editableBanner == null || maxIndex <= 0) {
            return
        }

        support.transition(session) {
            requestSignInput(
                player,
                arrayOf("", "↑ № позиции ↑", "", ""),
                { submitPlayer, input ->
                    val parsedTarget = input?.trim()?.toIntOrNull()
                    if (parsedTarget != null) {
                        val targetActualIndex = (parsedTarget - 1).coerceIn(0, maxIndex)
                        BannerPatternSupport.movePattern(
                            item = session.editableBanner,
                            fromIndex = sourceActualIndex,
                            toIndex = targetActualIndex
                        )
                    }
                    open(submitPlayer, session)
                },
                { leavePlayer ->
                    open(leavePlayer, session)
                }
            )
        }
    }

    private fun showTemporaryWarning(
        menu: Menu,
        slot: Int,
        title: String,
        restore: () -> Button
    ) {
        support.replaceSlotTemporarily(
            menu = menu,
            slot = slot,
            temporaryButton = buttonFactory.temporaryBarrierButton(title),
            restoreAfterTicks = 30L,
            restoreButton = restore
        )
    }

    private fun pickerTitle(page: Int, totalPages: Int): String {
        return com.ratger.acreative.menus.banner.service.BannerTextSupport.titleWithPages(
            "▍ Редактор → Рисунок",
            page,
            totalPages
        )
    }

    private fun pagesFor(totalItems: Int, pageSize: Int): Int {
        if (totalItems <= 0) return 1
        return (totalItems + pageSize - 1) / pageSize
    }

    private fun isBanner(item: ItemStack?): Boolean = BannerPatternSupport.isBanner(item)

    private fun isEmpty(item: ItemStack?): Boolean = item == null || item.type == Material.AIR || item.amount <= 0
}
