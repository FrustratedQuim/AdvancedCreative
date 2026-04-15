package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.category.CategoryMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.menus.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.menus.decorationheads.service.CatalogService
import com.ratger.acreative.menus.decorationheads.service.GiveService
import com.ratger.acreative.menus.decorationheads.service.RecentService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.ExecutorService

class MenuService(
    private val plugin: org.bukkit.plugin.Plugin,
    private val sessionManager: SessionManager,
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val catalogService: CatalogService,
    private val recentService: RecentService,
    private val giveService: GiveService,
    private val renderer: MenuRenderer,
    private val executor: ExecutorService
) {
    private data class CategoryOption(
        val key: String,
        val displayName: String
    )

    private companion object {
        const val ALL_RECENT_CATEGORY_KEY = "__all__"
    }

    private val searchInputService = SearchInputService(
        plugin = plugin,
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
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                renderer.renderCategoryMenu(
                    player = player,
                    state = state.copy(page = page.page),
                    pageResult = page,
                    myCount = myCount,
                    categoryOptions = categoryOptions.map { it.displayName },
                    selectedCategoryIndex = selectedCategoryIndex,
                    categoryNameResolver = { categoryId -> resolveCategoryNameById(categoryId) },
                    onGive = { entry, categoryName, event ->
                        giveService.give(player, entry, categoryName, event, trackRecent = true)
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
        val selectedCategoryIndex = categoryOptions.indexOfFirst { it.key == state.categoryKey }.takeIf { it >= 0 } ?: 0
        val selectedCategoryKey = categoryOptions.getOrNull(selectedCategoryIndex)?.key ?: ALL_RECENT_CATEGORY_KEY

        executor.submit {
            val entries = recentService.list(player.uniqueId)
                .let { allEntries ->
                    if (selectedCategoryKey == ALL_RECENT_CATEGORY_KEY) {
                        allEntries
                    } else {
                        val apiCategoryIds = categoryResolver.resolveUiCategoryToApiIds(selectedCategoryKey)
                        allEntries.filter { entry -> entry.categoryId in apiCategoryIds }
                    }
                }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                renderer.renderRecentMenu(
                    player = player,
                    categoryName = categoryOptions[selectedCategoryIndex].displayName,
                    categoryOptions = categoryOptions.map { it.displayName },
                    selectedCategoryIndex = selectedCategoryIndex,
                    entries = entries,
                    categoryNameResolver = { categoryId -> resolveCategoryNameById(categoryId) },
                    onGive = { entry, categoryName, event ->
                        giveService.give(player, entry, categoryName, event, trackRecent = false)
                    },
                    onSwitchCategory = { nextIndex ->
                        val nextCategory = categoryOptions.getOrNull(nextIndex)?.key ?: ALL_RECENT_CATEGORY_KEY
                        val old = sessionManager.getOrCreate(player.uniqueId)
                        sessionManager.update(player.uniqueId, old.copy(categoryKey = nextCategory))
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

    fun clearPlayer(playerId: java.util.UUID) = sessionManager.clear(playerId)

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
}
