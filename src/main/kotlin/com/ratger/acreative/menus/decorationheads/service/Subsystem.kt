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
import com.ratger.acreative.menus.decorationheads.persistence.Database
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Subsystem(
    hooker: FunctionHooker,
    parser: MiniMessageParser,
    buttonFactory: MenuButtonFactory
) {
    private val config = hooker.configManager.config
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "acreative-decoration-heads").apply { isDaemon = true }
    }

    private val categoryRegistry = CategoryRegistry(config)
    private val categoryResolver = CategoryResolver()
    private val cache = Cache(
        dynamicLimit = config.getInt("decoration-heads.head-cache-size", 4096),
        searchLimit = config.getInt("decoration-heads.search-query-cache-size", 128)
    )

    private val database = Database(hooker.plugin.dataFolder)
    private val catalogRepository = CatalogRepository(database)
    private val recentRepository = RecentRepository(
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

    private val catalogService = CatalogService(
        cache = cache,
        categoryRegistry = categoryRegistry,
        categoryResolver = categoryResolver,
        catalogRepository = catalogRepository,
        menuPageSize = config.getInt("decoration-heads.menu-page-size", 45)
    )
    private val recentService = RecentService(recentRepository, executor)
    private val giveService = GiveService(hooker.menuService.headMutationSupport(), parser, recentService)

    private val sessionManager = SessionManager(categoryRegistry.firstCategoryKey())
    private val renderer = MenuRenderer(hooker.plugin, parser, buttonFactory, categoryRegistry)
    val menuService = MenuService(
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

    private val syncService = SyncService(
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
