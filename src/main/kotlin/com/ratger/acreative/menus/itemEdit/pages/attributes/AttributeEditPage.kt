package com.ratger.acreative.menus.itemEdit.pages.attributes

import com.google.common.collect.LinkedHashMultimap
import com.ratger.acreative.itemedit.attributes.ItemAttributeMenuSupport
import com.ratger.acreative.itemedit.attributes.SlotGroupAdapter
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.common.PagedListPageBuilder
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.button.Button

class AttributeEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val listBuilder = PagedListPageBuilder(support, buttonFactory)

    fun open(player: Player, session: ItemEditSession, page: Int = 0) {
        val entries = ItemAttributeMenuSupport.listEffectiveEntries(session.editableItem)
        listBuilder.open(
            player = player,
            session = session,
            page = page,
            entries = entries,
            title = { window ->
                if (window.totalPages == 1) {
                    "<!i>▍ Редактор → Атрибуты"
                } else {
                    "<!i>▍ Редактор → Атрибуты [${window.pageIndex + 1}/${window.totalPages}]"
                }
            },
            openPage = ::open,
            backOnFirstPage = openAdvancedPageTwo,
            addAction = PagedListPageBuilder.ActionSlot(
                material = Material.LIME_DYE,
                name = "<!i><#00FF40>₪ Добавить атрибут"
            ) { addPlayer, addSession, pageIndex ->
                support.transition(addSession) {
                    requestApplyInput(addPlayer, addSession, EditorApplyKind.ATTRIBUTE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, pageIndex)
                    }
                }
            },
            clearAction = PagedListPageBuilder.ActionSlot(
                material = Material.RED_DYE,
                name = "<!i><#FF1500>⚠ Удалить всё"
            ) { clearPlayer, clearSession, _ ->
                ItemAttributeMenuSupport.writeExplicitAttributes(
                    clearSession.editableItem,
                    LinkedHashMultimap.create<Attribute, AttributeModifier>()
                )
                support.transition(clearSession) {
                    open(clearPlayer, clearSession, 0)
                }
            },
            entryButton = { entryPlayer, entrySession, pageWindow, globalIndex, entry ->
                buildAttributeButton(entryPlayer, entrySession, pageWindow.pageIndex, globalIndex, entry)
            }
        )
    }

    private fun buildAttributeButton(
        player: Player,
        session: ItemEditSession,
        pageIndex: Int,
        globalIndex: Int,
        entry: ItemAttributeMenuSupport.AttributeEntry
    ): Button {
        val attributeDisplayName = ItemAttributeMenuSupport.displayAttributeName(entry.attribute)
        val slotDisplayName = SlotGroupAdapter.displayName(entry.modifier)
        val operationDisplayName = ItemAttributeMenuSupport.displayOperation(entry.modifier.operation)
        val value = ItemAttributeMenuSupport.formatAmount(entry.modifier)

        return buttonFactory.actionButton(
            material = Material.PRISMARINE_SHARD,
            name = "<!i><#C7A300>◎ <#FFD700>Атрибут №${globalIndex + 1}",
            lore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
                "",
                "<!i><#FFD700>Параметры:",
                "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>$attributeDisplayName ",
                "<!i><#C7A300> ● <#FFE68A>Значение: <#FFF3E0>$value ",
                "<!i><#C7A300> ● <#FFE68A>Тип: <#FFF3E0>$operationDisplayName ",
                "<!i><#C7A300> ● <#FFE68A>Слот: <#FFF3E0>$slotDisplayName ",
                ""
            ),
            action = {
                removeAt(session, globalIndex)
                val afterSize = ItemAttributeMenuSupport.listEffectiveEntries(session.editableItem).size
                val targetPage = listBuilder.coercePageIndexAfterUpdate(pageIndex, afterSize)
                support.transition(session) {
                    open(player, session, targetPage)
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
