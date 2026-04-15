package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.PageResult
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class MenuRenderer(
    private val plugin: org.bukkit.plugin.Plugin,
    private val parser: MiniMessageParser,
    private val buttonFactory: MenuButtonFactory,
    private val categoryRegistry: CategoryRegistry
) {
    fun renderCategoryMenu(
        player: Player,
        state: DecorationHeadMenuState,
        pageResult: PageResult,
        myCount: Int,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        categoryNameResolver: (Int) -> String,
        onGive: (Entry, String, ClickEvent) -> Unit,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onMyHeads: () -> Unit,
        onSwitchCategory: (Int) -> Unit,
        onSearch: () -> Unit
    ) {
        val categoryName = categoryRegistry.byKey(state.categoryKey)?.displayName ?: state.categoryKey
        val menu = baseMenu("▍ Головы → $categoryName [${pageResult.page}/${pageResult.totalPages}]", setOf(0, 53, 46, 48, 49, 50, 52) + (0..44).toSet())

        fillBase(menu, black = setOf(45, 53), gray = setOf(46, 47, 48, 50, 51))
        if (pageResult.page > 1) menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        if (pageResult.page < pageResult.totalPages) menu.setButton(50, buttonFactory.decorationHeadsForwardButton { onForward() })

        menu.setButton(46, buttonFactory.decorationHeadsMyHeadsButton(myCount) { onMyHeads() })
        menu.setButton(49, buttonFactory.decorationHeadsCategoryButton(categoryOptions, selectedCategoryIndex) { nextIndex -> onSwitchCategory(nextIndex) })
        menu.setButton(52, buttonFactory.decorationHeadsSearchButton(state.searchQuery) { onSearch() })

        val showRealCategory = state.mode == DecorationHeadMenuMode.SEARCH || categoryRegistry.byKey(state.categoryKey)?.mode == CategoryMode.NEW
        pageResult.entries.forEachIndexed { index, entry ->
            val entryCategoryName = if (showRealCategory) categoryNameResolver(entry.categoryId) else categoryName
            menu.setButton(index, buttonFactory.decorationHeadsResultButton(entry, entryCategoryName, showRealCategory) {
                onGive(entry, entryCategoryName, it)
            })
        }

        menu.open(player)
    }

    fun renderRecentMenu(
        player: Player,
        categoryName: String,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        entries: List<Entry>,
        categoryNameResolver: (Int) -> String,
        onGive: (Entry, String, ClickEvent) -> Unit,
        onSwitchCategory: (Int) -> Unit,
        onBack: () -> Unit
    ) {
        val menu = baseMenu("▍ Головы → Мои → $categoryName", setOf(48, 49) + (0..44).toSet())
        fillBase(menu, black = setOf(45, 53), gray = setOf(46, 47, 50, 51, 52))
        menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        menu.setButton(49, buttonFactory.decorationHeadsCategoryButton(categoryOptions, selectedCategoryIndex) { nextIndex -> onSwitchCategory(nextIndex) })
        entries.take(45).forEachIndexed { index, entry ->
            val categoryName = categoryNameResolver(entry.categoryId)
            menu.setButton(index, buttonFactory.decorationHeadsResultButton(entry, categoryName, true) { onGive(entry, categoryName, it) })
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
        black.forEach { slot -> menu.setButton(slot, blackButton) }
        gray.forEach { slot -> menu.setButton(slot, grayButton) }
    }
}
