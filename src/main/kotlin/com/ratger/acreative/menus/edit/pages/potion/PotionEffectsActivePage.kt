package com.ratger.acreative.menus.edit.pages.potion

import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectContextKey
import com.ratger.acreative.menus.edit.pages.common.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class PotionEffectsActivePage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openVisualEffectTypePage: (Player, ItemEditSession, VisualEffectContextKey, Int, (Player, ItemEditSession) -> Unit) -> Unit,
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
                name = "<!i><#00FF40>₪ Добавить эффект <#7BFF00>[Команда]"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.POTION_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            },
            addMenuAction = PagedListPageBuilder.ActionSlot(
                material = Material.MAGENTA_DYE,
                name = "<!i><#FF00FF>₪ Добавить эффект <#FF66FF>[Меню]"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    openVisualEffectTypePage(
                        addPlayer,
                        addSession,
                        VisualEffectContextKey.POTION,
                        0
                    ) { backPlayer, backSession ->
                        open(backPlayer, backSession, pageIndex)
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
    ) = buttonFactory.potionEffectEntryButton(
        index = entry.index + 1,
        displayName = entry.displayName,
        durationLabel = entry.durationLabel,
        level = entry.displayLevel,
        showParticles = entry.showParticles,
        showIcon = entry.showIcon,
        type = entry.effect.type,
        action = action
    )
}
