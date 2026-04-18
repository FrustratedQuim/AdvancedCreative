package com.ratger.acreative.menus.edit.pages.effects

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectFlowService
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectIconResolver
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectInputSupport
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class VisualEffectParametersPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val flowService: VisualEffectFlowService,
    private val requestSignInput: (Player, Array<String>, (Player, String?) -> Unit, (Player) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 1, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 42, 43, 44)

    fun open(
        player: Player,
        session: ItemEditSession,
        openParent: (Player, ItemEditSession) -> Unit,
        openTypePage: (Player, ItemEditSession, Int) -> Unit
    ) {
        val context = flowService.context(session) ?: run {
            support.transition(session) { openParent(player, session) }
            return
        }

        val menu = support.buildMenu(
            title = "<!i>▍ Эффект → Параметры",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 20, 21, 22, 23, 24, 40),
            session = session
        )
        support.fillBase(menu, 45, blackSlots)

        val draft = session.visualEffectDraft
        val selectedType = flowService.resolveType(draft.effectTypeKey)

        if (selectedType != null) {
            menu.setButton(4, buttonFactory.visualEffectSelectedPreviewButton(
                displayName = PotionItemSupport.displayName(selectedType),
                material = VisualEffectIconResolver.resolve(selectedType)
            ))
        }

        menu.setButton(18, buttonFactory.backButton("◀ Назад") {
            support.transition(session) { openTypePage(player, session, session.visualEffectLastTypePage) }
        })

        menu.setButton(20, buttonFactory.visualEffectLevelButton(draft.level) {
            support.transition(session) {
                player.closeInventory()
                requestSignInput(
                    player,
                    arrayOf("", "↑ Уровень ↑", "", ""),
                    { submitPlayer: Player, input: String? ->
                        val parsed = VisualEffectInputSupport.parseLevel(input)
                        if (parsed != null) {
                            session.visualEffectDraft = session.visualEffectDraft.copy(level = parsed)
                        }
                        open(submitPlayer, session, openParent, openTypePage)
                    },
                    { leavePlayer: Player -> open(leavePlayer, session, openParent, openTypePage) }
                )
            }
        })
        menu.setButton(21, buttonFactory.visualEffectDurationButton(draft.durationSeconds) {
            support.transition(session) {
                player.closeInventory()
                requestSignInput(
                    player,
                    arrayOf("", "↑ Время ↑", "", ""),
                    { submitPlayer: Player, input: String? ->
                        val parsed = VisualEffectInputSupport.parseDurationSeconds(input)
                        if (parsed != null) {
                            session.visualEffectDraft = session.visualEffectDraft.copy(durationSeconds = parsed)
                        }
                        open(submitPlayer, session, openParent, openTypePage)
                    },
                    { leavePlayer: Player -> open(leavePlayer, session, openParent, openTypePage) }
                )
            }
        })
        if (context.supportsProbability) {
            menu.setButton(22, buttonFactory.visualEffectProbabilityButton(draft.probabilityPercent) {
                support.transition(session) {
                    player.closeInventory()
                    requestSignInput(
                        player,
                        arrayOf("", "↑ Шанс ↑", "", ""),
                        { submitPlayer: Player, input: String? ->
                            val parsed = VisualEffectInputSupport.parseProbabilityPercent(input)
                            if (parsed != null) {
                                session.visualEffectDraft = session.visualEffectDraft.copy(probabilityPercent = parsed)
                            }
                            open(submitPlayer, session, openParent, openTypePage)
                        },
                        { leavePlayer: Player -> open(leavePlayer, session, openParent, openTypePage) }
                    )
                }
            })
        }
        menu.setButton(23, buttonFactory.visualEffectParticlesToggleButton(draft.showParticles) {
            session.visualEffectDraft = session.visualEffectDraft.copy(showParticles = !session.visualEffectDraft.showParticles)
            support.transition(session) { open(player, session, openParent, openTypePage) }
        })
        menu.setButton(24, buttonFactory.visualEffectIconToggleButton(draft.showIcon) {
            session.visualEffectDraft = session.visualEffectDraft.copy(showIcon = !session.visualEffectDraft.showIcon)
            support.transition(session) { open(player, session, openParent, openTypePage) }
        })

        menu.setButton(40, buttonFactory.visualEffectConfirmButton {
            if (session.visualEffectDraft.effectTypeKey == null) {
                session.visualEffectLastTypePage = 0
                support.transition(session) { openTypePage(player, session, 0) }
                return@visualEffectConfirmButton
            }
            val success = flowService.apply(player, session)
            if (!success) {
                support.transition(session) { openTypePage(player, session, 0) }
                return@visualEffectConfirmButton
            }
            flowService.reset(session)
            support.transition(session) { openParent(player, session) }
        })

        menu.open(player)
    }
}
