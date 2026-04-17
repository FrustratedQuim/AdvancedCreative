package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.model.PageResult
import com.ratger.acreative.menus.decorationheads.model.SavedPageEntry
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

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
        myPagesCount: Int,
        isCurrentPageSaved: Boolean,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        categoryNameResolver: (Int) -> String,
        onGive: (Entry, String, ClickEvent) -> Unit,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onMyHeads: () -> Unit,
        onMyPages: () -> Unit,
        onToggleSavePage: (ClickEvent) -> Unit,
        onSwitchCategory: (Int) -> Unit,
        onSearch: () -> Unit
    ) {
        val categoryName = categoryRegistry.byKey(state.categoryKey)?.displayName ?: state.categoryKey
        val menu = baseMenu("▍ Головы → $categoryName [${pageResult.page}/${pageResult.totalPages}]", setOf(46, 47, 48, 49, 50, 51, 52) + (0..44).toSet())

        fillBase(menu, black = setOf(45, 53), gray = setOf(46, 47, 48, 50, 51, 52))
        if (pageResult.page > 1) menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        if (pageResult.page < pageResult.totalPages) menu.setButton(50, buttonFactory.decorationHeadsForwardButton { onForward() })

        menu.setButton(46, buttonFactory.decorationHeadsMyHeadsButton(myCount) { onMyHeads() })
        menu.setButton(47, buttonFactory.decorationHeadsMyPagesButton(myPagesCount) { onMyPages() })
        menu.setButton(49, buttonFactory.decorationHeadsCategoryButton(categoryOptions, selectedCategoryIndex) { nextIndex -> onSwitchCategory(nextIndex) })
        menu.setButton(51, buttonFactory.decorationHeadsSavePageButton(isCurrentPageSaved) { onToggleSavePage(it) })
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
        myPagesCount: Int,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        entries: List<Entry>,
        categoryNameResolver: (Int) -> String,
        onGive: (Entry, String, ClickEvent) -> Unit,
        onMyPages: () -> Unit,
        onSwitchCategory: (Int) -> Unit,
        onBack: () -> Unit
    ) {
        val menu = baseMenu("▍ Головы → Мои → $categoryName", setOf(47, 48, 49, 52) + (0..44).toSet())
        fillBase(menu, black = setOf(45, 53), gray = setOf(46, 47, 50, 51, 52))
        menu.setButton(47, buttonFactory.decorationHeadsMyPagesButton(myPagesCount) { onMyPages() })
        menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        menu.setButton(49, buttonFactory.decorationHeadsCategoryButton(categoryOptions, selectedCategoryIndex) { nextIndex -> onSwitchCategory(nextIndex) })
        menu.setButton(52, buttonFactory.decorationHeadsReminderButton())
        entries.take(45).forEachIndexed { index, entry ->
            val resolved = categoryNameResolver(entry.categoryId)
            menu.setButton(index, buttonFactory.decorationHeadsResultButton(entry, resolved, true) { onGive(entry, resolved, it) })
        }
        menu.open(player)
    }

    fun renderSavedPagesMenu(
        player: Player,
        selectedFilterTitle: String,
        filterOptions: List<String>,
        selectedFilterIndex: Int,
        entries: List<SavedPageEntry>,
        categoryTitleResolver: (SavedPageEntry) -> String,
        onBack: () -> Unit,
        onFilter: (Int) -> Unit,
        onOpenEntry: (SavedPageEntry) -> Unit,
        onEditEntry: (SavedPageEntry) -> Unit
    ) {
        val menu = baseMenu("▍ Головы → Мои страницы", setOf(48, 49) + (0..44).toSet())
        fillBase(menu, black = setOf(45, 53), gray = setOf(46, 47, 48, 50, 51, 52))
        menu.setButton(48, buttonFactory.decorationHeadsBackButton { onBack() })
        menu.setButton(49, buttonFactory.decorationHeadsCategoryButton(filterOptions, selectedFilterIndex) { onFilter(it) })

        entries.take(45).forEachIndexed { index, entry ->
            menu.setButton(index, buttonFactory.decorationHeadsSavedPageButton(entry, categoryTitleResolver(entry)) { event ->
                when {
                    event.isLeft || event.isShiftLeft -> onOpenEntry(entry)
                    event.isRight || event.isShiftRight -> onEditEntry(entry)
                }
            })
        }
        menu.open(player)
    }

    fun renderSavedPageEditor(
        player: Player,
        entry: SavedPageEntry,
        onBack: () -> Unit,
        onEditNote: () -> Unit,
        onResetNote: () -> Unit,
        onEditPage: () -> Unit,
        onChangeColor: (Boolean) -> Unit,
        onDelete: () -> Unit
    ) {
        val menu = Menu.newBuilder(plugin)
            .title(parser.parse("▍ Страница #${entry.id} → Редактор"))
            .rows(MenuRows.THREE)
            .postClickRefresh(false)
            .clickListener { event -> event.rawSlot !in 0..26 || event.rawSlot in setOf(11, 12, 13, 15, 18) }
            .dragListener { event -> event.rawSlots.none { it in 0..26 } }
            .build()

        val black = buttonFactory.decorationHeadsBlackFiller()
        val gray = buttonFactory.decorationHeadsGrayFiller()
        val blackSlots = setOf(0, 8, 9, 17, 26)
        (0..26).forEach { slot -> menu.setButton(slot, if (slot in blackSlots) black else gray) }

        menu.setButton(11, buttonFactory.decorationHeadsSavedPageNoteButton(entry.note, onApply = { onEditNote() }, onReset = { onResetNote() }))
        menu.setButton(12, buttonFactory.decorationHeadsSavedPageNumberButton(entry.sourcePage) { onEditPage() })
        menu.setButton(13, buttonFactory.decorationHeadsSavedPageColorButton(entry.mapColorKey) { _, forward -> onChangeColor(forward) })
        menu.setButton(15, buttonFactory.decorationHeadsSavedPageDeleteButton { onDelete() })
        menu.setButton(18, buttonFactory.decorationHeadsBackButton { onBack() })

        menu.open(player)
    }

    private fun baseMenu(title: String, interactiveTopSlots: Set<Int>): Menu = Menu.newBuilder(plugin)
        .title(parser.parse(title))
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
