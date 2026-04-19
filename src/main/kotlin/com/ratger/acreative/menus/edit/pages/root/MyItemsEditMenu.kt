package com.ratger.acreative.menus.edit.pages.root

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.personal.PersonalItemsService
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class MyItemsEditMenu(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val personalItemsService: PersonalItemsService,
    private val openRoot: (Player, ItemEditSession) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
    private val graySlots = (1..7).toSet() + (37..43).toSet()
    private val workingSlots = (10..16).toSet() + (19..25).toSet() + (28..34).toSet()
    private val sortedWorkingSlots = workingSlots.sorted()

    fun open(player: Player, session: ItemEditSession) {
        openInternal(player, session, showBackButton = true)
    }

    fun openStandalone(player: Player) {
        openInternal(player, session = null, showBackButton = false)
    }

    private fun openInternal(player: Player, session: ItemEditSession?, showBackButton: Boolean) {
        val entries = personalItemsService.list(player.uniqueId)
        val interactiveSlots = mutableSetOf<Int>().apply {
            if (showBackButton) {
                add(18)
            }
            addAll(sortedWorkingSlots.take(entries.size))
        }

        val menu = if (session != null) {
            support.buildMenu(
                title = "<!i>▍ Редактор → Мои предметы",
                menuSize = 45,
                rows = MenuRows.FIVE,
                interactiveTopSlots = interactiveSlots,
                session = session,
                allowPlayerInventoryClicks = true,
                blockShiftClickFromPlayerInventory = true
            )
        } else {
            support.buildStandaloneMenu(
                title = "<!i>▍ Редактор → Мои предметы",
                menuSize = 45,
                rows = MenuRows.FIVE,
                interactiveTopSlots = interactiveSlots,
                allowPlayerInventoryClicks = true,
                blockShiftClickFromPlayerInventory = true,
                onClose = { event ->
                    personalItemsService.commitDeferredPromotions(event.player.uniqueId)
                }
            )
        }

        for (slot in blackSlots) {
            menu.setButton(slot, buttonFactory.blackFillerButton())
        }
        for (slot in graySlots) {
            menu.setButton(slot, buttonFactory.grayFillerButton())
        }
        if (showBackButton && session != null) {
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        }
        menu.setButton(
            26,
            buttonFactory.actionButton(
                material = Material.FIRE_CHARGE,
                name = "<!i><#FFD700>ℹ Важно!",
                lore = listOf(
                    "",
                    "<!i><#FFD700> ◆ <#FFE68A>Здесь хранятся последние",
                    "<!i><#FFE68A><b>  </b> редактируемые вами <#FFD700>предметы.",
                    "",
                    "<!i><#FFD700> ◆ <#FFE68A>Новые предметы <#FFD700>автоматически",
                    "<!i><#FFE68A><b>  </b> заменяют старые.",
                    "",
                    "<!i><#FFD700> ◆ <#FFE68A>Если предмет не был взят <#FFD700>ни разу<#FFE68A> в ",
                    "<!i><#FFE68A><b>  </b> <#FFD700>течение<#FFE68A> недели, то он будет удалён ",
                    "<!i><#FFE68A><b>  </b> из этого меню.",
                    ""
                )
            )
        )

        sortedWorkingSlots.forEachIndexed { index, slot ->
            val item = entries.getOrNull(index) ?: return@forEachIndexed
            menu.setButton(slot, buttonFactory.itemAsIsButton(item) { event ->
                personalItemsService.giveFromMenu(player, item, event)
            })
        }

        menu.open(player)
    }
}
