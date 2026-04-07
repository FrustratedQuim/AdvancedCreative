package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.attributes.ItemAttributeMenuSupport
import com.ratger.acreative.itemedit.attributes.SlotGroupAdapter
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemRarity
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class AdvancedEditPageTwo(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageOne: (Player, ItemEditSession) -> Unit,
    private val openAttributePage: (Player, ItemEditSession) -> Unit,
    private val openEquippablePage: (Player, ItemEditSession) -> Unit,
    private val openToolPage: (Player, ItemEditSession) -> Unit,
    private val openEnchantmentsPage: (Player, ItemEditSession) -> Unit,
    private val openUseRemainderPage: (Player, ItemEditSession) -> Unit
) {
    private fun updateEditablePreview(menu: ru.violence.coreapi.bukkit.api.menu.Menu, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
    }

    private fun buildRarityButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val rarityOptions = listOf(
            MenuButtonFactory.ListButtonOption(ItemRarity.COMMON, "Обычное"),
            MenuButtonFactory.ListButtonOption(ItemRarity.UNCOMMON, "Необычное"),
            MenuButtonFactory.ListButtonOption(ItemRarity.RARE, "Редкое"),
            MenuButtonFactory.ListButtonOption(ItemRarity.EPIC, "Эпическое")
        )
        val currentRarity = runCatching {
            val meta = session.editableItem.itemMeta
            if (meta != null && meta.hasRarity()) meta.rarity else ItemRarity.COMMON
        }.getOrDefault(ItemRarity.COMMON)
        val selectedRarityIndex = rarityOptions.indexOfFirst { it.value == currentRarity }.takeIf { it >= 0 } ?: 0

        return buttonFactory.listButton(
            material = Material.DIAMOND,
            options = rarityOptions,
            selectedIndex = selectedRarityIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Редкость: <#FFF3E0>Обычное"
                    1 -> "<!i><#C7A300>② <#FFD700>Редкость: <#FFF3E0>Необычное"
                    2 -> "<!i><#C7A300>③ <#FFD700>Редкость: <#FFF3E0>Редкое"
                    else -> "<!i><#C7A300>④ <#FFD700>Редкость: <#FFF3E0>Эпическое"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf(
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Влияет на цвет ",
                "<!i><#C7A300> ● <#FFF3E0>обычного <#FFE68A>названия. ",
                ""
            ),
            itemModifier = { selected ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.setRarity(selected.value)
                    item.itemMeta = meta
                }
            },
            action = { event, newIndex ->
                val selected = rarityOptions[newIndex]
                val meta = session.editableItem.itemMeta ?: return@listButton
                meta.setRarity(selected.value)
                session.editableItem.itemMeta = meta
                event.menu.setButton(42, buildRarityButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }

    private fun buildAttributesButton(player: Player, session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val entries = ItemAttributeMenuSupport.listEffectiveEntries(session.editableItem)
        if (entries.isEmpty()) {
            return buttonFactory.actionButton(
                material = Material.PRISMARINE_CRYSTALS,
                name = "<!i><#C7A300>⭘ <#FFD700>Атрибуты: <#FF1500>Нет",
                lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
                action = { support.transition(session) { openAttributePage(player, session) } }
            )
        }

        val lore = mutableListOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>Выбрано:"
        )
        entries.forEach { entry ->
            lore += "<!i><#C7A300> ● <#FFE68A>${ItemAttributeMenuSupport.displayAttributeName(entry.attribute)} " +
                "<#FFF3E0>${ItemAttributeMenuSupport.formatAmount(entry.modifier)} " +
                "<#C7A300>[<#FFD700>${SlotGroupAdapter.displayName(entry.modifier)}<#C7A300>]"
        }
        lore += ""

        return buttonFactory.actionButton(
            material = Material.PRISMARINE_CRYSTALS,
            name = "<!i><#C7A300>◎ <#FFD700>Атрибуты: <#00FF40>${entries.size}",
            lore = lore,
            itemModifier = {
                glint(true)
            },
            action = { support.transition(session) { openAttributePage(player, session) } }
        )
    }

    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = support.buildMenu(
            title = "<!i>▍ Продвинутый редактор [2/2]",
            menuSize = menuSize,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 29, 31, 32, 41, 42),
            session = session
        )

        support.fillBase(menu, menuSize, support.advancedBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openAdvancedPageOne(player, session) } })
        menu.setButton(29, buttonFactory.actionButton(Material.IRON_CHESTPLATE, "<!i><#C7A300>🛡 <#FFD700>Параметры экипировки", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.hideAttributes(), action = { support.transition(session) { openEquippablePage(player, session) } }))
        menu.setButton(30, buttonFactory.actionButton(
            material = Material.IRON_PICKAXE,
            name = "<!i><#C7A300>⛏ <#FFD700>Параметры инструмента",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            itemModifier = buttonFactory.hideAttributes(),
            action = { support.transition(session) { openToolPage(player, session) } }
        ))
        menu.setButton(31, buttonFactory.actionButton(
            material = Material.LAPIS_LAZULI,
            name = "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openEnchantmentsPage(player, session) } }
        ))
        menu.setButton(32, buildAttributesButton(player, session))
        menu.setButton(33, buttonFactory.actionButton(Material.FIRE_CHARGE, "<!i><#C7A300>🔥 <#FFD700>Ограничения", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Ограничивает действия в <#FFF3E0>/gm 2 ",
            ""
        )))
        menu.setButton(38, buttonFactory.actionButton(Material.APPLE, "<!i><#C7A300>🍖 <#FFD700>Съедобность", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"), buttonFactory.zeroFoodPreview()))
        menu.setButton(39, buttonFactory.actionButton(Material.TOTEM_OF_UNDYING, "<!i><#C7A300>☠ <#FFD700>Защита от смерти", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(40, buttonFactory.actionButton(Material.CLOCK, "<!i><#C7A300>⌚ <#FFD700>Задержка использования", listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
        menu.setButton(41, buttonFactory.actionButton(
            material = Material.RESIN_CLUMP,
            name = "<!i><#C7A300>⚡ <#FFD700>После использования",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openUseRemainderPage(player, session) } }
        ))
        menu.setButton(42, buildRarityButton(session))
        menu.open(player)
    }
}
