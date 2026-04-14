package com.ratger.acreative.decorationheads.menu

import com.ratger.acreative.decorationheads.category.DecorationHeadCategoryRegistry
import com.ratger.acreative.decorationheads.category.DecorationHeadCategoryResolver
import com.ratger.acreative.decorationheads.model.DecorationHeadMenuMode
import com.ratger.acreative.decorationheads.model.DecorationHeadMenuState
import com.ratger.acreative.decorationheads.service.DecorationHeadsCatalogService
import com.ratger.acreative.decorationheads.service.DecorationHeadsGiveService
import com.ratger.acreative.decorationheads.service.DecorationHeadsRecentService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.ExecutorService

class DecorationHeadsMenuService(
    private val plugin: org.bukkit.plugin.Plugin,
    private val sessionManager: DecorationHeadsSessionManager,
    private val categoryRegistry: DecorationHeadCategoryRegistry,
    private val categoryResolver: DecorationHeadCategoryResolver,
    private val catalogService: DecorationHeadsCatalogService,
    private val recentService: DecorationHeadsRecentService,
    private val giveService: DecorationHeadsGiveService,
    private val renderer: DecorationHeadsMenuRenderer,
    private val executor: ExecutorService
) {
    private val searchInputService = DecorationHeadsSearchInputService(
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
                    onGive = { entry ->
                        giveService.give(player, entry)
                        open(player)
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
                    onSwitchCategory = {
                        val nextCategory = nextCategoryKey(state.categoryKey)
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
        executor.submit {
            val entries = recentService.list(player.uniqueId)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                renderer.renderRecentMenu(
                    player = player,
                    entries = entries,
                    categoryNameResolver = { categoryId -> resolveCategoryNameById(categoryId) },
                    onGive = { entry ->
                        giveService.give(player, entry)
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

    private fun nextCategoryKey(current: String): String {
        val all = categoryRegistry.definitions
        val currentIndex = all.indexOfFirst { it.key == current }
        if (currentIndex == -1 || all.isEmpty()) return categoryRegistry.firstCategoryKey()
        return all[(currentIndex + 1) % all.size].key
    }

    private fun resolveCategoryNameById(categoryId: Int): String {
        val mapped = categoryRegistry.definitions.firstOrNull { categoryId in categoryResolver.resolveUiCategoryToApiIds(it.key) }
        return mapped?.displayName ?: "Unknown"
    }
}
