package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.enchant.EnchantmentSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class EnchantmentsEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openActivePage: (Player, ItemEditSession, Int, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    fun open(player: Player, session: ItemEditSession, back: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Зачарования",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, blackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { back(player, session) } })
        menu.setButton(30, buildGlintButton(session))
        menu.setButton(32, buildEnchantmentsButton(player, session, back))
        menu.open(player)
    }

    private fun buildGlintButton(session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val options: List<MenuButtonFactory.ListButtonOption<Boolean?>> = listOf(
            MenuButtonFactory.ListButtonOption(null, "Обычное"),
            MenuButtonFactory.ListButtonOption(false, "Всегда отключено"),
            MenuButtonFactory.ListButtonOption(true, "Всегда включено")
        )
        val meta = session.editableItem.itemMeta
        val selectedIndex = when {
            meta?.hasEnchantmentGlintOverride() != true -> 0
            !meta.enchantmentGlintOverride -> 1
            else -> 2
        }

        return buttonFactory.listButton(
            material = Material.AMETHYST_SHARD,
            options = options,
            selectedIndex = selectedIndex,
            titleBuilder = { _: MenuButtonFactory.ListButtonOption<Boolean?>, index: Int ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Свечение: <#FFF3E0>Обычное"
                    1 -> "<!i><#C7A300>② <#FFD700>Свечение: <#FFF3E0>Всегда отключено"
                    else -> "<!i><#C7A300>③ <#FFD700>Свечение: <#FFF3E0>Всегда включено"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                "<!i>"
            ),
            afterOptionsLore = listOf("<!i>"),
            action = { event, newIndex ->
                val selected = options[newIndex]
                val mutableMeta = session.editableItem.itemMeta ?: return@listButton
                mutableMeta.setEnchantmentGlintOverride(selected.value)
                session.editableItem.itemMeta = mutableMeta
                event.menu.setButton(30, buildGlintButton(session))
                event.menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
            }
        )
    }

    private fun buildEnchantmentsButton(
        player: Player,
        session: ItemEditSession,
        back: (Player, ItemEditSession) -> Unit
    ): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val entries = EnchantmentSupport.entries(session.editableItem.itemMeta)
        if (entries.isEmpty()) {
            return buttonFactory.actionButton(
                material = Material.BOOK,
                name = "<!i><#C7A300>⭘ <#FFD700>Зачарования: <#FF1500>Нет",
                lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
                action = { support.transition(session) { openActivePage(player, session, 0) { reopenPlayer, reopenSession -> open(reopenPlayer, reopenSession, back) } } }
            )
        }

        val lore = mutableListOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "<!i>",
            "<!i><#FFD700>Выбрано:"
        )
        entries.forEach { entry ->
            lore += "<!i><#C7A300> ● <#FFE68A>${entry.displayName}${EnchantmentSupport.levelDisplay(entry.level, showOne = false)}"
        }
        lore += "<!i>"

        return buttonFactory.actionButton(
            material = Material.BOOK,
            name = "<!i><#C7A300>◎ <#FFD700>Зачарования: <#00FF40>${entries.size}",
            lore = lore,
            itemModifier = { glint(true) },
            action = { support.transition(session) { openActivePage(player, session, 0) { reopenPlayer, reopenSession -> open(reopenPlayer, reopenSession, back) } } }
        )
    }
}
