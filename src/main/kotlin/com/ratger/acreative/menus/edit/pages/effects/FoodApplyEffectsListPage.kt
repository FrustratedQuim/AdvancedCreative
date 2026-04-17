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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

class FoodApplyEffectsListPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openFoodRoot: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int = 0) {
        val entries = ConsumableComponentSupport.applyEffectEntries(session.editableItem)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Еда → Наложение эффектов"
                } else {
                    "<!i>▍ Еда → Наложение эффектов [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = { pagePlayer, pageSession, pageIndex -> open(pagePlayer, pageSession, openBack, pageIndex) },
            backOnFirstPage = { backPlayer, backSession -> openFoodRoot(backPlayer, backSession, openBack) },
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить эффект"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.CONSUMABLE_APPLY_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack, pageIndex)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                ConsumableComponentSupport.clearApplyEffects(clearSession.editableItem)
                support.transition(clearSession) { open(clearPlayer, clearSession, openBack, 0) }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, _, entry ->
                buttonFactory.potionApplyEffectEntryButton(
                    index = entry.index + 1,
                    displayName = PotionItemSupport.displayName(entry.effect.type),
                    durationLabel = PotionItemSupport.durationLabel(entry.effect.duration),
                    level = entry.effect.amplifier + 1,
                    chancePercent = formatChancePercent(entry.probability),
                    showParticles = entry.effect.showParticles,
                    showIcon = entry.effect.showIcon,
                    type = entry.effect.type
                ) {
                    ConsumableComponentSupport.removeApplyEffect(entrySession.editableItem, entry.index)
                    val afterSize = ConsumableComponentSupport.applyEffectEntries(entrySession.editableItem).size
                    val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                    support.transition(entrySession) { open(entryPlayer, entrySession, openBack, targetPage) }
                }
            }
        )
    }

    private fun formatChancePercent(probability: Float): String {
        val percent = probability * 100f
        val rounded = round(percent)
        return if (abs(percent - rounded) < 0.0001f) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", percent)
        }
    }
}
