package com.ratger.acreative.menus.edit.pages.enchantments

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.enchant.EnchantmentIconResolver
import com.ratger.acreative.menus.edit.enchant.EnchantmentMenuFlowService
import com.ratger.acreative.menus.edit.enchant.EnchantmentSupport
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class EnchantmentParametersPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val flowService: EnchantmentMenuFlowService,
    private val requestSignInput: (Player, Array<String>, (Player, String?) -> Unit, (Player) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 1, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 42, 43, 44)

    fun open(
        player: Player,
        session: ItemEditSession,
        openParent: (Player, ItemEditSession) -> Unit,
        openTypePage: (Player, ItemEditSession, Int) -> Unit
    ) {
        flowService.begin(session)
        val selected = flowService.resolveSelected(session)

        val menu = support.buildMenu(
            title = "<!i>▍ Зачарования → Параметры",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 22, 40),
            session = session
        )
        support.fillBase(menu, 45, blackSlots)

        if (selected != null) {
            menu.setButton(4, buttonFactory.enchantmentSelectedPreviewButton(
                displayName = EnchantmentSupport.displayName(selected),
                modelId = EnchantmentIconResolver.resolve(selected).key.asString()
            ))
        }

        menu.setButton(18, buttonFactory.backButton("◀ Назад") {
            support.transition(session) {
                openTypePage(player, session, session.enchantmentDraftLastTypePage)
            }
        })

        menu.setButton(22, buttonFactory.enchantmentLevelButton(session.enchantmentDraftLevel) {
            support.transition(session) {
                player.closeInventory()
                requestSignInput(
                    player,
                    arrayOf("", "↑ Уровень ↑", "", ""),
                    { submitPlayer: Player, input: String? ->
                        val parsed = input?.trim()?.toIntOrNull()?.coerceIn(1, 127)
                        if (parsed != null) {
                            flowService.setLevel(session, parsed)
                        }
                        open(submitPlayer, session, openParent, openTypePage)
                    },
                    { leavePlayer: Player -> open(leavePlayer, session, openParent, openTypePage) }
                )
            }
        })

        menu.setButton(40, buttonFactory.enchantmentConfirmCreateButton {
            if (!flowService.apply(session)) {
                session.enchantmentDraftLastTypePage = 0
                support.transition(session) { openTypePage(player, session, 0) }
                return@enchantmentConfirmCreateButton
            }
            flowService.reset(session)
            support.transition(session) { openParent(player, session) }
        })

        menu.open(player)
    }
}
