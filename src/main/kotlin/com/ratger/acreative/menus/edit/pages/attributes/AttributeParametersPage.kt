package com.ratger.acreative.menus.edit.pages.attributes

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.attributes.AttributeIconResolver
import com.ratger.acreative.menus.edit.attributes.AttributeMenuFlowService
import com.ratger.acreative.menus.edit.attributes.ItemAttributeMenuSupport
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class AttributeParametersPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val flowService: AttributeMenuFlowService,
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
            title = "<!i>▍ Атрибуты → Параметры",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 20, 22, 24, 40),
            session = session
        )
        support.fillBase(menu, 45, blackSlots)

        if (selected != null) {
            menu.setButton(
                4,
                buttonFactory.attributeSelectedPreviewButton(
                    displayName = ItemAttributeMenuSupport.displayAttributeName(selected),
                    modelId = AttributeIconResolver.resolve(selected).key.asString()
                )
            )
        }

        menu.setButton(18, buttonFactory.backButton("◀ Назад") {
            support.transition(session) { openTypePage(player, session, session.attributeDraftLastTypePage) }
        })

        menu.setButton(20, buttonFactory.attributeValueButton(session.attributeDraftAmount) {
            support.transition(session) {
                player.closeInventory()
                requestSignInput(
                    player,
                    arrayOf("", "↑ Значение ↑", "", ""),
                    { submitPlayer: Player, input: String? ->
                        flowService.setAmount(session, input)
                        open(submitPlayer, session, openParent, openTypePage)
                    },
                    { leavePlayer: Player ->
                        open(leavePlayer, session, openParent, openTypePage)
                    }
                )
            }
        })

        val operationOptions = flowService.operationOptions().map {
            MenuButtonFactory.ListButtonOption(value = it.value, label = it.label)
        }
        menu.setButton(
            22,
            buttonFactory.listButton(
                material = org.bukkit.Material.BLAZE_POWDER,
                options = operationOptions,
                selectedIndex = flowService.operationIndex(session),
                titleBuilder = { _, _ -> "<!i><#C7A300>◎ <#FFD700>Добавление значения" },
                beforeOptionsLore = listOf(
                    "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                    ""
                ),
                afterOptionsLore = listOf("")
            ) { _, newIndex ->
                flowService.setOperationByIndex(session, newIndex)
                support.transition(session) { open(player, session, openParent, openTypePage) }
            }
        )

        val slotOptions = flowService.slotOptions().map {
            MenuButtonFactory.ListButtonOption(value = it.value, label = it.label)
        }
        menu.setButton(
            24,
            buttonFactory.listButton(
                material = org.bukkit.Material.IRON_CHESTPLATE,
                options = slotOptions,
                selectedIndex = flowService.slotIndex(session),
                titleBuilder = { _, _ -> "<!i><#C7A300>◎ <#FFD700>Слот активации" },
                beforeOptionsLore = listOf(
                    "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                    ""
                ),
                afterOptionsLore = listOf("")
            ) { _, newIndex ->
                flowService.setSlotByIndex(session, newIndex)
                support.transition(session) { open(player, session, openParent, openTypePage) }
            }
        )

        menu.setButton(40, buttonFactory.attributeConfirmCreateButton {
            if (!flowService.apply(session)) {
                session.attributeDraftLastTypePage = 0
                support.transition(session) { openTypePage(player, session, 0) }
                return@attributeConfirmCreateButton
            }
            flowService.reset(session)
            support.transition(session) { openParent(player, session) }
        })

        menu.open(player)
    }
}

