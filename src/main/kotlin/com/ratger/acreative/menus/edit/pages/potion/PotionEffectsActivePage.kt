package com.ratger.acreative.menus.edit.pages.potion

import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.pages.common.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.PotionMeta
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class PotionEffectsActivePage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openPotionRoot: (Player, ItemEditSession) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, page: Int = 0) {
        val entries = PotionItemSupport.effectEntries(session.editableItem)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Зелье → Эффекты"
                } else {
                    "<!i>▍ Зелье → Эффекты [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = ::open,
            backOnFirstPage = openPotionRoot,
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить эффект"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.POTION_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                PotionItemSupport.clearEffects(clearSession.editableItem)
                support.transition(clearSession) { open(clearPlayer, clearSession, 0) }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, _, entry ->
                buildPotionEffectEntryButton(entry) {
                    PotionItemSupport.removeEffect(entrySession.editableItem, entry.effect.type)
                    val afterSize = PotionItemSupport.effectEntries(entrySession.editableItem).size
                    val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                    support.transition(entrySession) { open(entryPlayer, entrySession, targetPage) }
                }
            }
        )
    }

    private fun buildPotionEffectEntryButton(
        entry: PotionItemSupport.PotionEffectEntry,
        action: (ClickEvent) -> Unit
    ) = buttonFactory.actionButton(
        material = Material.POTION,
        name = "<!i><#C7A300>◎ <#FFD700>Эффект №${entry.index + 1}",
        lore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
            "",
            "<!i><#FFD700>Параметры:",
            "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>${entry.displayName} ",
            "<!i><#C7A300> ● <#FFE68A>Длительность: <#FFF3E0>${entry.seconds} ",
            "<!i><#C7A300> ● <#FFE68A>Уровень: <#FFF3E0>${entry.displayLevel} ",
            "<!i><#C7A300> ● <#FFE68A>Видны партиклы: ${if (entry.showParticles) "<#00FF40>Да" else "<#FF1500>Нет"}",
            "<!i><#C7A300> ● <#FFE68A>Иконка в углу: ${if (entry.showIcon) "<#00FF40>Да" else "<#FF1500>Нет"}",
            ""
        ),
        itemModifier = {
            val previewPotionType = PotionItemSupport.previewPotionType(entry.effect.type)
            if (previewPotionType != null) {
                edit { item ->
                    val meta = item.itemMeta as? PotionMeta ?: return@edit
                    meta.basePotionType = previewPotionType
                    meta.addCustomEffect(entry.effect, true)
                    item.itemMeta = meta
                }
            }
            flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            this
        },
        action = action
    )
}
