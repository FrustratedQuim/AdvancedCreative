package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsHttpClient
import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsRequestFactory
import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsResponseMapper
import com.ratger.acreative.menus.decorationheads.cache.Cache
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.menu.MenuRenderer
import com.ratger.acreative.menus.decorationheads.menu.MenuService
import com.ratger.acreative.menus.decorationheads.menu.SessionManager
import com.ratger.acreative.menus.decorationheads.persistence.CatalogRepository
import com.ratger.acreative.menus.decorationheads.persistence.RecentRepository
import com.ratger.acreative.menus.decorationheads.persistence.SavedPagesRepository
import com.ratger.acreative.menus.decorationheads.support.TemporaryMenuButtonOverrideSupport
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Subsystem(
    hooker: FunctionHooker,
    parser: MiniMessageParser,
    buttonFactory: MenuButtonFactory
) {
    data class MemorySnapshot(
        val dynamicEntries: List<com.ratger.acreative.menus.decorationheads.model.Entry>,
        val searchEntries: List<Pair<String, List<com.ratger.acreative.menus.decorationheads.model.Entry>>>,
        val dynamicCount: Int,
        val dynamicLimit: Int,
        val searchCount: Int,
        val searchLimit: Int,
        val cachedRecentEntries: Int,
        val cachedRecentPlayers: Int,
        val sessionEntries: Int
    )

    private val config = hooker.configManager.config
    private val plugin = hooker.plugin
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "acreative-decoration-heads").apply { isDaemon = true }
    }

    private val categoryRegistry = CategoryRegistry(config)
    private val categoryResolver = CategoryResolver()
    private val cache = Cache(
        dynamicLimit = config.getInt("decoration-heads.head-cache-size", 4096),
        searchLimit = config.getInt("decoration-heads.search-query-cache-size", 128)
    )

    private val catalogRepository = CatalogRepository(hooker.database)
    private val playerRecentLimit = config.getInt("decoration-heads.player-recent-limit", 45)
    private val recentRepository = RecentRepository(hooker.database, playerRecentLimit)
    private val savedPagesRepository = SavedPagesRepository(hooker.database)

    private val requestFactory = MinecraftHeadsRequestFactory(
        baseUrl = config.getString("decoration-heads.api.base-url", "https://minecraft-heads.com")!!,
        appUuid = config.getString("decoration-heads.api.app-uuid", "APP_UUID")!!,
        apiKey = config.getString("decoration-heads.api.api-key", "")!!,
        demo = config.getBoolean("decoration-heads.api.demo", false)
    )
    private val httpClient = MinecraftHeadsHttpClient(
        requestFactory = requestFactory,
        connectTimeoutMs = config.getLong("decoration-heads.api.connect-timeout-ms", 5000L),
        readTimeoutMs = config.getLong("decoration-heads.api.read-timeout-ms", 7000L)
    )
    private val mapper = MinecraftHeadsResponseMapper()

    private val catalogService = CatalogService(
        cache = cache,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        catalogRepository = catalogRepository,
        menuPageSize = config.getInt("decoration-heads.menu-page-size", 45)
    )
    private val recentService = RecentService(recentRepository, executor, playerRecentLimit)
    private val giveService = GiveService(
        hooker.menuService.headMutationSupport(),
        parser,
        recentService,
        hooker.accountLinkRequirementService
    )
    private val savedPagesService = SavedPagesService(savedPagesRepository, 45)

    private val sessionManager = SessionManager(categoryRegistry.firstCategoryKey())
    private val renderer = MenuRenderer(hooker.plugin, parser, buttonFactory, categoryRegistry)
    private val temporaryMenuButtonOverrideSupport = TemporaryMenuButtonOverrideSupport(hooker.tickScheduler)
    val menuService = MenuService(
        plugin = hooker.plugin,
        sessionManager = sessionManager,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        catalogService = catalogService,
        recentService = recentService,
        savedPagesService = savedPagesService,
        giveService = giveService,
        buttonFactory = buttonFactory,
        renderer = renderer,
        executor = executor,
        temporaryOverrideSupport = temporaryMenuButtonOverrideSupport,
        messageManager = hooker.messageManager,
        promptService = ApplyPromptService(hooker.messageManager),
        accountLinkRequirementService = hooker.accountLinkRequirementService
    )

    init {
        hooker.menuService.registerApplyTarget(menuService.applyTarget())
    }

    private val syncService = SyncService(
        client = httpClient,
        mapper = mapper,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        logger = hooker.plugin.logger
    )
    private val restoreService = HeadCatalogRestoreService(
        catalogRepository = catalogRepository,
        syncService = syncService,
        fallbackCatalogReader = HeadFallbackCatalogReader(),
        dataFolder = plugin.dataFolder,
        logger = hooker.plugin.logger
    )

    private var periodicRecentFlushTask: BukkitTask? = null

    fun init() {
        recentService.init()
        executor.submit {
            syncService.refreshCategoryMappings()
            catalogService.warmRecentPublishedPages(config.getInt("decoration-heads.warm-pages", 1))
        }
        val flushIntervalTicks = 6L * 60L * 60L * 20L
        periodicRecentFlushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable { recentService.flushDirtyToDatabase() },
            flushIntervalTicks,
            flushIntervalTicks
        )
    }

    fun restoreCatalogFromDat(): HeadCatalogRestoreService.RestoreResult = restoreService.restoreFromDat()

    fun restoreCatalogFromApi(): HeadCatalogRestoreService.RestoreResult = restoreService.restoreFromApi()

    fun shutdown() {
        periodicRecentFlushTask?.cancel()
        runCatching {
            executor.submit { recentService.flushDirtyToDatabase() }.get(5, TimeUnit.SECONDS)
        }.onFailure {
            plugin.logger.warning("Failed to flush decoration heads cache before shutdown: ${it.message}")
            recentService.flushDirtyToDatabase()
        }
        executor.shutdownNow()
    }

    fun memorySnapshot(): MemorySnapshot {
        val recentSnapshot = recentService.memorySnapshot()
        return MemorySnapshot(
            dynamicEntries = cache.dynamicEntriesSnapshot(),
            searchEntries = cache.searchIndex.snapshot(),
            dynamicCount = cache.dynamicSize(),
            dynamicLimit = cache.dynamicLimit(),
            searchCount = cache.searchIndex.size(),
            searchLimit = cache.searchIndex.limit(),
            cachedRecentEntries = recentSnapshot.cachedEntries,
            cachedRecentPlayers = recentSnapshot.cachedPlayers,
            sessionEntries = sessionManager.totalEntriesCount()
        )
    }
}
