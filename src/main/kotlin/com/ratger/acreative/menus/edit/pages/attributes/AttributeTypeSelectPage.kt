package com.ratger.acreative.menus.edit.pages.attributes

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.attributes.AttributeIconResolver
import com.ratger.acreative.menus.edit.attributes.AttributeMenuFlowService
import com.ratger.acreative.menus.edit.attributes.ItemAttributeMenuSupport
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class AttributeTypeSelectPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val flowService: AttributeMenuFlowService
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

    fun open(
        player: Player,
        session: ItemEditSession,
        page: Int,
        openParent: (Player, ItemEditSession) -> Unit,
        openParams: (Player, ItemEditSession) -> Unit
    ) {
        flowService.begin(session)

        val allAttributes = ItemAttributeMenuSupport.orderedAttributesForMenu()
        val totalPages = maxOf(1, (allAttributes.size + workSlots.size - 1) / workSlots.size)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        session.attributeDraftLastTypePage = pageIndex

        val from = pageIndex * workSlots.size
        val to = minOf(allAttributes.size, from + workSlots.size)
        val pageEntries = allAttributes.subList(from, to)
        val selected = flowService.resolveSelected(session)

        val menu = support.buildMenu(
            title = "<!i>▍ Атрибуты → Тип [${pageIndex + 1}/$totalPages]",
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
                if (pageIndex > 0) open(player, session, pageIndex - 1, openParent, openParams)
                else openParent(player, session)
            }
        })

        menu.setButton(26, buttonFactory.forwardButton("Вперёд ▶") {
            support.transition(session) {
                if (pageIndex + 1 < totalPages) open(player, session, pageIndex + 1, openParent, openParams)
                else openParams(player, session)
            }
        })

        pageEntries.forEachIndexed { index, attribute ->
            val slot = workSlots[index]
            val isSelected = attribute == selected
            menu.setButton(
                slot,
                buttonFactory.attributeTypeEntryButton(
                    displayName = ItemAttributeMenuSupport.displayAttributeName(attribute),
                    modelId = AttributeIconResolver.resolve(attribute).key.asString(),
                    selected = isSelected
                ) {
                    flowService.setSelected(session, if (isSelected) null else attribute)
                    support.transition(session) { openParams(player, session) }
                }
            )
        }

        menu.open(player)
    }
}

