package com.ratger.acreative.menus.banner.menu

import com.ratger.acreative.menus.banner.BannerButtonFactory
import com.ratger.acreative.menus.banner.model.BannedPatternEntry
import com.ratger.acreative.menus.banner.model.BannedUserEntry
import com.ratger.acreative.menus.banner.model.BannerGalleryState
import com.ratger.acreative.menus.banner.model.BannerPageResult
import com.ratger.acreative.menus.banner.model.BannerPostDraft
import com.ratger.acreative.menus.banner.model.MyBannersState
import com.ratger.acreative.menus.banner.model.PublishedBannerEntry
import com.ratger.acreative.menus.banner.service.BannerTextSupport
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class BannerMenuRenderer(
    private val plugin: org.bukkit.plugin.Plugin,
    private val parser: MiniMessageParser,
    private val buttonFactory: BannerButtonFactory
) {
    fun renderMainMenu(
        player: Player,
        onOpenStorage: () -> Unit,
        onOpenEditor: () -> Unit,
        onOpenGallery: () -> Unit
    ) {
        val menu = buildMenu(
            title = "▍ Менеджер флагов",
            rows = MenuRows.THREE,
            interactiveTopSlots = setOf(11, 13, 15),
            allowPlayerInventoryClicks = false,
            blockShiftClickFromPlayerInventory = false
        )
        fillMask(menu, 27, setOf(0, 8, 9, 17, 18, 26))
        menu.setButton(11, buttonFactory.mainMenuStorageButton { onOpenStorage() })
        menu.setButton(13, buttonFactory.mainMenuEditorButton { onOpenEditor() })
        menu.setButton(15, buttonFactory.mainMenuGalleryButton { onOpenGallery() })
        menu.open(player)
    }

    fun renderPostMenu(
        player: Player,
        draft: BannerPostDraft,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        currentMenu: Menu? = null,
        onApplyTitle: (ClickEvent) -> Unit,
        onResetTitle: (ClickEvent) -> Unit,
        onSwitchCategory: (ClickEvent, Int) -> Unit,
        onConfirm: (ClickEvent) -> Unit
    ) {
        val menu = currentMenu ?: buildMenu(
            title = "▍ Публикация флага",
            rows = MenuRows.FOUR,
            interactiveTopSlots = setOf(11, 15, 31),
            allowPlayerInventoryClicks = false,
            blockShiftClickFromPlayerInventory = false
        ).also {
            fillMask(it, 36, setOf(0, 8, 9, 17, 18, 26, 27, 28, 34, 35))
        }
        menu.setButton(11, buttonFactory.postTitleButton(draft.title, onApplyTitle, onResetTitle))
        menu.setButton(13, buttonFactory.previewButton(draft.bannerItem))
        menu.setButton(15, buttonFactory.postCategoryButton(categoryOptions, selectedCategoryIndex, onSwitchCategory))
        menu.setButton(31, buttonFactory.postConfirmButton(onConfirm))
        if (currentMenu == null) {
            menu.open(player)
        }
    }

    fun renderPublicGallery(
        player: Player,
        state: BannerGalleryState,
        pageResult: BannerPageResult<PublishedBannerEntry>,
        myFlagsCount: Int,
        filterOptions: List<String>,
        selectedFilterIndex: Int,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        onEntry: (PublishedBannerEntry, ClickEvent) -> Unit,
        onMyFlags: () -> Unit,
        onFilter: (Int) -> Unit,
        onCategory: (Int) -> Unit,
        onSearch: (ClickEvent) -> Unit,
        onBack: (() -> Unit)?,
        onForward: (() -> Unit)?,
        currentMenu: Menu? = null
    ) {
        val title = BannerTextSupport.titleWithPages(
            BannerTextSupport.galleryBaseTitle(state.authorFilterName, state.category),
            pageResult.page,
            pageResult.totalPages
        )
        val interactive = mutableSetOf(46, 47, 49, 51, 52)
        if (onBack != null) interactive += 48
        if (onForward != null) interactive += 50
        interactive += contentSlots(pageResult.entries.size)

        val menu = currentMenu ?: buildMenu(
            title = title,
            rows = MenuRows.SIX,
            interactiveTopSlots = interactive,
            allowPlayerInventoryClicks = true,
            blockShiftClickFromPlayerInventory = true
        )
        if (currentMenu != null) {
            configureCurrentMenu(menu, title, MenuRows.SIX, interactive, true, true)
        }

        clearTopArea(menu)
        fillFooter(menu)
        if (onBack != null) menu.setButton(48, buttonFactory.backButton { onBack() })
        if (onForward != null) menu.setButton(50, buttonFactory.forwardButton { onForward() })
        menu.setButton(46, buttonFactory.myFlagsButton(myFlagsCount) { onMyFlags() })
        menu.setButton(47, buttonFactory.filterButton(filterOptions, selectedFilterIndex, onFilter))
        menu.setButton(49, buttonFactory.categoryButton(categoryOptions, selectedCategoryIndex, onCategory))
        menu.setButton(51, buttonFactory.postInfoButton())
        menu.setButton(52, buttonFactory.searchButton(state.searchQuery) { onSearch(it) })

        pageResult.entries.forEachIndexed { index, entry ->
            menu.setButton(
                index,
                buttonFactory.publishedBannerButton(
                    entry = entry,
                    categoryName = entry.category.displayName,
                    showAuthor = true,
                    showCategory = state.category == com.ratger.acreative.menus.banner.model.BannerCategory.ALL,
                    showDeleteHint = false,
                    moderationMode = state.moderatorMode,
                    action = { onEntry(entry, it) }
                )
            )
        }

        if (currentMenu == null) {
            menu.open(player)
        }
    }

    fun renderMyGallery(
        player: Player,
        state: MyBannersState,
        pageResult: BannerPageResult<PublishedBannerEntry>,
        currentCount: Int,
        limitText: String,
        filterOptions: List<String>,
        selectedFilterIndex: Int,
        categoryOptions: List<String>,
        selectedCategoryIndex: Int,
        onEntry: (PublishedBannerEntry, ClickEvent) -> Unit,
        onFilter: (Int) -> Unit,
        onCategory: (Int) -> Unit,
        onBack: () -> Unit,
        onForward: (() -> Unit)?,
        currentMenu: Menu? = null
    ) {
        val title = BannerTextSupport.titleWithPages(
            BannerTextSupport.myFlagsBaseTitle(state.category),
            pageResult.page,
            pageResult.totalPages
        )
        val interactive = mutableSetOf(46, 48, 49, 52)
        if (onForward != null) interactive += 50
        interactive += contentSlots(pageResult.entries.size)

        val menu = currentMenu ?: buildMenu(
            title = title,
            rows = MenuRows.SIX,
            interactiveTopSlots = interactive,
            allowPlayerInventoryClicks = true,
            blockShiftClickFromPlayerInventory = true
        )
        if (currentMenu != null) {
            configureCurrentMenu(menu, title, MenuRows.SIX, interactive, true, true)
        }

        clearTopArea(menu)
        fillFooter(menu)
        menu.setButton(46, buttonFactory.filterButton(filterOptions, selectedFilterIndex, onFilter))
        menu.setButton(48, buttonFactory.backButton { onBack() })
        menu.setButton(49, buttonFactory.categoryButton(categoryOptions, selectedCategoryIndex, onCategory))
        if (onForward != null) menu.setButton(50, buttonFactory.forwardButton { onForward() })
        menu.setButton(52, buttonFactory.limitInfoButton(currentCount, limitText))

        pageResult.entries.forEachIndexed { index, entry ->
            menu.setButton(
                index,
                buttonFactory.publishedBannerButton(
                    entry = entry,
                    categoryName = entry.category.displayName,
                    showAuthor = false,
                    showCategory = state.category == com.ratger.acreative.menus.banner.model.BannerCategory.ALL,
                    showDeleteHint = true,
                    moderationMode = false,
                    action = { onEntry(entry, it) }
                )
            )
        }

        if (currentMenu == null) {
            menu.open(player)
        }
    }

    fun renderBannedPatterns(
        player: Player,
        pageResult: BannerPageResult<BannedPatternEntry>,
        onEntry: (BannedPatternEntry) -> Unit,
        onBack: (() -> Unit)?,
        onForward: (() -> Unit)?,
        currentMenu: Menu? = null
    ) {
        renderPagedModerationMenu(
            player = player,
            title = BannerTextSupport.titleWithPages("▍ Забаненые паттерны", pageResult.page, pageResult.totalPages),
            entries = pageResult.entries,
            entryButton = { entry -> buttonFactory.bannedPatternButton(entry) { onEntry(entry) } },
            onBack = onBack,
            onForward = onForward,
            currentMenu = currentMenu
        )
    }

    fun renderBannedUsers(
        player: Player,
        pageResult: BannerPageResult<BannedUserEntry>,
        onEntry: (BannedUserEntry) -> Unit,
        onBack: (() -> Unit)?,
        onForward: (() -> Unit)?,
        currentMenu: Menu? = null
    ) {
        renderPagedModerationMenu(
            player = player,
            title = BannerTextSupport.titleWithPages("▍ Забаненые игроки", pageResult.page, pageResult.totalPages),
            entries = pageResult.entries,
            entryButton = { entry -> buttonFactory.bannedUserButton(entry) { onEntry(entry) } },
            onBack = onBack,
            onForward = onForward,
            currentMenu = currentMenu
        )
    }

    private fun <T> renderPagedModerationMenu(
        player: Player,
        title: String,
        entries: List<T>,
        entryButton: (T) -> ru.violence.coreapi.bukkit.api.menu.button.Button,
        onBack: (() -> Unit)?,
        onForward: (() -> Unit)?,
        currentMenu: Menu? = null
    ) {
        val interactive = mutableSetOf<Int>()
        if (onBack != null) interactive += 48
        if (onForward != null) interactive += 50
        interactive += contentSlots(entries.size)

        val menu = currentMenu ?: buildMenu(
            title = title,
            rows = MenuRows.SIX,
            interactiveTopSlots = interactive,
            allowPlayerInventoryClicks = false,
            blockShiftClickFromPlayerInventory = false
        )
        if (currentMenu != null) {
            configureCurrentMenu(menu, title, MenuRows.SIX, interactive, false, false)
        }
        clearTopArea(menu)
        fillFooter(menu)
        if (onBack != null) menu.setButton(48, buttonFactory.backButton { onBack() })
        if (onForward != null) menu.setButton(50, buttonFactory.forwardButton { onForward() })
        entries.forEachIndexed { index, entry ->
            menu.setButton(index, entryButton(entry))
        }
        if (currentMenu == null) {
            menu.open(player)
        }
    }

    private fun buildMenu(
        title: String,
        rows: MenuRows,
        interactiveTopSlots: Set<Int>,
        allowPlayerInventoryClicks: Boolean,
        blockShiftClickFromPlayerInventory: Boolean
    ): Menu = MenuUiSupport.buildMenu(
        plugin = plugin,
        parser = parser,
        title = "<!i>$title",
        rows = rows,
        menuTopRange = 0 until rows.size,
        interactiveTopSlots = interactiveTopSlots,
        allowPlayerInventoryClicks = allowPlayerInventoryClicks,
        blockShiftClickFromPlayerInventory = blockShiftClickFromPlayerInventory
    )

    private fun fillMask(menu: Menu, menuSize: Int, blackSlots: Set<Int>) {
        MenuUiSupport.fillByMask(
            menu = menu,
            menuSize = menuSize,
            primarySlots = blackSlots,
            primaryButton = buttonFactory.blackFiller(),
            secondaryButton = buttonFactory.grayFiller()
        )
    }

    private fun fillFooter(menu: Menu) {
        menu.setButton(45, buttonFactory.blackFiller())
        menu.setButton(53, buttonFactory.blackFiller())
        for (slot in 46..52) {
            menu.setButton(slot, buttonFactory.grayFiller())
        }
    }

    private fun clearTopArea(menu: Menu) {
        for (slot in 0 until 45) {
            menu.setButton(slot, buttonFactory.airButton())
        }
    }

    private fun configureCurrentMenu(
        menu: Menu,
        title: String,
        rows: MenuRows,
        interactiveTopSlots: Set<Int>,
        allowPlayerInventoryClicks: Boolean,
        blockShiftClickFromPlayerInventory: Boolean
    ) {
        menu.setTitle(parser.parse("<!i>$title"))
        menu.setClickListener { event ->
            if (
                blockShiftClickFromPlayerInventory &&
                (event.isShiftLeft || event.isShiftRight) &&
                event.rawSlot !in (0 until rows.size)
            ) {
                return@setClickListener false
            }

            if (event.rawSlot in 0 until rows.size) {
                return@setClickListener event.rawSlot in interactiveTopSlots
            }

            allowPlayerInventoryClicks
        }
        menu.setDragListener { event -> event.rawSlots.none { it in 0 until rows.size } }
    }

    private fun contentSlots(size: Int): Set<Int> = (0 until size.coerceIn(0, 45)).toSet()
}
