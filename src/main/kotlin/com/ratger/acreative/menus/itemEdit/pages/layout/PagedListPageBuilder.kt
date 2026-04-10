package com.ratger.acreative.menus.itemEdit.pages.layout

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.button.Button

class PagedListPageBuilder(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val layout: PagedListLayout = ItemEditPageLayouts.pagedList
) {
    data class PageWindow<T>(
        val entries: List<T>,
        val pageIndex: Int,
        val totalPages: Int,
        val pageSize: Int,
        val fromIndex: Int,
        val pageEntries: List<T>
    )

    data class ActionSlot(
        val material: Material,
        val name: String,
        val lore: List<String> = emptyList(),
        val onClick: (Player, ItemEditSession, Int) -> Unit
    )

    fun <T> open(
        player: Player,
        session: ItemEditSession,
        page: Int,
        entries: List<T>,
        title: (PageWindow<T>) -> String,
        openPage: (Player, ItemEditSession, Int) -> Unit,
        backOnFirstPage: (Player, ItemEditSession) -> Unit,
        addAction: ActionSlot,
        clearAction: ActionSlot,
        entryButton: (player: Player, session: ItemEditSession, pageWindow: PageWindow<T>, globalIndex: Int, entry: T) -> Button
    ) {
        val pageSize = layout.workSlots.size
        val totalPages = maxOf(1, (entries.size + pageSize - 1) / pageSize)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        val from = pageIndex * pageSize
        val to = minOf(entries.size, from + pageSize)
        val pageEntries = entries.subList(from, to)
        val pageWindow = PageWindow(entries, pageIndex, totalPages, pageSize, from, pageEntries)

        val menu = support.buildMenu(
            title = title(pageWindow),
            menuSize = layout.menuSize,
            rows = layout.rows,
            interactiveTopSlots = layout.interactiveTopSlots,
            session = session
        )

        val blackFiller = buttonFactory.blackFillerButton()
        val grayFiller = buttonFactory.grayFillerButton()
        layout.blackSlots.forEach { menu.setButton(it, blackFiller) }
        layout.graySlots.forEach { menu.setButton(it, grayFiller) }

        if (pageIndex > 0) {
            menu.setButton(layout.backSlot, buttonFactory.backButton {
                support.transition(session) { openPage(player, session, pageIndex - 1) }
            })
        } else {
            menu.setButton(layout.backSlot, buttonFactory.backButton {
                support.transition(session) { backOnFirstPage(player, session) }
            })
        }

        if (pageIndex + 1 < totalPages) {
            menu.setButton(layout.forwardSlot, buttonFactory.forwardButton {
                support.transition(session) { openPage(player, session, pageIndex + 1) }
            })
        }

        menu.setButton(layout.addSlot, buttonFactory.actionButton(
            material = addAction.material,
            name = addAction.name,
            lore = addAction.lore,
            action = { addAction.onClick(player, session, pageIndex) }
        ))

        menu.setButton(layout.clearSlot, buttonFactory.actionButton(
            material = clearAction.material,
            name = clearAction.name,
            lore = clearAction.lore,
            action = { clearAction.onClick(player, session, pageIndex) }
        ))

        pageEntries.forEachIndexed { localIndex, entry ->
            val globalIndex = from + localIndex
            val slot = layout.workSlots[localIndex]
            menu.setButton(slot, entryButton(player, session, pageWindow, globalIndex, entry))
        }

        menu.open(player)
    }

    fun coercePageIndexAfterUpdate(currentPage: Int, totalEntriesAfterUpdate: Int): Int {
        val totalPages = maxOf(1, (totalEntriesAfterUpdate + layout.workSlots.size - 1) / layout.workSlots.size)
        return currentPage.coerceAtMost(totalPages - 1)
    }
}
