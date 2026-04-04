package com.ratger.acreative.menus.itemEdit.pages

import com.google.common.collect.LinkedHashMultimap
import com.ratger.acreative.itemedit.attributes.ItemAttributeMenuSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

class AttributeEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
    private val graySlots = setOf(
        1, 2, 3, 4, 5, 6, 7,
        10, 16,
        19, 25,
        28, 34,
        37, 38, 39, 40, 41, 42, 43
    )
    private val workSlots = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33)

    fun open(player: Player, session: ItemEditSession, page: Int = 0) {
        val entries = ItemAttributeMenuSupport.listEffectiveEntries(session.editableItem)
        val pageSize = workSlots.size
        val totalPages = maxOf(1, (entries.size + pageSize - 1) / pageSize)
        val pageIndex = page.coerceIn(0, totalPages - 1)
        val title = if (totalPages == 1) {
            "<!i>▍ Редактор → Атрибуты"
        } else {
            "<!i>▍ Редактор → Атрибуты [${pageIndex + 1}/$totalPages]"
        }

        val menu = support.buildMenu(
            title = title,
            menuSize = 45,
            rows = ru.violence.coreapi.bukkit.api.menu.MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 26, 39, 41) + workSlots,
            session = session
        )

        val blackFiller = buttonFactory.blackFillerButton()
        val grayFiller = buttonFactory.grayFillerButton()
        blackSlots.forEach { menu.setButton(it, blackFiller) }
        graySlots.forEach { menu.setButton(it, grayFiller) }

        if (pageIndex > 0) {
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { open(player, session, pageIndex - 1) } })
        } else {
            menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        }
        if (pageIndex + 1 < totalPages) {
            menu.setButton(26, buttonFactory.forwardButton { support.transition(session) { open(player, session, pageIndex + 1) } })
        }

        menu.setButton(39, buttonFactory.actionButton(
            material = Material.LIME_DYE,
            name = "<!i><#00FF40>₪ Добавить атрибут",
            lore = emptyList(),
            action = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.ATTRIBUTE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            }
        ))
        menu.setButton(41, buttonFactory.actionButton(
            material = Material.RED_DYE,
            name = "<!i><#FF1500>⚠ Удалить всё",
            lore = emptyList(),
            action = {
                ItemAttributeMenuSupport.writeExplicitAttributes(
                    session.editableItem,
                    LinkedHashMultimap.create<Attribute, AttributeModifier>()
                )
                support.transition(session) {
                    open(player, session, 0)
                }
            }
        ))

        val from = pageIndex * pageSize
        val to = minOf(entries.size, from + pageSize)
        val pageEntries = entries.subList(from, to)

        pageEntries.forEachIndexed { localIndex, entry ->
            val globalIndex = from + localIndex
            val slot = workSlots[localIndex]
            menu.setButton(slot, buildAttributeButton(player, session, pageIndex, globalIndex, entry))
        }

        menu.open(player)
    }

    private fun buildAttributeButton(
        player: Player,
        session: ItemEditSession,
        pageIndex: Int,
        globalIndex: Int,
        entry: ItemAttributeMenuSupport.AttributeEntry
    ): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val attributeDisplayName = ItemAttributeMenuSupport.displayAttributeName(entry.attribute)
        val slotDisplayName = ItemAttributeMenuSupport.displaySlot(entry.modifier.slotGroup)
        val operationDisplayName = ItemAttributeMenuSupport.displayOperation(entry.modifier.operation)
        val value = ItemAttributeMenuSupport.formatAmount(entry.modifier)

        return buttonFactory.actionButton(
            material = Material.PRISMARINE_SHARD,
            name = "<!i><#C7A300>◎ <#FFD700>Атрибут №${globalIndex + 1}",
            lore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
                "",
                "<!i><#FFD700>Параметры:",
                "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>$attributeDisplayName",
                "<!i><#C7A300> ● <#FFE68A>Значение: <#FFF3E0>$value",
                "<!i><#C7A300> ● <#FFE68A>Тип: <#FFF3E0>$operationDisplayName",
                "<!i><#C7A300> ● <#FFE68A>Слот: <#FFF3E0>$slotDisplayName",
                ""
            ),
            action = {
                removeAt(session, globalIndex)
                val after = ItemAttributeMenuSupport.listEffectiveEntries(session.editableItem)
                val totalPages = maxOf(1, (after.size + workSlots.size - 1) / workSlots.size)
                support.transition(session) {
                    open(player, session, pageIndex.coerceAtMost(totalPages - 1))
                }
            }
        )
    }

    private fun removeAt(session: ItemEditSession, globalIndex: Int) {
        val item = session.editableItem
        val explicit = ItemAttributeMenuSupport.currentEffectiveAttributes(item)
        val entries = explicit.entries().toList()
        if (globalIndex !in entries.indices) return
        val pair = entries[globalIndex]
        explicit.remove(pair.key, pair.value)
        ItemAttributeMenuSupport.writeExplicitAttributes(item, explicit)
    }
}
