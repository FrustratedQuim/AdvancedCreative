package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.menus.edit.effects.DeathProtectionMenuSupport
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

class DeathProtectionApplyEffectsListPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openDeathProtectionRoot: (Player, ItemEditSession) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, page: Int = 0) {
        val entries = DeathProtectionMenuSupport.applyEffectEntries(session.editableItem)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Защита → Наложение эффектов"
                } else {
                    "<!i>▍ Защита → Наложение эффектов [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = ::open,
            backOnFirstPage = openDeathProtectionRoot,
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить эффект"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.DEATH_PROTECTION_APPLY_EFFECT_ADD) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                DeathProtectionMenuSupport.clearApplyEffects(clearSession.editableItem)
                support.transition(clearSession) { open(clearPlayer, clearSession, 0) }
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
                    DeathProtectionMenuSupport.removeApplyEffect(entrySession.editableItem, entry.index)
                    val afterSize = DeathProtectionMenuSupport.applyEffectEntries(entrySession.editableItem).size
                    val targetPage = listBuilder.coercePageIndexAfterUpdate(pageWindow.pageIndex, afterSize)
                    support.transition(entrySession) { open(entryPlayer, entrySession, targetPage) }
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
