package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.enchant.EnchantmentSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class EnchantmentsActivePage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
    private val graySlots = setOf(
        1, 2, 3, 4, 5, 6, 7,
        10, 16,
        19, 25,
        28, 34,
        37, 38, 39, 40, 41, 42, 43
    )
    private val workSlots = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33)

    fun open(player: Player, session: ItemEditSession, page: Int, back: (Player, ItemEditSession) -> Unit) {
        val entries = EnchantmentSupport.entries(session.editableItem.itemMeta)
        val pageSize = workSlots.size
        val totalPages = maxOf(1, (entries.size + pageSize - 1) / pageSize)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        val title = if (totalPages == 1) {
            "<!i>▍ Зачарования → Активные"
        } else {
            "<!i>▍ Зачарования → Активные [${pageIndex + 1}/$totalPages]"
        }

        val menu = support.buildMenu(
            title = title,
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 26, 39, 41) + workSlots,
            session = session
        )

        val blackFiller = buttonFactory.blackFillerButton()
        val grayFiller = buttonFactory.grayFillerButton()
        blackSlots.forEach { menu.setButton(it, blackFiller) }
        graySlots.forEach { menu.setButton(it, grayFiller) }
        if (pageIndex > 0) {
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { open(player, session, pageIndex - 1, back) } })
        } else {
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { back(player, session) } })
        }
        if (pageIndex + 1 < totalPages) {
            menu.setButton(26, buttonFactory.forwardButton { support.transition(session) { open(player, session, pageIndex + 1, back) } })
        }

        menu.setButton(39, buttonFactory.actionButton(
            material = Material.LIME_DYE,
            name = "<!i><#00FF40>₪ Добавить зачарование",
            lore = emptyList(),
            action = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.ENCHANTMENT) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex, back)
                    }
                }
            }
        ))

        menu.setButton(41, buttonFactory.actionButton(
            material = Material.RED_DYE,
            name = "<!i><#FF1500>⚠ Удалить всё",
            lore = emptyList(),
            action = {
                val meta = session.editableItem.itemMeta ?: return@actionButton
                EnchantmentSupport.clear(meta)
                session.editableItem.itemMeta = meta
                support.transition(session) { open(player, session, 0, back) }
            }
        ))

        val from = pageIndex * pageSize
        val to = minOf(entries.size, from + pageSize)
        val pageEntries = entries.subList(from, to)

        pageEntries.forEachIndexed { localIndex, entry ->
            val globalIndex = from + localIndex
            val slot = workSlots[localIndex]
            menu.setButton(slot, buttonFactory.actionButton(
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
                    val meta = session.editableItem.itemMeta ?: return@actionButton
                    EnchantmentSupport.remove(meta, entry.enchantment)
                    session.editableItem.itemMeta = meta
                    val after = EnchantmentSupport.entries(session.editableItem.itemMeta)
                    val afterPages = maxOf(1, (after.size + workSlots.size - 1) / workSlots.size)
                    support.transition(session) { open(player, session, pageIndex.coerceAtMost(afterPages - 1), back) }
                }
            ))
        }

        menu.open(player)
    }
}
