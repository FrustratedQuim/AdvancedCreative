package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.service.CatalogService
import com.ratger.acreative.menus.decorationheads.service.GiveService
import com.ratger.acreative.menus.decorationheads.service.RecentService
import com.ratger.acreative.menus.decorationheads.service.SavedPagesService
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.decorationheads.support.TemporaryMenuButtonOverrideSupport
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class MenuService(
    private val plugin: org.bukkit.plugin.Plugin,
    private val sessionManager: SessionManager,
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val catalogService: CatalogService,
    private val recentService: RecentService,
    private val savedPagesService: SavedPagesService,
    private val giveService: GiveService,
    private val buttonFactory: MenuButtonFactory,
    private val renderer: MenuRenderer,
    private val executor: ExecutorService,
    private val temporaryOverrideSupport: TemporaryMenuButtonOverrideSupport,
    private val messageManager: MessageManager,
    private val promptService: ApplyPromptService
) {
    private data class CategoryOption(val key: String, val displayName: String)

    private companion object {
        const val ALL_RECENT_CATEGORY_KEY = "__all__"
        const val ALL_SAVED_PAGES_FILTER_KEY = "__all_saved__"
        const val MENU_TITLE_PREFIX = "▍ Головы"
    }

    private val signInputService = SignInputService(plugin)
    private val savedPagesFilterByPlayer = ConcurrentHashMap<UUID, String>()
    private val searchInputService = SearchInputService(
        signInputService = signInputService,
        onSubmit = { player, query ->
            val base = sessionManager.getOrCreate(player.uniqueId)
            val next = if (query.isNullOrBlank()) {
                base.copy(mode = DecorationHeadMenuMode.CATEGORY, searchQuery = null, page = 1)
            } else {
                base.copy(mode = DecorationHeadMenuMode.SEARCH, searchQuery = query, page = 1)
            }
            sessionManager.update(player.uniqueId, next)
            open(player)
        },
        onLeave = { player -> open(player) }
    )

    private val noteApplyStateManager = SavedPageNoteApplyStateManager(
        plugin = plugin,
        messageManager = messageManager,
        promptService = promptService,
        onApply = { player, pageId, note ->
            executor.submit {
                savedPagesService.updateNote(player.uniqueId, pageId, note)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (player.isOnline) {
                        openSavedPageEditor(player, sessionManager.getOrCreate(player.uniqueId), ALL_SAVED_PAGES_FILTER_KEY, pageId)
                    }
                })
            }
        },
        onReopen = { player, pageId ->
            openSavedPageEditor(player, sessionManager.getOrCreate(player.uniqueId), ALL_SAVED_PAGES_FILTER_KEY, pageId)
        }
    )

    fun applyTarget(): ApplyCommandTarget = noteApplyStateManager

    fun open(player: Player) {
        val state = sessionManager.getOrCreate(player.uniqueId)
        if (state.mode == DecorationHeadMenuMode.RECENT) {
            openRecent(player)
            return
        }
        val categoryOptions = categoryRegistry.definitions.map { CategoryOption(it.key, it.displayName) }
        val selectedCategoryIndex = categoryOptions.indexOfFirst { it.key == state.categoryKey }.takeIf { it >= 0 } ?: 0

        executor.submit {
            val page = catalogService.page(state)
            val myCount = recentService.list(player.uniqueId).size
            val myPagesCount = savedPagesService.countByPlayer(player.uniqueId)
            val isSaved = savedPagesService.findByCurrentSource(player.uniqueId, state.copy(page = page.page)) != null
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                renderer.renderCategoryMenu(
                    player = player,
                    state = state.copy(page = page.page),
                    pageResult = page,
                    myCount = myCount,
                    myPagesCount = myPagesCount,
                    isCurrentPageSaved = isSaved,
                    categoryOptions = categoryOptions.map { it.displayName },
                    selectedCategoryIndex = selectedCategoryIndex,
                    categoryNameResolver = ::resolveCategoryNameById,
                    onGive = { entry, categoryName, event ->
                        giveService.give(
                            player = player,
                            entry = entry,
                            categoryName = categoryName,
                            clickEvent = event,
                            trackRecent = true,
                            onRecentCountUpdated = { updatedCount ->
                                Bukkit.getScheduler().runTask(plugin, Runnable {
                                    if (!player.isOnline) return@Runnable
                                    event.menu.setButton(46, buttonFactory.decorationHeadsMyHeadsButton(updatedCount) {
                                        sessionManager.setRecentMode(player.uniqueId)
                                        open(player)
                                    })
                                })
                            }
                        )
                    },
                    onBack = {
                        sessionManager.update(player.uniqueId, state.copy(page = (state.page - 1).coerceAtLeast(1)))
                        open(player)
                    },
                    onForward = {
                        sessionManager.update(player.uniqueId, state.copy(page = state.page + 1))
                        open(player)
                    },
                    onMyHeads = {
                        sessionManager.setRecentMode(player.uniqueId)
                        open(player)
                    },
                    onMyPages = {
                        openSavedPages(player, state.copy(page = page.page))
                    },
                    onToggleSavePage = { event ->
                        toggleSaveCurrentPage(player, state.copy(page = page.page), event.menu)
                    },
                    onSwitchCategory = { nextIndex ->
                        val nextCategory = categoryOptions.getOrNull(nextIndex)?.key ?: categoryRegistry.firstCategoryKey()
                        sessionManager.update(player.uniqueId, DecorationHeadMenuState(DecorationHeadMenuMode.CATEGORY, nextCategory, 1, null, null))
                        open(player)
                    },
                    onSearch = {
                        player.closeInventory()
                        searchInputService.open(player)
                    }
                )
            })
        }
    }

    fun openRecent(player: Player) {
        val state = sessionManager.getOrCreate(player.uniqueId)
        val categoryOptions = recentCategoryOptions()
        val recentCategoryKey = sessionManager.getRecentCategory(player.uniqueId)
        val selectedCategoryIndex = categoryOptions.indexOfFirst { it.key == recentCategoryKey }.takeIf { it >= 0 } ?: 0
        val selectedCategoryKey = categoryOptions.getOrNull(selectedCategoryIndex)?.key ?: ALL_RECENT_CATEGORY_KEY

        executor.submit {
            val entries = recentService.list(player.uniqueId)
                .let { allEntries ->
                    if (selectedCategoryKey == ALL_RECENT_CATEGORY_KEY) allEntries else {
                        val apiCategoryIds = categoryResolver.resolveUiCategoryToApiIds(selectedCategoryKey)
                        allEntries.filter { it.categoryId in apiCategoryIds }
                    }
                }
            val pagesCount = savedPagesService.countByPlayer(player.uniqueId)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                renderer.renderRecentMenu(
                    player = player,
                    categoryName = categoryOptions[selectedCategoryIndex].displayName,
                    myPagesCount = pagesCount,
                    categoryOptions = categoryOptions.map { it.displayName },
                    selectedCategoryIndex = selectedCategoryIndex,
                    entries = entries,
                    categoryNameResolver = ::resolveCategoryNameById,
                    onGive = { entry, categoryName, event ->
                        recentService.rememberInteractionForDeferredPromotion(player.uniqueId, entry.stableKey)
                        giveService.give(player, entry, categoryName, event, trackRecent = false)
                    },
                    onMyPages = { openSavedPages(player, state) },
                    onSwitchCategory = { nextIndex ->
                        val nextCategory = categoryOptions.getOrNull(nextIndex)?.key ?: ALL_RECENT_CATEGORY_KEY
                        sessionManager.setRecentCategory(player.uniqueId, nextCategory)
                        openRecent(player)
                    },
                    onBack = {
                        sessionManager.backFromRecent(player.uniqueId)
                        open(player)
                    }
                )
            })
        }
    }

    fun openSavedPages(player: Player, originState: DecorationHeadMenuState, selectedFilterKeyOverride: String? = null) {
        val filterOptions = savedPagesFilterOptions()
        val selectedFilterKey = selectedFilterKeyOverride
            ?: savedPagesFilterByPlayer[player.uniqueId]
            ?: ALL_SAVED_PAGES_FILTER_KEY
        val selectedIndex = filterOptions.indexOfFirst { it.key == selectedFilterKey }.takeIf { it >= 0 } ?: 0
        val selected = filterOptions[selectedIndex]
        savedPagesFilterByPlayer[player.uniqueId] = selected.key
        executor.submit {
            val entries = if (selected.key == ALL_SAVED_PAGES_FILTER_KEY) {
                savedPagesService.listByPlayer(player.uniqueId)
            } else {
                savedPagesService.listByPlayerAndCategory(player.uniqueId, selected.key)
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                renderer.renderSavedPagesMenu(
                    player = player,
                    filterOptions = filterOptions.map { it.displayName },
                    selectedFilterIndex = selectedIndex,
                    entries = entries,
                    categoryTitleResolver = { entry -> categoryRegistry.byKey(entry.categoryKey)?.displayName ?: entry.categoryKey },
                    onBack = {
                        sessionManager.update(player.uniqueId, originState)
                        open(player)
                    },
                    onFilter = { nextIndex ->
                        val nextKey = filterOptions.getOrNull(nextIndex)?.key ?: ALL_SAVED_PAGES_FILTER_KEY
                        savedPagesFilterByPlayer[player.uniqueId] = nextKey
                        openSavedPages(player, originState, nextKey)
                    },
                    onOpenEntry = { entry ->
                        val restored = savedPagesService.toMenuState(entry)
                        sessionManager.update(player.uniqueId, restored)
                        open(player)
                    },
                    onEditEntry = { entry ->
                        openSavedPageEditor(player, originState, selected.key, entry.id)
                    },
                    onDeleteEntry = { entry ->
                        executor.submit {
                            savedPagesService.delete(player.uniqueId, entry.id)
                            Bukkit.getScheduler().runTask(plugin, Runnable {
                                if (player.isOnline) {
                                    openSavedPages(player, originState, selected.key)
                                }
                            })
                        }
                    }
                )
            })
        }
    }

    fun openSavedPageEditor(player: Player, originState: DecorationHeadMenuState, selectedFilterKey: String, pageId: Long) {
        executor.submit {
            val entry = savedPagesService.findById(player.uniqueId, pageId)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                if (entry == null) {
                    openSavedPages(player, originState, selectedFilterKey)
                    return@Runnable
                }
                renderer.renderSavedPageEditor(
                    player = player,
                    entry = entry,
                    onBack = { openSavedPages(player, originState, selectedFilterKey) },
                    onEditNote = {
                        noteApplyStateManager.begin(player, pageId)
                        player.closeInventory()
                    },
                    onResetNote = {
                        executor.submit {
                            savedPagesService.updateNote(player.uniqueId, pageId, null)
                            Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) openSavedPageEditor(player, originState, selectedFilterKey, pageId) })
                        }
                    },
                    onEditPage = {
                        player.closeInventory()
                        signInputService.open(
                            player = player,
                            templateLines = arrayOf("", "↑ Страница ↑", "", ""),
                            onSubmit = { submitPlayer, input ->
                                val pageNumber = input?.toIntOrNull()?.takeIf { it >= 1 }
                                executor.submit {
                                    if (pageNumber != null) {
                                        val targetEntry = savedPagesService.findById(submitPlayer.uniqueId, pageId)
                                        if (targetEntry != null) {
                                            val sourceState = savedPagesService.toMenuState(targetEntry)
                                            val totalPages = catalogService.page(sourceState).totalPages.coerceAtLeast(1)
                                            val clampedPage = pageNumber.coerceIn(1, totalPages)
                                            savedPagesService.updateSourcePage(submitPlayer.uniqueId, pageId, clampedPage)
                                        }
                                    }
                                    Bukkit.getScheduler().runTask(plugin, Runnable { if (submitPlayer.isOnline) openSavedPageEditor(submitPlayer, originState, selectedFilterKey, pageId) })
                                }
                            },
                            onLeave = { leavePlayer -> openSavedPageEditor(leavePlayer, originState, selectedFilterKey, pageId) }
                        )
                    },
                    onChangeColor = { forward ->
                        executor.submit {
                            savedPagesService.cycleMapColorKey(player.uniqueId, pageId, forward)
                            Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) openSavedPageEditor(player, originState, selectedFilterKey, pageId) })
                        }
                    },
                    onDelete = {
                        executor.submit {
                            savedPagesService.delete(player.uniqueId, pageId)
                            Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) openSavedPages(player, originState, selectedFilterKey) })
                        }
                    }
                )
            })
        }
    }

    private fun toggleSaveCurrentPage(player: Player, state: DecorationHeadMenuState, menu: ru.violence.coreapi.bukkit.api.menu.Menu) {
        executor.submit {
            val result = savedPagesService.toggleForCurrentState(player.uniqueId, state)
            val count = savedPagesService.countByPlayer(player.uniqueId)
            val isSaved = savedPagesService.findByCurrentSource(player.uniqueId, state) != null
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                when (result) {
                    SavedPagesService.ToggleResult.LIMIT_REACHED -> {
                        temporaryOverrideSupport.replaceSlotTemporarily(
                            menu = menu,
                            slot = 51,
                            temporaryButton = buttonFactory.actionButton(Material.BARRIER, "<!i><#FF1500>⚠ Превышен лимит", emptyList()),
                            restoreAfterTicks = 30L,
                            restoreButton = { buttonFactory.decorationHeadsSavePageButton(isSaved) { event -> toggleSaveCurrentPage(player, state, event.menu) } }
                        )
                    }
                    else -> {
                        menu.setButton(51, buttonFactory.decorationHeadsSavePageButton(isSaved) { event -> toggleSaveCurrentPage(player, state, event.menu) })
                    }
                }
                menu.setButton(47, buttonFactory.decorationHeadsMyPagesButton(count) { openSavedPages(player, state) })
            })
        }
    }

    fun onPlayerJoin(playerId: UUID) {
        recentService.pruneExpiredOnFirstJoin(playerId)
    }

    fun onInventoryClosed(player: Player, closedTitle: String) {
        if (!closedTitle.contains(MENU_TITLE_PREFIX)) return
        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!player.isOnline) return@Runnable
            val currentTitle = PlainTextComponentSerializer.plainText().serialize(player.openInventory.title())
            if (currentTitle.contains(MENU_TITLE_PREFIX)) return@Runnable
            recentService.commitDeferredPromotions(player.uniqueId)
        })
    }

    fun clearPlayer(playerId: UUID) {
        recentService.commitDeferredPromotions(playerId)
        sessionManager.clear(playerId)
        savedPagesFilterByPlayer.remove(playerId)
    }

    private fun resolveCategoryNameById(categoryId: Int): String {
        val mapped = categoryRegistry.definitions.firstOrNull { categoryId in categoryResolver.resolveUiCategoryToApiIds(it.key) }
        return mapped?.displayName ?: "Unknown"
    }

    private fun recentCategoryOptions(): List<CategoryOption> {
        val filtered = categoryRegistry.definitions
            .filterNot { it.mode == CategoryMode.NEW }
            .map { CategoryOption(it.key, it.displayName) }
        return listOf(CategoryOption(ALL_RECENT_CATEGORY_KEY, "Все")) + filtered
    }

    private fun savedPagesFilterOptions(): List<CategoryOption> {
        return listOf(CategoryOption(ALL_SAVED_PAGES_FILTER_KEY, "Все")) +
            categoryRegistry.definitions.map { CategoryOption(it.key, it.displayName) }
    }
}
