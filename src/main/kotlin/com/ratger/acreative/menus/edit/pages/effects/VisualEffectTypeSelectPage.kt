package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
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
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
    private val graySlots = setOf(
        1, 2, 3, 4, 5, 6, 7,
        37, 38, 39, 40, 41, 42, 43
    )
    private val workSlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    )

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
        openParams: (Player, ItemEditSession) -> Unit
    ) {
        flowService.begin(session, contextKey)
        val totalPages = maxOf(1, (orderedTypes.size + workSlots.size - 1) / workSlots.size)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        session.visualEffectLastTypePage = pageIndex

        val from = pageIndex * workSlots.size
        val to = minOf(orderedTypes.size, from + workSlots.size)
        val pageEntries = orderedTypes.subList(from, to)

        val menu = support.buildMenu(
            title = "<!i>▍ Эффект → Тип [${pageIndex + 1}/$totalPages]",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 26) + workSlots,
            session = session
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        blackSlots.forEach { menu.setButton(it, black) }
        graySlots.forEach { menu.setButton(it, gray) }

        menu.setButton(18, buttonFactory.backButton("◀ Назад") {
            support.transition(session) {
                if (pageIndex > 0) open(player, session, contextKey, pageIndex - 1, openParent, openParams)
                else openParent(player, session)
            }
        })

        menu.setButton(26, buttonFactory.forwardButton("Вперёд ▶") {
            support.transition(session) {
                if (pageIndex + 1 < totalPages) {
                    open(player, session, contextKey, pageIndex + 1, openParent, openParams)
                } else {
                    openParams(player, session)
                }
            }
        })

        val selectedType = flowService.resolveType(session.visualEffectDraft.effectTypeKey)
        pageEntries.forEachIndexed { index, type ->
            val slot = workSlots[index]
            val isSelected = selectedType == type
            menu.setButton(slot, buttonFactory.visualEffectTypeEntryButton(
                displayName = PotionItemSupport.displayName(type),
                modelId = VisualEffectIconResolver.resolve(type).key.asString(),
                selected = isSelected
            ) {
                if (isSelected) {
                    session.visualEffectDraft = session.visualEffectDraft.copy(effectTypeKey = null)
                    support.transition(session) { open(player, session, contextKey, pageIndex, openParent, openParams) }
                    return@visualEffectTypeEntryButton
                }
                session.visualEffectDraft = session.visualEffectDraft.copy(effectTypeKey = type.key.key)
                support.transition(session) { openParams(player, session) }
            })
        }

        menu.open(player)
    }
}
