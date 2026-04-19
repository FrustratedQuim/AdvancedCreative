package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.menus.edit.effects.DeathProtectionMenuSupport
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectContextKey
import com.ratger.acreative.menus.edit.pages.common.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class DeathProtectionRemoveEffectsListPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openVisualEffectTypeOnlyPage: (Player, ItemEditSession, VisualEffectContextKey, Int, (Player, ItemEditSession) -> Unit, Boolean, (ItemEditSession) -> Set<PotionEffectType>, (Player, ItemEditSession, PotionEffectType) -> Unit) -> Unit,
    private val openDeathProtectionRoot: (Player, ItemEditSession) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, page: Int = 0) {
        val entries = DeathProtectionMenuSupport.removedEffects(session.editableItem)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Защита → Снятие эффектов"
                } else {
                    "<!i>▍ Защита → Снятие эффектов [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = ::open,
            backOnFirstPage = openDeathProtectionRoot,
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить эффект <#7BFF00>[Команда]"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.DEATH_PROTECTION_REMOVE_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            },
            addMenuAction = PagedListPageBuilder.ActionSlot(
                material = Material.MAGENTA_DYE,
                name = "<!i><#FF00FF>₪ Добавить эффект <#FF66FF>[Меню]"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    openVisualEffectTypeOnlyPage(
                        addPlayer,
                        addSession,
                        VisualEffectContextKey.DEATH_PROTECTION,
                        0,
                        { backPlayer, backSession -> open(backPlayer, backSession, pageIndex) },
                        true,
                        { selectionSession -> DeathProtectionMenuSupport.removedEffects(selectionSession.editableItem).toSet() }
                    ) { _, selectedSession, selectedType ->
                        support.transition(selectedSession) {
                            val hasEffect = DeathProtectionMenuSupport.removedEffects(selectedSession.editableItem).contains(selectedType)
                            if (hasEffect) {
                                DeathProtectionMenuSupport.removeRemovedEffect(selectedSession.editableItem, selectedType)
                            } else {
                                DeathProtectionMenuSupport.addRemovedEffect(selectedSession.editableItem, selectedType)
                            }
                        }
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                DeathProtectionMenuSupport.clearRemovedEffects(clearSession.editableItem)
                support.transition(clearSession) { open(clearPlayer, clearSession, 0) }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, _, entry ->
                buildEntryButton(entry) {
                    DeathProtectionMenuSupport.removeRemovedEffect(entrySession.editableItem, entry)
                    val afterSize = DeathProtectionMenuSupport.removedEffects(entrySession.editableItem).size
                    val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                    support.transition(entrySession) { open(entryPlayer, entrySession, targetPage) }
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
