package com.ratger.acreative.menus.edit.pages.restrictions

import com.ratger.acreative.menus.edit.restrictions.ItemRestrictionSupport
import com.ratger.acreative.menus.edit.restrictions.RestrictionMode
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.pages.common.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

class RestrictionsListPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRestrictionsRoot: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, mode: RestrictionMode, page: Int = 0) {
        val entries = ItemRestrictionSupport.entries(session.editableItem, mode)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) mode.listTitle else "${mode.listTitle} [${window.pageIndex + 1}/${window.totalPages}]"
            },
            openPage = { pagePlayer, pageSession, pageIndex -> open(pagePlayer, pageSession, mode, pageIndex) },
            backOnFirstPage = openRestrictionsRoot,
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = mode.addButtonTitle
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, applyKind(mode)) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, mode, pageIndex)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                ItemRestrictionSupport.clear(clearSession.editableItem, mode)
                support.transition(clearSession) { open(clearPlayer, clearSession, mode, 0) }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, _, entry ->
                buttonFactory.actionButton(
                    material = entry.material ?: mode.itemMaterialFallback,
                    name = "<!i><#C7A300>◎ <#FFD700>${entry.displayId}",
                    lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить"),
                    action = {
                        ItemRestrictionSupport.remove(entrySession.editableItem, mode, entry.key)
                        val afterSize = ItemRestrictionSupport.entries(entrySession.editableItem, mode).size
                        val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                        support.transition(entrySession) { open(entryPlayer, entrySession, mode, targetPage) }
                    }
                )
            }
        )
    }

    private fun applyKind(mode: RestrictionMode): EditorApplyKind = when (mode) {
        RestrictionMode.CAN_PLACE_ON -> EditorApplyKind.CAN_PLACE_ON
        RestrictionMode.CAN_BREAK -> EditorApplyKind.CAN_BREAK
    }
}
