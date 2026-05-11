package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.common.PagedSelectionLayout
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectContextKey
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectFlowService
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectIconResolver
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class VisualEffectTypeSelectPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val flowService: VisualEffectFlowService
) {
    private val preferredOrder = listOf(
        "speed", "slowness", "jump_boost", "slow_falling",
        "night_vision", "darkness", "invisibility", "glowing", "blindness", "nausea",
        "water_breathing", "fire_resistance", "resistance",
        "haste", "mining_fatigue",
        "strength", "weakness",
        "regeneration", "instant_health", "health_boost", "absorption",
        "instant_damage", "poison", "wither",
        "saturation", "hunger",
        "luck", "unluck",
        "conduit_power", "dolphins_grace",
        "wind_charged", "weaving", "oozing", "infested",
        "bad_omen", "raid_omen", "trial_omen", "hero_of_the_village"
    )
    private val orderIndexByKey = preferredOrder.withIndex().associate { it.value to it.index }
    private val orderedTypes: List<PotionEffectType> = Registry.MOB_EFFECT.iterator().asSequence()
        .sortedWith(compareBy<PotionEffectType>(
            { orderIndexByKey[it.key.key] ?: Int.MAX_VALUE },
            { it.key.key }
        ))
        .toList()

    fun open(
        player: Player,
        session: ItemEditSession,
        contextKey: VisualEffectContextKey,
        page: Int,
        openParent: (Player, ItemEditSession) -> Unit,
        openParams: (Player, ItemEditSession) -> Unit,
        multiSelect: Boolean = false,
        selectedTypesProvider: (ItemEditSession) -> Set<PotionEffectType> = { emptySet() },
        onTypeSelected: ((Player, ItemEditSession, PotionEffectType) -> Unit)? = null
    ) {
        flowService.begin(session, contextKey)
        val totalPages = maxOf(1, (orderedTypes.size + PagedSelectionLayout.workSlots.size - 1) / PagedSelectionLayout.workSlots.size)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        session.visualEffectLastTypePage = pageIndex

        val from = pageIndex * PagedSelectionLayout.workSlots.size
        val to = minOf(orderedTypes.size, from + PagedSelectionLayout.workSlots.size)
        val pageEntries = orderedTypes.subList(from, to)

        val menu = support.buildMenu(
            title = "<!i>▍ Эффект → Тип [${pageIndex + 1}/$totalPages]",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(PagedSelectionLayout.BACK_SLOT, PagedSelectionLayout.FORWARD_SLOT) + PagedSelectionLayout.workSlots,
            session = session
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        PagedSelectionLayout.blackSlots.forEach { menu.setButton(it, black) }
        PagedSelectionLayout.graySlots.forEach { menu.setButton(it, gray) }
        val selectedType = flowService.resolveType(session.visualEffectDraft.effectTypeKey)
        val selectedTypesSet = if (multiSelect) selectedTypesProvider(session) else emptySet()

        menu.setButton(PagedSelectionLayout.BACK_SLOT, buttonFactory.backButton("◀ Назад") {
            support.transition(session) {
                if (pageIndex > 0) open(player, session, contextKey, pageIndex - 1, openParent, openParams, multiSelect, selectedTypesProvider, onTypeSelected)
                else openParent(player, session)
            }
        })

        val hideForwardOnLastTypeOnlyPage = onTypeSelected != null && pageIndex + 1 >= totalPages
        if (!hideForwardOnLastTypeOnlyPage) {
            menu.setButton(PagedSelectionLayout.FORWARD_SLOT, buttonFactory.forwardButton("Вперёд ▶") {
                support.transition(session) {
                    if (pageIndex + 1 < totalPages) {
                        open(player, session, contextKey, pageIndex + 1, openParent, openParams, multiSelect, selectedTypesProvider, onTypeSelected)
                    } else {
                        if (onTypeSelected != null) {
                            if (selectedType != null) {
                                onTypeSelected(player, session, selectedType)
                            } else {
                                openParent(player, session)
                            }
                        } else {
                            openParams(player, session)
                        }
                    }
                }
            })
        }

        pageEntries.forEachIndexed { index, type ->
            val slot = PagedSelectionLayout.workSlots[index]
            val isSelected = if (multiSelect) selectedTypesSet.contains(type) else selectedType == type
            menu.setButton(slot, buttonFactory.visualEffectTypeEntryButton(
                displayName = PotionItemSupport.displayName(type),
                modelId = VisualEffectIconResolver.resolve(type).key.asString(),
                selected = isSelected
            ) {
                if (!multiSelect && isSelected) {
                    session.visualEffectDraft = session.visualEffectDraft.copy(effectTypeKey = null)
                    support.transition(session) { open(player, session, contextKey, pageIndex, openParent, openParams, multiSelect, selectedTypesProvider, onTypeSelected) }
                    return@visualEffectTypeEntryButton
                }
                session.visualEffectDraft = session.visualEffectDraft.copy(effectTypeKey = type.key.key)
                support.transition(session) {
                    if (onTypeSelected != null) {
                        onTypeSelected(player, session, type)
                        if (multiSelect) {
                            open(player, session, contextKey, pageIndex, openParent, openParams, multiSelect, selectedTypesProvider, onTypeSelected)
                        }
                    } else {
                        openParams(player, session)
                    }
                }
            })
        }

        menu.open(player)
    }
}
