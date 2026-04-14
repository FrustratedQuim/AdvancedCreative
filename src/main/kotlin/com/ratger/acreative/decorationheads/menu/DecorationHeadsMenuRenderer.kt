package com.ratger.acreative.decorationheads.menu

import com.ratger.acreative.decorationheads.category.DecorationHeadCategoryRegistry
import com.ratger.acreative.decorationheads.model.DecorationHeadEntry
import com.ratger.acreative.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.decorationheads.model.DecorationHeadPageResult
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class DecorationHeadsMenuRenderer(
    private val plugin: org.bukkit.plugin.Plugin,
    private val parser: MiniMessageParser,
    private val buttonFactory: MenuButtonFactory,
    private val categoryRegistry: DecorationHeadCategoryRegistry
) {
    fun renderCategoryMenu(
        player: Player,
        state: DecorationHeadMenuState,
        pageResult: DecorationHeadPageResult,
        myCount: Int,
        onGive: (DecorationHeadEntry) -> Unit,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onMyHeads: () -> Unit,
        onSwitchCategory: () -> Unit,
        onSearch: () -> Unit
    ) {
        val categoryName = categoryRegistry.byKey(state.categoryKey)?.displayName ?: state.categoryKey
        val menu = baseMenu("▍ Головы → $categoryName [${pageResult.page}/${pageResult.totalPages}]", setOf(0, 53, 46, 48, 49, 50, 52) + (0..44).toSet())

        fillBase(menu, black = setOf(45, 53), gray = setOf(47, 51))
        if (pageResult.page > 1) menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        if (pageResult.page < pageResult.totalPages) menu.setButton(50, buttonFactory.decorationHeadsForwardButton { onForward() })

        menu.setButton(46, buttonFactory.decorationHeadsMyHeadsButton(myCount) { onMyHeads() })
        menu.setButton(49, buttonFactory.decorationHeadsCategoryButton(categoryRegistry.definitions.map { it.displayName }, categoryName) { onSwitchCategory() })
        menu.setButton(52, buttonFactory.decorationHeadsSearchButton(state.searchQuery) { onSearch() })

        pageResult.entries.forEachIndexed { index, entry ->
            menu.setButton(index, buttonFactory.decorationHeadsResultButton(entry, categoryName, state.mode == DecorationHeadMenuMode.SEARCH || categoryRegistry.byKey(state.categoryKey)?.mode?.name == "NEW") {
                onGive(entry)
            })
        }

        menu.open(player)
    }

    fun renderRecentMenu(
        player: Player,
        entries: List<DecorationHeadEntry>,
        categoryNameResolver: (Int) -> String,
        onGive: (DecorationHeadEntry) -> Unit,
        onBack: () -> Unit
    ) {
        val menu = baseMenu("▍ Головы → Мои", setOf(48) + (0..44).toSet())
        fillBase(menu, black = setOf(45, 53), gray = setOf(47, 50, 51, 52))
        menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        entries.take(45).forEachIndexed { index, entry ->
            val categoryName = categoryNameResolver(entry.categoryId)
            menu.setButton(index, buttonFactory.decorationHeadsResultButton(entry, categoryName, true) { onGive(entry) })
        }
        menu.open(player)
    }

    private fun baseMenu(title: String, interactiveTopSlots: Set<Int>): Menu = Menu.newBuilder(plugin)
        .title(parser.parse(title))
        .size(54)
        .rows(MenuRows.SIX)
        .postClickRefresh(false)
        .clickListener { event -> event.rawSlot !in 0..53 || event.rawSlot in interactiveTopSlots }
        .dragListener { event -> event.rawSlots.none { it in 0..53 } }
        .build()

    private fun fillBase(menu: Menu, black: Set<Int>, gray: Set<Int>) {
        val blackButton = buttonFactory.decorationHeadsBlackFiller()
        val grayButton = buttonFactory.decorationHeadsGrayFiller()
        for (slot in 0..53) {
            menu.setButton(
                slot,
                when {
                    slot in black -> blackButton
                    slot in gray -> grayButton
                    else -> grayButton
                }
            )
        }
    }
}
