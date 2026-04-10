package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.enchant.EnchantmentSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.layout.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.entity.Player

class EnchantmentsActivePage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, page: Int, back: (Player, ItemEditSession) -> Unit) {
        val entries = EnchantmentSupport.entries(session.editableItem.itemMeta)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Зачарования → Активные"
                } else {
                    "<!i>▍ Зачарования → Активные [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = { pagePlayer, pageSession, pageIndex -> open(pagePlayer, pageSession, pageIndex, back) },
            backOnFirstPage = back,
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить зачарование"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.ENCHANTMENT) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex, back)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                val meta = clearSession.editableItem.itemMeta
                if (meta != null) {
                    EnchantmentSupport.clear(meta)
                    clearSession.editableItem.itemMeta = meta
                    support.transition(clearSession) { open(clearPlayer, clearSession, 0, back) }
                }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, globalIndex, entry ->
                buttonFactory.actionButton(
                    material = Material.ENCHANTED_BOOK,
                    name = "<!i><#C7A300>◎ <#FFD700>Зачарование №${globalIndex + 1}",
                    lore = listOf(
                        "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
                        "<!i>",
                        "<!i><#FFD700>Параметры:",
                        "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>${entry.displayName}",
                        "<!i><#C7A300> ● <#FFE68A>Уровень: <#FFF3E0>${entry.level}",
                        "<!i>"
                    ),
                    action = {
                        val meta = entrySession.editableItem.itemMeta ?: return@actionButton
                        EnchantmentSupport.remove(meta, entry.enchantment)
                        entrySession.editableItem.itemMeta = meta
                        val afterSize = EnchantmentSupport.entries(entrySession.editableItem.itemMeta).size
                        val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                        support.transition(entrySession) { open(entryPlayer, entrySession, targetPage, back) }
                    }
                )
            }
        )
    }
}
