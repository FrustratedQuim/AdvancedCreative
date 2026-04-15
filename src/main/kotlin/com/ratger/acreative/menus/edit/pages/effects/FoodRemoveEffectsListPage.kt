package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.menus.edit.effects.ConsumableComponentSupport
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.pages.common.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class FoodRemoveEffectsListPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openFoodRoot: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int = 0) {
        val entries = ConsumableComponentSupport.removedEffects(session.editableItem)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Еда → Снятие эффектов"
                } else {
                    "<!i>▍ Еда → Снятие эффектов [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = { pagePlayer, pageSession, pageIndex -> open(pagePlayer, pageSession, openBack, pageIndex) },
            backOnFirstPage = { backPlayer, backSession -> openFoodRoot(backPlayer, backSession, openBack) },
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить эффект"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.CONSUMABLE_REMOVE_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack, pageIndex)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                ConsumableComponentSupport.clearRemovedEffects(clearSession.editableItem)
                support.transition(clearSession) { open(clearPlayer, clearSession, openBack, 0) }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, _, entry ->
                buildEntryButton(entry) {
                    ConsumableComponentSupport.removeRemovedEffect(entrySession.editableItem, entry)
                    val afterSize = ConsumableComponentSupport.removedEffects(entrySession.editableItem).size
                    val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                    support.transition(entrySession) { open(entryPlayer, entrySession, openBack, targetPage) }
                }
            }
        )
    }

    private fun buildEntryButton(
        entry: PotionEffectType,
        action: (ClickEvent) -> Unit
    ) = buttonFactory.potionRemoveEffectEntryButton(
        type = entry,
        displayName = PotionItemSupport.displayName(entry),
        action = action
    )
}
