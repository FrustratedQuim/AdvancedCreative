package com.ratger.acreative.menus.banner.menu

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.banner.BannerButtonFactory
import com.ratger.acreative.menus.banner.editor.BannerEditorMenu
import com.ratger.acreative.menus.banner.editor.BannerEditorSession
import com.ratger.acreative.menus.banner.editor.BannerEditorSessionManager
import com.ratger.acreative.menus.banner.model.*
import com.ratger.acreative.menus.banner.service.*
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.decorationheads.support.TemporaryMenuButtonOverrideSupport
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import java.util.*
import java.util.concurrent.ExecutorService

class BannerMenuService(
    private val hooker: FunctionHooker,
    private val executor: ExecutorService,
    private val buttonFactory: BannerButtonFactory,
    private val sessionManager: BannerSessionManager,
    private val editorSessionManager: BannerEditorSessionManager,
    private val galleryService: BannerGalleryService,
    private val giveService: BannerGiveService,
    private val publicationService: BannerPublicationService,
    private val moderationService: BannerModerationService,
    private val playerLookupService: BannerPlayerLookupService,
    private val authorCache: BannerAuthorCache,
    private val renderer: BannerMenuRenderer,
    private val editorMenu: BannerEditorMenu,
    private val titleApplyStateManager: BannerTitleApplyStateManager
) {
    private val plugin = hooker.plugin
    private val temporaryOverrideSupport = TemporaryMenuButtonOverrideSupport(hooker.tickScheduler)
    private val searchInputService = BannerSearchInputService(
        signInputService = SignInputService(plugin),
        onSubmit = { player, query ->
            val currentState = sessionManager.publicState(player.uniqueId)
            val nextState = currentState.copy(
                page = 1,
                searchQuery = query?.trim()?.takeIf { it.isNotEmpty() }
            )
            sessionManager.updatePublicState(player.uniqueId, nextState)
            openPublicGallery(player, nextState)
        },
        onLeave = { player -> openPublicGallery(player, sessionManager.publicState(player.uniqueId)) }
    )

    fun applyTarget(): ApplyCommandTarget = titleApplyStateManager

    fun openMainMenu(player: Player) {
        sessionManager.markLastMenuAsBanner(player.uniqueId)
        renderer.renderMainMenu(
            player = player,
            onOpenEditor = { openEditor(player, openedFromMainMenu = true) },
            onOpenGallery = {
                val nextState = sessionManager.publicState(player.uniqueId).copy(
                    openedFromMainMenu = true,
                    moderatorMode = false
                )
                sessionManager.updatePublicState(player.uniqueId, nextState)
                openPublicGallery(player, nextState)
            }
        )
    }

    fun openEditor(player: Player, openedFromMainMenu: Boolean) {
        sessionManager.markLastMenuAsBanner(player.uniqueId)
        val existingSession = editorSessionManager.getSession(player)
        if (existingSession != null) {
            existingSession.openedFromMainMenu = existingSession.openedFromMainMenu || openedFromMainMenu
            editorMenu.open(player, existingSession)
            return
        }

        val handBanner = player.inventory.itemInMainHand.takeIf(BannerPatternSupport::isBanner)
        val session = editorSessionManager.openSession(player, handBanner, openedFromMainMenu)
        if (handBanner != null) {
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        }
        editorMenu.open(player, session)
    }

    fun openPostFromCommand(player: Player) {
        if (moderationService.isUserBanned(player.uniqueId)) {
            hooker.messageManager.sendChat(player, MessageKey.BANNER_POST_BANNED)
            return
        }

        val normalizedBanner = BannerPatternSupport.normalizeForStorage(player.inventory.itemInMainHand)
        if (normalizedBanner == null) {
            hooker.messageManager.sendChat(player, MessageKey.BANNER_HAND_REQUIRED)
            return
        }

        val previousDraft = sessionManager.getPostDraft(player.uniqueId)
        val draft = BannerPostDraft(
            bannerItem = normalizedBanner,
            title = previousDraft?.title,
            category = previousDraft?.category ?: BannerCatalog.publishCategories.first()
        )
        sessionManager.setPostDraft(player.uniqueId, draft)
        openStoredPostMenu(player)
    }

    fun openStoredPostMenu(player: Player, currentMenu: Menu? = null) {
        if (moderationService.isUserBanned(player.uniqueId)) {
            hooker.messageManager.sendChat(player, MessageKey.BANNER_POST_BANNED)
            return
        }

        val draft = sessionManager.getPostDraft(player.uniqueId) ?: run {
            openPostFromCommand(player)
            return
        }
        sessionManager.markLastMenuAsBanner(player.uniqueId)

        val categoryOptions = BannerCatalog.publishCategories.map { it.displayName }
        val selectedCategoryIndex = BannerCatalog.publishCategories.indexOf(draft.category).takeIf { it >= 0 } ?: 0

        renderer.renderPostMenu(
            player = player,
            draft = draft,
            categoryOptions = categoryOptions,
            selectedCategoryIndex = selectedCategoryIndex,
            currentMenu = currentMenu,
            onApplyTitle = {
                titleApplyStateManager.begin(player)
                player.closeInventory()
            },
            onResetTitle = { event ->
                updatePostDraft(player.uniqueId) { it.copy(title = null) }
                openStoredPostMenu(player, event.menu)
            },
            onSwitchCategory = { event, index ->
                val category = BannerCatalog.publishCategories.getOrNull(index)
                if (category != null) {
                    updatePostDraft(player.uniqueId) { it.copy(category = category) }
                    openStoredPostMenu(player, event.menu)
                }
            },
            onConfirm = { event -> confirmPublish(player, event.menu) }
        )
    }

    fun reopenPostFromApply(player: Player): Boolean {
        if (!sessionManager.wasLastMenuBanner(player.uniqueId)) {
            return false
        }
        openPostFromCommand(player)
        return true
    }

    fun openPublicGalleryFromCommand(player: Player, moderationMode: Boolean = false) {
        sessionManager.markLastMenuAsBanner(player.uniqueId)
        val nextState = sessionManager.publicState(player.uniqueId).copy(
            authorFilterUuid = null,
            authorFilterName = null,
            openedFromMainMenu = false,
            moderatorMode = moderationMode
        )
        sessionManager.updatePublicState(player.uniqueId, nextState)
        openPublicGallery(player, nextState)
    }

    fun openPublicGalleryForAuthor(player: Player, authorName: String, moderationMode: Boolean) {
        val resolvedAuthorName = authorCache.resolve(authorName) ?: authorName
        val targetUser = playerLookupService.findUser(resolvedAuthorName)
        if (targetUser == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        executor.submit {
            val publishedCount = galleryService.countByAuthorName(targetUser.name)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }
                if (publishedCount <= 0) {
                    hooker.messageManager.sendChat(player, MessageKey.BANNER_USER_NO_FLAGS)
                    return@runSync
                }
                openPublicGalleryForResolvedAuthor(player, targetUser.name, moderationMode)
            }
        }
    }

    fun openBannedPatterns(player: Player, requestedPage: Int = 1) {
        executor.submit {
            val pageResult = moderationService.bannedPatternsPage(requestedPage)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }
                renderer.renderBannedPatterns(
                    player = player,
                    pageResult = pageResult,
                    onEntry = { entry -> unbanPatternFromMenu(player, entry, pageResult.page) },
                    onBack = if (pageResult.page > 1) {
                        { openBannedPatterns(player, pageResult.page - 1) }
                    } else {
                        null
                    },
                    onForward = if (pageResult.page < pageResult.totalPages) {
                        { openBannedPatterns(player, pageResult.page + 1) }
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun openBannedUsers(player: Player, requestedPage: Int = 1) {
        executor.submit {
            val pageResult = moderationService.bannedUsersPage(requestedPage)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }
                renderer.renderBannedUsers(
                    player = player,
                    pageResult = pageResult,
                    onEntry = { entry -> unbanUserFromMenu(player, entry, pageResult.page) },
                    onBack = if (pageResult.page > 1) {
                        { openBannedUsers(player, pageResult.page - 1) }
                    } else {
                        null
                    },
                    onForward = if (pageResult.page < pageResult.totalPages) {
                        { openBannedUsers(player, pageResult.page + 1) }
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun togglePatternBan(player: Player) {
        executor.submit {
            val result = moderationService.togglePattern(player)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }
                when (result) {
                    BannerModerationService.PatternToggleResult.INVALID -> hooker.messageManager.sendChat(player, MessageKey.BANNER_HAND_REQUIRED)
                    BannerModerationService.PatternToggleResult.BANNED -> hooker.messageManager.sendChat(player, MessageKey.BANNER_PATTERN_BANNED)
                    BannerModerationService.PatternToggleResult.UNBANNED -> hooker.messageManager.sendChat(player, MessageKey.BANNER_PATTERN_UNBANNED)
                }
            }
        }
    }

    fun toggleUserBan(player: Player, targetName: String, reason: String?) {
        val targetUser = playerLookupService.findUser(targetName)
        if (targetUser == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        moderationService.toggleUserBan(targetUser, reason)
            .whenComplete { result, error ->
                runSync {
                    if (!player.isOnline) {
                        return@runSync
                    }
                    if (error != null || result == null) {
                        hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
                        return@runSync
                    }

                    when (result) {
                        BannerModerationService.UserToggleResult.Unbanned -> {
                            hooker.messageManager.sendChat(
                                player,
                                MessageKey.BANNER_USER_UNBANNED,
                                mapOf("player" to targetUser.name)
                            )
                        }
                        is BannerModerationService.UserToggleResult.Banned -> {
                            hooker.messageManager.sendChat(
                                player,
                                MessageKey.BANNER_USER_BANNED,
                                mapOf("player" to result.entry.playerName)
                            )
                        }
                    }
                }
            }
    }

    fun authorSuggestions(prefix: String): List<String> = authorCache.suggest(prefix)

    fun handlePlayerDisconnect(player: Player) {
        titleApplyStateManager.cancel(player)
        val session = editorSessionManager.getSession(player)
        if (session != null) {
            editorSessionManager.clear(player.uniqueId)
            syncEditedBannerBack(player, session)
        }
        sessionManager.clear(player.uniqueId)
    }

    fun handlePlayerDeath(player: Player) {
        titleApplyStateManager.cancel(player)
        val session = editorSessionManager.getSession(player)
        if (session != null) {
            editorSessionManager.clear(player.uniqueId)
            syncEditedBannerBack(player, session)
        }
        sessionManager.clearTransient(player.uniqueId)
    }

    fun handleBannerTitleApply(player: Player, title: String?) {
        updatePostDraft(player.uniqueId) { it.copy(title = title?.trim()?.takeIf(String::isNotBlank)) }
        openStoredPostMenu(player)
    }

    fun reopenAfterTitleApply(player: Player) {
        openStoredPostMenu(player)
    }

    fun syncEditedBannerBack(player: Player, session: BannerEditorSession) {
        val item = session.editableBanner?.clone() ?: return
        if (item.type == Material.AIR || item.amount <= 0) {
            return
        }

        val inventory = player.inventory
        val targetSlotItem = inventory.getItem(session.originalMainHandSlot)
        if (targetSlotItem == null || targetSlotItem.type == Material.AIR || targetSlotItem.amount <= 0) {
            inventory.setItem(session.originalMainHandSlot, item)
            return
        }

        val remainingAmount = PlayerInventoryTransferSupport.storeInPreferredSlots(inventory, item)
        if (remainingAmount > 0) {
            player.world.dropItemNaturally(
                player.location.clone().add(0.0, 1.0, 0.0),
                item.clone().apply { amount = remainingAmount }
            )
        }
    }

    private fun openPublicGallery(player: Player, state: BannerGalleryState) {
        sessionManager.markLastMenuAsBanner(player.uniqueId)
        sessionManager.updatePublicState(player.uniqueId, state)
        executor.submit {
            val pageResult = galleryService.publicPage(state)
            val currentState = state.copy(page = pageResult.page)
            sessionManager.updatePublicState(player.uniqueId, currentState)
            val myFlagsCount = galleryService.myCount(player)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }

                val selectedFilterIndex = BannerCatalog.sorts.indexOf(currentState.sort).takeIf { it >= 0 } ?: 0
                val selectedCategoryIndex = BannerCatalog.galleryCategories.indexOf(currentState.category).takeIf { it >= 0 } ?: 0
                renderer.renderPublicGallery(
                    player = player,
                    state = currentState,
                    pageResult = pageResult,
                    myFlagsCount = myFlagsCount,
                    filterOptions = BannerCatalog.sorts.map { it.displayName },
                    selectedFilterIndex = selectedFilterIndex,
                    categoryOptions = BannerCatalog.galleryCategories.map { it.displayName },
                    selectedCategoryIndex = selectedCategoryIndex,
                    onEntry = { entry, event -> handlePublicEntryClick(player, entry, event) },
                    onMyFlags = {
                        sessionManager.rememberMyOrigin(player.uniqueId, sessionManager.publicState(player.uniqueId))
                        openMyGallery(player, sessionManager.myState(player.uniqueId))
                    },
                    onFilter = { newIndex ->
                        val nextSort = BannerCatalog.sorts.getOrNull(newIndex)
                        if (nextSort != null) {
                            val nextState = sessionManager.publicState(player.uniqueId).copy(page = 1, sort = nextSort)
                            sessionManager.updatePublicState(player.uniqueId, nextState)
                            openPublicGallery(player, nextState)
                        }
                    },
                    onCategory = { newIndex ->
                        val nextCategory = BannerCatalog.galleryCategories.getOrNull(newIndex)
                        if (nextCategory != null) {
                            val nextState = sessionManager.publicState(player.uniqueId).copy(
                                page = 1,
                                category = nextCategory,
                                searchQuery = null
                            )
                            sessionManager.updatePublicState(player.uniqueId, nextState)
                            openPublicGallery(player, nextState)
                        }
                    },
                    onSearch = { event ->
                        val currentState = sessionManager.publicState(player.uniqueId)
                        if (MenuUiSupport.isDropClick(event)) {
                            val nextState = currentState.copy(page = 1, searchQuery = null)
                            sessionManager.updatePublicState(player.uniqueId, nextState)
                            openPublicGallery(player, nextState)
                            return@renderPublicGallery
                        }

                        player.closeInventory()
                        searchInputService.open(player)
                    },
                    onBack = buildPublicBackAction(player, currentState),
                    onForward = if (pageResult.page < pageResult.totalPages) {
                        {
                            val nextState = sessionManager.publicState(player.uniqueId).copy(page = pageResult.page + 1)
                            sessionManager.updatePublicState(player.uniqueId, nextState)
                            openPublicGallery(player, nextState)
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    private fun openMyGallery(player: Player, state: MyBannersState) {
        sessionManager.markLastMenuAsBanner(player.uniqueId)
        sessionManager.updateMyState(player.uniqueId, state)
        executor.submit {
            val pageResult = galleryService.myPage(player, state)
            val currentCount = galleryService.myCount(player)
            val currentState = state.copy(page = pageResult.page)
            sessionManager.updateMyState(player.uniqueId, currentState)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }

                val selectedFilterIndex = BannerCatalog.sorts.indexOf(currentState.sort).takeIf { it >= 0 } ?: 0
                val selectedCategoryIndex = BannerCatalog.galleryCategories.indexOf(currentState.category).takeIf { it >= 0 } ?: 0
                renderer.renderMyGallery(
                    player = player,
                    state = currentState,
                    pageResult = pageResult,
                    currentCount = currentCount,
                    filterOptions = BannerCatalog.sorts.map { it.displayName },
                    selectedFilterIndex = selectedFilterIndex,
                    categoryOptions = BannerCatalog.galleryCategories.map { it.displayName },
                    selectedCategoryIndex = selectedCategoryIndex,
                    onEntry = { entry, event -> handleMyEntryClick(player, entry, event) },
                    onFilter = { newIndex ->
                        val nextSort = BannerCatalog.sorts.getOrNull(newIndex)
                        if (nextSort != null) {
                            val nextState = sessionManager.myState(player.uniqueId).copy(page = 1, sort = nextSort)
                            sessionManager.updateMyState(player.uniqueId, nextState)
                            openMyGallery(player, nextState)
                        }
                    },
                    onCategory = { newIndex ->
                        val nextCategory = BannerCatalog.galleryCategories.getOrNull(newIndex)
                        if (nextCategory != null) {
                            val nextState = sessionManager.myState(player.uniqueId).copy(page = 1, category = nextCategory)
                            sessionManager.updateMyState(player.uniqueId, nextState)
                            openMyGallery(player, nextState)
                        }
                    },
                    onBack = {
                        if (currentState.page > 1) {
                            val nextState = currentState.copy(page = currentState.page - 1)
                            sessionManager.updateMyState(player.uniqueId, nextState)
                            openMyGallery(player, nextState)
                        } else {
                            val origin = sessionManager.consumeMyOrigin(player.uniqueId) ?: sessionManager.publicState(player.uniqueId)
                            openPublicGallery(player, origin)
                        }
                    },
                    onForward = if (pageResult.page < pageResult.totalPages) {
                        {
                            val nextState = sessionManager.myState(player.uniqueId).copy(page = pageResult.page + 1)
                            sessionManager.updateMyState(player.uniqueId, nextState)
                            openMyGallery(player, nextState)
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }

    private fun confirmPublish(player: Player, menu: Menu) {
        if (moderationService.isUserBanned(player.uniqueId)) {
            hooker.messageManager.sendChat(player, MessageKey.BANNER_POST_BANNED)
            return
        }

        val draft = sessionManager.getPostDraft(player.uniqueId) ?: run {
            openPostFromCommand(player)
            return
        }

        executor.submit {
            when (val result = publicationService.publish(player, draft)) {
                is BannerPublicationService.Result.Failure -> runSync {
                    if (!player.isOnline) {
                        return@runSync
                    }
                    showTemporaryPostWarning(
                        menu = menu,
                        title = when (result.reason) {
                            BannerPublicationService.PublishFailure.ALREADY_PUBLISHED_TODAY -> "<!i><#FF1500>⚠ Уже опубликован сегодня"
                            BannerPublicationService.PublishFailure.ALREADY_PUBLISHED_BY_PLAYER -> "<!i><#FF1500>⚠ Вы уже публиковали такой флаг"
                            BannerPublicationService.PublishFailure.LIMIT_REACHED -> {
                                val currentCount = galleryService.myCount(player)
                                "<!i><#FF1500>⚠ Превышен лимит ($currentCount/${BannerPatternSupport.PUBLISH_LIMIT})"
                            }
                            BannerPublicationService.PublishFailure.BLOCKED_PATTERN -> "<!i><#FF1500>⚠ Проказник! Такой флаг запрещён"
                        }
                    )
                }
                is BannerPublicationService.Result.Success -> runSync {
                    if (!player.isOnline) {
                        return@runSync
                    }
                    hooker.messageManager.sendChat(
                        player,
                        MessageKey.BANNER_POST_SUCCESS,
                        mapOf(
                            "current" to result.value.activeCount.toString(),
                            "limit" to BannerPatternSupport.PUBLISH_LIMIT.toString()
                        )
                    )
                    openPublicGalleryForCategory(player, draft.category)
                }
            }
        }
    }

    private fun buildPublicBackAction(player: Player, currentState: BannerGalleryState): (() -> Unit)? {
        return when {
            currentState.page > 1 -> {
                {
                    val nextState = sessionManager.publicState(player.uniqueId).copy(page = currentState.page - 1)
                    sessionManager.updatePublicState(player.uniqueId, nextState)
                    openPublicGallery(player, nextState)
                }
            }
            currentState.openedFromMainMenu -> {
                { openMainMenu(player) }
            }
            else -> null
        }
    }

    private fun handlePublicEntryClick(player: Player, entry: PublishedBannerEntry, event: ClickEvent) {
        if (sessionManager.publicState(player.uniqueId).moderatorMode) {
            event.handle.isCancelled = true
            executor.submit {
                publicationService.deletePublishedBanner(entry.id)
                runSync {
                    if (player.isOnline) {
                        openPublicGallery(player, sessionManager.publicState(player.uniqueId))
                    }
                }
            }
            return
        }

        giveService.give(player, entry, event)
    }

    private fun handleMyEntryClick(player: Player, entry: PublishedBannerEntry, event: ClickEvent) {
        if (event.isRight || event.isShiftRight) {
            event.handle.isCancelled = true
            executor.submit {
                publicationService.deletePublishedBanner(entry.id)
                runSync {
                    if (player.isOnline) {
                        openMyGallery(player, sessionManager.myState(player.uniqueId))
                    }
                }
            }
            return
        }

        giveService.give(player, entry, event)
    }

    private fun unbanPatternFromMenu(player: Player, entry: BannedPatternEntry, currentPage: Int) {
        executor.submit {
            moderationService.unbanPattern(entry.patternSignature)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }
                hooker.messageManager.sendChat(player, MessageKey.BANNER_PATTERN_UNBANNED)
                openBannedPatterns(player, currentPage)
            }
        }
    }

    private fun unbanUserFromMenu(player: Player, entry: BannedUserEntry, currentPage: Int) {
        executor.submit {
            moderationService.unbanUser(entry.playerUuid)
            runSync {
                if (!player.isOnline) {
                    return@runSync
                }
                hooker.messageManager.sendChat(
                    player,
                    MessageKey.BANNER_USER_UNBANNED,
                    mapOf("player" to entry.playerName)
                )
                openBannedUsers(player, currentPage)
            }
        }
    }

    private fun showTemporaryPostWarning(menu: Menu, title: String) {
        temporaryOverrideSupport.replaceSlotTemporarily(
            menu = menu,
            slot = 31,
            temporaryButton = buttonFactory.temporaryBarrierButton(title),
            restoreAfterTicks = 30L,
            restoreButton = { buttonFactory.postConfirmButton { confirmPublish(it.player, it.menu) } }
        )
    }

    private fun updatePostDraft(playerId: UUID, update: (BannerPostDraft) -> BannerPostDraft) {
        val currentDraft = sessionManager.getPostDraft(playerId) ?: return
        sessionManager.setPostDraft(playerId, update(currentDraft))
    }

    private fun openPublicGalleryForResolvedAuthor(player: Player, authorName: String, moderationMode: Boolean) {
        val currentState = sessionManager.publicState(player.uniqueId)
        val nextState = currentState.copy(
            page = 1,
            authorFilterUuid = null,
            authorFilterName = authorName,
            moderatorMode = moderationMode,
            openedFromMainMenu = false
        )
        sessionManager.updatePublicState(player.uniqueId, nextState)
        openPublicGallery(player, nextState)
    }

    private fun openPublicGalleryForCategory(player: Player, category: BannerCategory) {
        val currentState = sessionManager.publicState(player.uniqueId)
        val nextState = currentState.copy(
            page = 1,
            category = category,
            searchQuery = null,
            authorFilterUuid = null,
            authorFilterName = null,
            openedFromMainMenu = false,
            moderatorMode = false
        )
        sessionManager.updatePublicState(player.uniqueId, nextState)
        openPublicGallery(player, nextState)
    }

    private fun runSync(action: () -> Unit) {
        Bukkit.getScheduler().runTask(plugin, Runnable(action))
    }

    fun clearApplyRecoveryContext(player: Player) {
        sessionManager.clearLastBannerMenuMarker(player.uniqueId)
    }
}
