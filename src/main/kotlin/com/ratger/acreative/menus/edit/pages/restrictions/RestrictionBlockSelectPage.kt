package com.ratger.acreative.menus.edit.pages.restrictions

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.common.PagedSelectionLayout
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.restrictions.ItemRestrictionSupport
import com.ratger.acreative.menus.edit.restrictions.RestrictionMode
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class RestrictionBlockSelectPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestSignInput: (Player, Array<String>, (Player, String?) -> Unit, (Player) -> Unit) -> Unit
) {

    fun open(
        player: Player,
        session: ItemEditSession,
        mode: RestrictionMode,
        page: Int,
        openParent: (Player, ItemEditSession) -> Unit,
        multiSelect: Boolean = false,
        onSelected: (Player, ItemEditSession) -> Unit
    ) {
        val options = ItemRestrictionSupport.blockOptions()
        val selectedKeys = ItemRestrictionSupport.keys(session.editableItem, mode)
        val totalPages = maxOf(1, (options.size + PagedSelectionLayout.workSlots.size - 1) / PagedSelectionLayout.workSlots.size)
        val pageIndex = page.coerceIn(0, totalPages - 1)

        val from = pageIndex * PagedSelectionLayout.workSlots.size
        val to = minOf(options.size, from + PagedSelectionLayout.workSlots.size)
        val pageEntries = options.subList(from, to)

        val menu = support.buildMenu(
            title = "${mode.blockSelectTitlePrefix} [${pageIndex + 1}/$totalPages]",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(PagedSelectionLayout.BACK_SLOT, PagedSelectionLayout.FORWARD_SLOT, 40) + PagedSelectionLayout.workSlots,
            session = session
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        PagedSelectionLayout.blackSlots.forEach { menu.setButton(it, black) }
        PagedSelectionLayout.graySlots.forEach { menu.setButton(it, gray) }

        menu.setButton(PagedSelectionLayout.BACK_SLOT, buttonFactory.backButton("◀ Назад") {
            support.transition(session) {
                if (pageIndex > 0) open(player, session, mode, pageIndex - 1, openParent, multiSelect, onSelected)
                else openParent(player, session)
            }
        })

        menu.setButton(PagedSelectionLayout.FORWARD_SLOT, buttonFactory.forwardButton("Вперёд ▶") {
            support.transition(session) {
                val nextPage = if (pageIndex + 1 < totalPages) pageIndex + 1 else 0
                open(player, session, mode, nextPage, openParent, multiSelect, onSelected)
            }
        })

        menu.setButton(40, buttonFactory.restrictionBlockPageButton(pageIndex + 1) {
            requestSignInput(
                player,
                arrayOf("", "↑ Страница ↑", "", ""),
                { submitPlayer, input ->
                    support.transition(session) {
                        val parsed = input?.trim()?.toIntOrNull()
                        val targetPage = (parsed?.coerceIn(1, totalPages) ?: (pageIndex + 1)) - 1
                        open(submitPlayer, session, mode, targetPage, openParent, multiSelect, onSelected)
                    }
                },
                { leavePlayer ->
                    support.transition(session) { open(leavePlayer, session, mode, pageIndex, openParent, multiSelect, onSelected) }
                }
            )
        })

        pageEntries.forEachIndexed { index, entry ->
            val slot = PagedSelectionLayout.workSlots[index]
            val selected = selectedKeys.contains(entry.key)
            menu.setButton(slot, buttonFactory.restrictionBlockTypeEntryButton(
                displayName = entry.displayName,
                modelId = entry.modelId,
                selected = selected
            ) {
                ItemRestrictionSupport.add(session.editableItem, mode, entry.key)
                support.transition(session) {
                    if (multiSelect) {
                        open(player, session, mode, pageIndex, openParent, multiSelect, onSelected)
                    } else {
                        onSelected(player, session)
                    }
                }
            })
        }

        menu.open(player)
    }
}
