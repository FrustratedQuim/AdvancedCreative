package com.ratger.acreative.menus.edit.pages.restrictions

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
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
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
    private val graySlots = setOf(
        1, 2, 3, 4, 5, 6, 7,
        37, 38, 39, 40, 41, 42, 43
    )
    private val workSlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    )

    fun open(
        player: Player,
        session: ItemEditSession,
        mode: RestrictionMode,
        page: Int,
        openParent: (Player, ItemEditSession) -> Unit,
        onSelected: (Player, ItemEditSession) -> Unit
    ) {
        val options = ItemRestrictionSupport.blockOptions()
        val selectedKeys = ItemRestrictionSupport.keys(session.editableItem, mode)
        val totalPages = maxOf(1, (options.size + workSlots.size - 1) / workSlots.size)
        val pageIndex = page.coerceIn(0, totalPages - 1)

        val from = pageIndex * workSlots.size
        val to = minOf(options.size, from + workSlots.size)
        val pageEntries = options.subList(from, to)

        val menu = support.buildMenu(
            title = "${mode.blockSelectTitlePrefix} [${pageIndex + 1}/$totalPages]",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 26, 40) + workSlots,
            session = session
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        blackSlots.forEach { menu.setButton(it, black) }
        graySlots.forEach { menu.setButton(it, gray) }

        menu.setButton(18, buttonFactory.backButton("◀ Назад") {
            support.transition(session) {
                if (pageIndex > 0) open(player, session, mode, pageIndex - 1, openParent, onSelected)
                else openParent(player, session)
            }
        })

        menu.setButton(26, buttonFactory.forwardButton("Вперёд ▶") {
            support.transition(session) {
                val nextPage = if (pageIndex + 1 < totalPages) pageIndex + 1 else 0
                open(player, session, mode, nextPage, openParent, onSelected)
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
                        open(submitPlayer, session, mode, targetPage, openParent, onSelected)
                    }
                },
                { leavePlayer ->
                    support.transition(session) { open(leavePlayer, session, mode, pageIndex, openParent, onSelected) }
                }
            )
        })

        pageEntries.forEachIndexed { index, entry ->
            val slot = workSlots[index]
            val selected = selectedKeys.contains(entry.key)
            menu.setButton(slot, buttonFactory.restrictionBlockTypeEntryButton(
                displayName = entry.displayName,
                modelId = entry.modelId,
                selected = selected
            ) {
                ItemRestrictionSupport.add(session.editableItem, mode, entry.key)
                support.transition(session) { onSelected(player, session) }
            })
        }

        menu.open(player)
    }
}
