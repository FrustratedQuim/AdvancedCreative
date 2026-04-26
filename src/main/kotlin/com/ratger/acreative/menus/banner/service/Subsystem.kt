package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.banner.BannerButtonFactory
import com.ratger.acreative.menus.banner.editor.BannerEditorMenu
import com.ratger.acreative.menus.banner.editor.BannerEditorMenuSupport
import com.ratger.acreative.menus.banner.editor.BannerEditorSessionManager
import com.ratger.acreative.menus.banner.menu.BannerMenuRenderer
import com.ratger.acreative.menus.banner.menu.BannerMenuService
import com.ratger.acreative.menus.banner.menu.BannerSessionManager
import com.ratger.acreative.menus.banner.menu.BannerTitleApplyStateManager
import com.ratger.acreative.menus.banner.storage.BannerStorageConfigResolver
import com.ratger.acreative.menus.banner.storage.BannerStorageItemNormalizer
import com.ratger.acreative.menus.banner.storage.BannerStorageMenuController
import com.ratger.acreative.menus.banner.storage.BannerStorageMenuRenderer
import com.ratger.acreative.menus.banner.storage.BannerStorageNameSupport
import com.ratger.acreative.menus.banner.storage.BannerStorageRepository
import com.ratger.acreative.menus.banner.storage.BannerStorageService
import com.ratger.acreative.menus.banner.storage.BannerStorageSessionManager
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.banner.persistence.BannedPatternRepository
import com.ratger.acreative.menus.banner.persistence.BannedUserRepository
import com.ratger.acreative.menus.banner.persistence.PublishedBannerRepository
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.Bukkit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Subsystem(
    private val hooker: FunctionHooker,
    parser: MiniMessageParser,
    sharedButtonFactory: MenuButtonFactory
) {
    data class MemorySnapshot(
        val authorNames: List<String>,
        val publicationHistoryKeys: List<String>,
        val bannerSessionEntries: Int,
        val editorSessions: List<com.ratger.acreative.menus.banner.editor.BannerEditorSession>,
        val storageSessions: Int
    )

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "acreative-banner").apply { isDaemon = true }
    }

    private val publishedBannerRepository = PublishedBannerRepository(hooker.database, 45)
    private val bannedPatternRepository = BannedPatternRepository(hooker.database, 45)
    private val bannedUserRepository = BannedUserRepository(hooker.database, 45)
    private val authorCache = BannerAuthorCache(publishedBannerRepository)
    private val publicationHistoryCache = BannerPublicationHistoryCache(publishedBannerRepository)
    private val galleryService = BannerGalleryService(publishedBannerRepository, BannerTakeCooldownService())
    private val publicationService = BannerPublicationService(
        publishedBannerRepository,
        bannedPatternRepository,
        authorCache,
        publicationHistoryCache
    )
    private val playerLookupService = BannerPlayerLookupService(LicensedProfileLookupService())
    private val moderationService = BannerModerationService(
        bannedPatternRepository = bannedPatternRepository,
        bannedUserRepository = bannedUserRepository,
        publicationService = publicationService,
        playerLookupService = playerLookupService
    )
    private val giveService = BannerGiveService()
    private val buttonFactory = BannerButtonFactory(parser, sharedButtonFactory)
    private val signInputService = SignInputService(hooker.plugin)
    private val sessionManager = BannerSessionManager()
    private val editorSessionManager = BannerEditorSessionManager()
    private val storageSessionManager = BannerStorageSessionManager()
    private val storageRepository = BannerStorageRepository(hooker.database)
    private val storageService = BannerStorageService(
        repository = storageRepository,
        normalizer = BannerStorageItemNormalizer(BannerStorageNameSupport()),
        configResolver = BannerStorageConfigResolver(hooker)
    )
    private val renderer = BannerMenuRenderer(hooker.plugin, parser, buttonFactory)
    private val storageRenderer = BannerStorageMenuRenderer(hooker.plugin, parser, buttonFactory)
    private val storageController = BannerStorageMenuController(hooker.plugin, storageService, hooker.plugin.logger)

    val menuService: BannerMenuService

    init {
        var createdMenuService: BannerMenuService? = null

        val titleApplyStateManager = BannerTitleApplyStateManager(
            plugin = hooker.plugin,
            messageManager = hooker.messageManager,
            promptService = ApplyPromptService(hooker.messageManager),
            onApply = { player, title -> createdMenuService?.handleBannerTitleApply(player, title) },
            onReopen = { player -> createdMenuService?.reopenAfterTitleApply(player) }
        )

        val editorSupport = BannerEditorMenuSupport(
            hooker = hooker,
            sessionManager = editorSessionManager,
            buttonFactory = sharedButtonFactory,
            parser = parser,
            syncEditedBannerBack = { player, session -> createdMenuService?.syncEditedBannerBack(player, session) }
        )
        val editorMenu = BannerEditorMenu(
            support = editorSupport,
            buttonFactory = buttonFactory,
            requestSignInput = { player, templateLines, onSubmit, onLeave ->
                signInputService.open(
                    player = player,
                    templateLines = templateLines,
                    onSubmit = { submitPlayer, input ->
                        Bukkit.getScheduler().runTask(hooker.plugin, Runnable { onSubmit(submitPlayer, input) })
                    },
                    onLeave = { leavePlayer ->
                        Bukkit.getScheduler().runTask(hooker.plugin, Runnable { onLeave(leavePlayer) })
                    }
                )
            },
            openMainMenu = { player -> createdMenuService?.openMainMenu(player) }
        )

        createdMenuService = BannerMenuService(
            hooker = hooker,
            executor = executor,
            buttonFactory = buttonFactory,
            sessionManager = sessionManager,
            editorSessionManager = editorSessionManager,
            galleryService = galleryService,
            giveService = giveService,
            publicationService = publicationService,
            moderationService = moderationService,
            playerLookupService = playerLookupService,
            authorCache = authorCache,
            renderer = renderer,
            storageRenderer = storageRenderer,
            storageController = storageController,
            storageService = storageService,
            storageSessionManager = storageSessionManager,
            editorMenu = editorMenu,
            titleApplyStateManager = titleApplyStateManager
        )

        menuService = createdMenuService
        hooker.menuService.registerApplyTarget(menuService.applyTarget())
    }

    fun init() {
        authorCache.reload()
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    fun buttonFactory(): BannerButtonFactory = buttonFactory

    fun memorySnapshot(): MemorySnapshot = MemorySnapshot(
        authorNames = authorCache.snapshotValues(),
        publicationHistoryKeys = publicationHistoryCache.snapshotKeys(),
        bannerSessionEntries = sessionManager.totalEntriesCount(),
        editorSessions = editorSessionManager.sessionsSnapshot(),
        storageSessions = storageSessionManager.totalSessions()
    )
}
