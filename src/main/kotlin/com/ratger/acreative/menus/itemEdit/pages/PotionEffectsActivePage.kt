package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class PotionEffectsActivePage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openPotionRoot: (Player, ItemEditSession) -> Unit
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

    fun open(player: Player, session: ItemEditSession, page: Int = 0) {
        val entries = PotionItemSupport.effectEntries(session.editableItem)
        val pageSize = workSlots.size
        val totalPages = maxOf(1, (entries.size + pageSize - 1) / pageSize)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        val title = if (totalPages == 1) {
            "<!i>▍ Зелье → Эффекты"
        } else {
            "<!i>▍ Зелье → Эффекты [${pageIndex + 1}/$totalPages]"
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
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { open(player, session, pageIndex - 1) } })
        } else {
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { openPotionRoot(player, session) } })
        }

        if (pageIndex + 1 < totalPages) {
            menu.setButton(26, buttonFactory.forwardButton { support.transition(session) { open(player, session, pageIndex + 1) } })
        }

        menu.setButton(39, buttonFactory.actionButton(
            material = Material.LIME_DYE,
            name = "<!i><#00FF40>₪ Добавить эффект",
            lore = emptyList(),
            action = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.POTION_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            }
        ))

        menu.setButton(41, buttonFactory.actionButton(
            material = Material.RED_DYE,
            name = "<!i><#FF1500>⚠ Удалить всё",
            lore = emptyList(),
            action = {
                PotionItemSupport.clearEffects(session.editableItem)
                support.transition(session) { open(player, session, 0) }
            }
        ))

        val from = pageIndex * pageSize
        val to = minOf(entries.size, from + pageSize)
        val pageEntries = entries.subList(from, to)

        pageEntries.forEachIndexed { localIndex, entry ->
            val slot = workSlots[localIndex]
            menu.setButton(slot, buttonFactory.potionEffectEntryButton(entry) {
                PotionItemSupport.removeEffect(session.editableItem, entry.effect.type)
                val after = PotionItemSupport.effectEntries(session.editableItem)
                val afterPages = maxOf(1, (after.size + workSlots.size - 1) / workSlots.size)
                support.transition(session) { open(player, session, pageIndex.coerceAtMost(afterPages - 1)) }
            })
        }

        menu.open(player)
    }
}
