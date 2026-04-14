package com.ratger.acreative.menus.itemEdit.pages.enchantments

import com.ratger.acreative.itemedit.enchant.EnchantmentSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.pages.common.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class EnchantmentsEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openActivePage: (Player, ItemEditSession, Int, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession, back: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Зачарования",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { back(player, session) } })
        menu.setButton(30, buildGlintButton(session))
        menu.setButton(32, buildEnchantmentsButton(player, session, back))
        menu.open(player)
    }

    private fun buildGlintButton(session: ItemEditSession): Button {
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
                ""
            ),
            afterOptionsLore = listOf(""),
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
    ): Button {
        val entries = EnchantmentSupport.entries(session.editableItem.itemMeta)
        return buttonFactory.statefulSummaryButton(
            material = Material.BOOK,
            active = entries.isNotEmpty(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Зачарования: <#00FF40>${entries.size}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Зачарования: <#FF1500>Нет",
            selectedEntriesLore = entries.map { entry ->
                "<!i><#C7A300> ● <#FFE68A>${entry.displayName}${EnchantmentSupport.levelDisplay(entry.level, showOne = false)} "
            },
            action = { support.transition(session) { openActivePage(player, session, 0) { reopenPlayer, reopenSession -> open(reopenPlayer, reopenSession, back) } } }
        )
    }
}
