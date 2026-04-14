package com.ratger.acreative.decorationheads.service

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.decorationheads.api.MinecraftHeadsHttpClient
import com.ratger.acreative.decorationheads.api.MinecraftHeadsRequestFactory
import com.ratger.acreative.decorationheads.api.MinecraftHeadsResponseMapper
import com.ratger.acreative.decorationheads.cache.DecorationHeadCache
import com.ratger.acreative.decorationheads.category.DecorationHeadCategoryRegistry
import com.ratger.acreative.decorationheads.category.DecorationHeadCategoryResolver
import com.ratger.acreative.decorationheads.menu.DecorationHeadsMenuRenderer
import com.ratger.acreative.decorationheads.menu.DecorationHeadsMenuService
import com.ratger.acreative.decorationheads.menu.DecorationHeadsSessionManager
import com.ratger.acreative.decorationheads.persistence.DecorationHeadCatalogRepository
import com.ratger.acreative.decorationheads.persistence.DecorationHeadRecentRepository
import com.ratger.acreative.decorationheads.persistence.DecorationHeadsDatabase
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DecorationHeadsSubsystem(
    hooker: FunctionHooker,
    parser: MiniMessageParser,
    buttonFactory: MenuButtonFactory
) {
    private val config = hooker.configManager.config
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "acreative-decoration-heads").apply { isDaemon = true }
    }

    private val categoryRegistry = DecorationHeadCategoryRegistry(config)
    private val categoryResolver = DecorationHeadCategoryResolver()
    private val cache = DecorationHeadCache(
        dynamicLimit = config.getInt("decoration-heads.head-cache-size", 4096),
        searchLimit = config.getInt("decoration-heads.search-query-cache-size", 128)
    )

    private val database = DecorationHeadsDatabase(hooker.plugin.dataFolder)
    private val catalogRepository = DecorationHeadCatalogRepository(database)
    private val recentRepository = DecorationHeadRecentRepository(
        database,
        config.getInt("decoration-heads.player-recent-limit", 45)
    )

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

    private val catalogService = DecorationHeadsCatalogService(
        cache = cache,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        catalogRepository = catalogRepository,
        menuPageSize = config.getInt("decoration-heads.menu-page-size", 45)
    )
    private val recentService = DecorationHeadsRecentService(recentRepository)
    private val giveService = DecorationHeadsGiveService(hooker.menuService.headMutationSupport(), recentService)

    private val sessionManager = DecorationHeadsSessionManager(categoryRegistry.firstCategoryKey())
    private val renderer = DecorationHeadsMenuRenderer(hooker.plugin, parser, buttonFactory, categoryRegistry)
    val menuService = DecorationHeadsMenuService(
        plugin = hooker.plugin,
        sessionManager = sessionManager,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        catalogService = catalogService,
        recentService = recentService,
        giveService = giveService,
        renderer = renderer,
        executor = executor
    )

    private val syncService = DecorationHeadsSyncService(
        client = httpClient,
        mapper = mapper,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        cache = cache,
        catalogRepository = catalogRepository,
        executor = executor,
        logger = hooker.plugin.logger,
        warmPages = config.getInt("decoration-heads.warm-pages", 1),
        menuPageSize = config.getInt("decoration-heads.menu-page-size", 45)
    )

    fun init() {
        database.init()
        syncService.start()
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
