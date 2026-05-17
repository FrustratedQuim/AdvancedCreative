package com.ratger.acreative.commands.paint

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.paint.PaintMenuCallbacks
import com.ratger.acreative.menus.paint.PaintMenuController
import com.ratger.acreative.menus.paint.PaintToolDefinition
import com.ratger.acreative.menus.paint.PaintToolInventoryService
import com.ratger.acreative.menus.paint.PaintToolMarker
import com.ratger.acreative.commands.paint.agreement.PaintRuleConfirmationService
import com.ratger.acreative.commands.paint.artwork.PaintArtworkService
import com.ratger.acreative.commands.paint.interaction.PaintDirectUseController
import com.ratger.acreative.commands.paint.interaction.PaintDirectUseSuppression
import com.ratger.acreative.commands.paint.interaction.HitDetectionService
import com.ratger.acreative.commands.paint.interaction.PaintInventoryInteractionGuard
import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintCanvasSize
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.moderation.PaintBanController
import com.ratger.acreative.integration.plotsquared.PlotSquaredFlagGate
import com.ratger.acreative.integration.plotsquared.flags.PlotPaintFlag
import com.ratger.acreative.commands.paint.rendering.CanvasCellFactory
import com.ratger.acreative.commands.paint.rendering.CanvasRenderer
import com.ratger.acreative.commands.paint.rendering.EntityVisualFactory
import com.ratger.acreative.commands.paint.rendering.MapDataSender
import com.ratger.acreative.commands.paint.rendering.PaintCanvasPlacementService
import com.ratger.acreative.commands.paint.rendering.PaintCanvasPixelProjector
import com.ratger.acreative.commands.paint.rendering.PaintPreviewCoordinator
import com.ratger.acreative.commands.paint.rendering.PaintViewerTracker
import com.ratger.acreative.commands.paint.rendering.ViewerManager
import com.ratger.acreative.commands.paint.resize.PaintResizeService
import com.ratger.acreative.commands.paint.resize.PaintResizeTargetResolver
import com.ratger.acreative.commands.paint.session.PaintSessionManager
import com.ratger.acreative.commands.paint.session.PaintSessionRegistry
import com.ratger.acreative.commands.paint.session.PaintSessionStarter
import com.ratger.acreative.commands.paint.session.PaintSessionTerminator
import com.ratger.acreative.commands.paint.tools.BrushStrokeResolver
import com.ratger.acreative.commands.paint.tools.FillComponentResolver
import com.ratger.acreative.commands.paint.tools.HistoryManager
import com.ratger.acreative.commands.paint.tools.PaintColorResolver
import com.ratger.acreative.commands.paint.tools.PaintToolApplier
import com.ratger.acreative.commands.paint.tools.ShapeSurfaceResolver
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack as BukkitItemStack
import ru.violence.coreapi.bukkit.api.menu.Menu
import java.util.UUID

class PaintManager(private val hooker: FunctionHooker) {

    private val sessionManager = PaintSessionManager(PaintSessionRegistry())
    private val hitDetectionService = HitDetectionService()
    private val canvasPlacementService = PaintCanvasPlacementService()
    private val resizeTargetResolver = PaintResizeTargetResolver(
        hitDetectionService = hitDetectionService,
        maxCanvasSide = MAX_CANVAS_SIDE,
        resolveCellLocation = canvasPlacementService::resolveCellLocation,
        isEmptyFrameSpace = { player, location -> isEmptyFrameSpace(player, location) }
    )
    private val entityVisualFactory = EntityVisualFactory()
    private val viewerManager = ViewerManager(entityVisualFactory)
    private val mapDataSender = MapDataSender(hooker)
    private val viewerTracker = PaintViewerTracker(hooker, viewerManager, mapDataSender)
    private val canvasCellFactory = CanvasCellFactory(entityVisualFactory, mapDataSender)
    private val canvasRenderer = CanvasRenderer(canvasCellFactory, mapDataSender, ::isFrameSpaceAvailable)
    private val brushStrokeResolver = BrushStrokeResolver()
    private val fillComponentResolver = FillComponentResolver()
    private val shapeSurfaceResolver = ShapeSurfaceResolver()
    private val paintColorResolver = PaintColorResolver()
    private val canvasPixelProjector = PaintCanvasPixelProjector(canvasRenderer, paintColorResolver)
    private val directUseSuppression = PaintDirectUseSuppression()
    private val previewCoordinator = PaintPreviewCoordinator(
        hitDetectionService = hitDetectionService,
        mapDataSender = mapDataSender,
        brushStrokeResolver = brushStrokeResolver,
        fillComponentResolver = fillComponentResolver,
        shapeSurfaceResolver = shapeSurfaceResolver,
        canvasPixelProjector = canvasPixelProjector,
        toolResolver = ::resolveTool,
        isDirectUseSuppressed = directUseSuppression::isSuppressed,
        backgroundColorId = BACKGROUND_COLOR_ID
    )
    private val historyManager = HistoryManager(canvasPixelProjector, previewCoordinator, mapDataSender)
    private val parser = MiniMessageParser()
    private val artworkService = PaintArtworkService(hooker, parser)
    private val banController = PaintBanController(hooker, parser, PAINT_USERS_PAGE_SIZE)
    private val plotSquaredGate = PlotSquaredFlagGate(hooker.plugin.server.pluginManager)
    private val agreementService = PaintRuleConfirmationService(
        hooker = hooker,
        parser = parser,
        repository = banController.repository,
        onConfirmed = ::handleConfirmedPaintSession
    )

    private val toolInventoryService = PaintToolInventoryService(PaintToolMarker.key(hooker.plugin), parser)
    private val menuController: PaintMenuController = PaintMenuController(
        hooker = hooker,
        parser = parser,
        callbacks = object : PaintMenuCallbacks {
            override fun isPainting(player: Player): Boolean = this@PaintManager.isPainting(player)

            override fun session(playerId: UUID): PaintSession? = sessionManager.getSession(playerId)

            override fun refreshTools(player: Player, session: PaintSession) {
                toolInventoryService.refresh(player, session)
            }

            override fun resolveTool(item: BukkitItemStack?): PaintToolDefinition? = this@PaintManager.resolveTool(item)

            override fun clearCanvas(player: Player, session: PaintSession) {
                this@PaintManager.clearCanvas(player, session)
            }

            override fun removeResizePreview(player: Player, session: PaintSession) {
                this@PaintManager.removeResizePreview(player, session)
            }

            override fun beginResizeMode(player: Player, session: PaintSession) {
                this@PaintManager.beginResizeMode(player, session)
            }

            override fun handleEaselMenuClose(player: Player, session: PaintSession) {
                this@PaintManager.handleEaselMenuClose(player, session)
            }
        }
    )
    private val inventoryInteractionGuard = PaintInventoryInteractionGuard(
        sessionManager = sessionManager,
        toolInventoryService = toolInventoryService,
        menuController = menuController,
        handleDropAction = ::handleDropAction
    )

    private val resizeService: PaintResizeService = PaintResizeService(
        resizeTargetResolver = resizeTargetResolver,
        canvasRenderer = canvasRenderer,
        entityVisualFactory = entityVisualFactory,
        mapDataSender = mapDataSender,
        previewCoordinator = previewCoordinator,
        openEaselMenu = { player: Player, session: PaintSession -> menuController.openEaselMenu(player, session) },
        applyCanvasZoom = ::applyCanvasZoom,
        normalizeSelectedZoom = ::normalizeSelectedZoom,
        clearHistory = previewCoordinator::clearHistoryState,
        backgroundColorId = BACKGROUND_COLOR_ID
    )
    private val toolApplier: PaintToolApplier = PaintToolApplier(
        brushStrokeResolver = brushStrokeResolver,
        fillComponentResolver = fillComponentResolver,
        shapeSurfaceResolver = shapeSurfaceResolver,
        paintColorResolver = paintColorResolver,
        canvasPixelProjector = canvasPixelProjector,
        previewCoordinator = previewCoordinator,
        historyManager = historyManager,
        openEaselMenu = { player: Player, session: PaintSession -> menuController.openEaselMenu(player, session) },
        backgroundColorId = BACKGROUND_COLOR_ID
    )
    private val directUseController = PaintDirectUseController(
        sessionManager = sessionManager,
        hitDetectionService = hitDetectionService,
        toolInventoryService = toolInventoryService,
        menuController = menuController,
        previewCoordinator = previewCoordinator,
        resizeService = resizeService,
        historyManager = historyManager,
        toolApplier = toolApplier,
        directUseSuppression = directUseSuppression
    )
    private val sessionStarter = PaintSessionStarter(
        hooker = hooker,
        sessionManager = sessionManager,
        entityVisualFactory = entityVisualFactory,
        toolInventoryService = toolInventoryService,
        canvasPlacementService = canvasPlacementService,
        isEmptyFrameSpace = ::isEmptyFrameSpace,
        resolveVisibleViewers = viewerTracker::resolveVisibleViewers,
        sendFullMapDataToViewers = mapDataSender::sendToViewers,
        startViewerTask = ::startViewerTask,
        startPreviewTask = ::startPreviewTask,
        scheduleDelayedMapDataRefresh = ::scheduleDelayedMapDataRefresh
    )
    private val sessionTerminator = PaintSessionTerminator(
        hooker = hooker,
        sessionManager = sessionManager,
        previewCoordinator = previewCoordinator,
        resizeService = resizeService,
        entityVisualFactory = entityVisualFactory,
        toolInventoryService = toolInventoryService,
        artworkService = artworkService,
        applyCanvasZoom = ::applyCanvasZoom
    )

    fun registerPlotSquaredFlagIntegration() {
        plotSquaredGate.registerFlagIfNeeded(PlotPaintFlag.TRUE)
    }

    fun toggleUserBan(player: Player, targetName: String, reason: String?) {
        banController.toggleUserBan(player, targetName, reason)
    }

    fun openBannedUsers(player: Player, requestedPage: Int = 1, currentMenu: Menu? = null) {
        banController.openBannedUsers(player, requestedPage, currentMenu)
    }

    fun shutdown() {
        banController.shutdown()
    }

    fun handlePaintCommand(player: Player, args: Array<out String>) {
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.USAGE_PAINT)
            return
        }

        val requestedSize = PaintCanvasSize.parse(args.firstOrNull())
        if (requestedSize == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_INVALID_SIZE)
            return
        }

        if (isPainting(player)) {
            stopPainting(player)
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_OFF)
            return
        }

        if (!canActivatePaint(player)) {
            return
        }

        if (banController.isBanned(player.uniqueId)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_USER_BANNED)
            return
        }

        agreementService.requestPaintSession(player, requestedSize)
    }

    private fun handleConfirmedPaintSession(player: Player, requestedSize: PaintCanvasSize) {
        if (isPainting(player)) {
            return
        }
        if (!canActivatePaint(player)) {
            return
        }
        if (banController.isBanned(player.uniqueId)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_USER_BANNED)
            return
        }
        if (isPaintBlockedByPlotFlag(player)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_BLOCKED_ZONE)
            return
        }
        if (sessionStarter.startPainting(player, requestedSize)) {
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_ON)
        }
    }

    fun isPainting(player: Player): Boolean = sessionManager.hasSession(player.uniqueId)

    fun shouldCancelWorldAction(player: Player): Boolean {
        return directUseController.shouldCancelWorldAction(player)
    }

    fun handleFrameInteraction(entityId: Int): Boolean = sessionManager.getAllSessions().any { session ->
        session.canvasCells.values.any { it.frame.entityId == entityId } ||
            session.resizePreview?.frame?.entityId == entityId
    }

    fun handleFrameUse(player: Player, entityId: Int): Boolean {
        val session = sessionManager.getAllSessions().firstOrNull { current ->
            current.canvasCells.values.any { it.frame.entityId == entityId } ||
                current.resizePreview?.frame?.entityId == entityId
        } ?: return false

        if (session.playerId != player.uniqueId) return true
        if (session.resizePreview?.frame?.entityId == entityId) {
            resizeService.handleResizeActivation(player, session)
            return true
        }
        handleDirectUse(player)
        return true
    }

    fun handleSwing(player: Player): Boolean = handleDirectUse(player)

    fun handleInteract(player: Player): Boolean = handleDirectUse(player)

    fun handleLeftClick(player: Player): Boolean = handleDirectUse(player)

    fun handleDropAction(player: Player, stackDrop: Boolean): Boolean {
        return directUseController.handleDropAction(player, stackDrop)
    }

    fun handleInventoryClick(event: InventoryClickEvent) {
        inventoryInteractionGuard.handleInventoryClick(event)
    }

    fun handleInventoryDrag(event: InventoryDragEvent) {
        inventoryInteractionGuard.handleInventoryDrag(event)
    }

    fun handleInventoryClose(event: InventoryCloseEvent) {
        inventoryInteractionGuard.handleInventoryClose(event)
    }

    fun handlePlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!isPainting(player)) return

        stopPainting(player)
        event.keepInventory = true
        event.drops.clear()
    }


    fun stopPaintingIfTooFar(player: Player): Boolean {
        val session = sessionManager.getSession(player.uniqueId) ?: return false
        val distanceSquared = player.location.distanceSquared(session.anchorLocation)
        if (distanceSquared <= MAX_SESSION_DISTANCE_BLOCKS * MAX_SESSION_DISTANCE_BLOCKS) {
            return false
        }
        stopPainting(player)
        hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_OFF)
        return true
    }

    fun stopPaintingFromPlotCommand(player: Player, message: String) {
        if (!isPainting(player)) return
        val normalized = message.trim().lowercase()
        if (normalized.startsWith("/p kick") || normalized.startsWith("/plot kick") ||
            normalized.startsWith("/p ban") || normalized.startsWith("/plot ban")) {
            stopPainting(player)
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_OFF)
        }
    }

    fun cleanupSessionsForPlayer(player: Player) {
        stopPainting(player)
        agreementService.clearRuntimeState(player.uniqueId)
    }

    fun stopPainting(player: Player) {
        sessionTerminator.stopPainting(player)
    }

    fun releaseAll() {
        agreementService.releaseAll()
        sessionTerminator.releaseAll()
    }


    private fun isPaintBlockedByPlotFlag(player: Player): Boolean {
        return plotSquaredGate.isUsageForbidden(player, PlotPaintFlag.TRUE, PAINT_BYPASS_PERMISSION)
    }

    private fun canActivatePaint(player: Player): Boolean {
        if (!hooker.accountLinkRequirementService.hasRequiredLink(player)) {
            hooker.accountLinkRequirementService.sendLinkRequiredMessage(player)
            return false
        }
        if (!hooker.serverPerformanceService.isStableForTickSensitiveActivation(MIN_PAINT_ACTIVATION_TPS)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_SERVER_UNSTABLE)
            return false
        }
        return true
    }

    private fun removeResizePreview(player: Player, session: PaintSession) {
        resizeService.removeResizePreview(player, session)
    }

    private fun beginResizeMode(player: Player, session: PaintSession) {
        resizeService.beginResizeMode(player, session)
    }

    private fun handleEaselMenuClose(player: Player, session: PaintSession) {
        if (session.selectedZoom == session.appliedZoom) return
        if (!applyCanvasZoom(player, session, session.selectedZoom)) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_NO_SPACE)
            session.selectedZoom = session.appliedZoom
        }
    }

    private fun applyCanvasZoom(
        player: Player,
        session: PaintSession,
        targetZoom: Int
    ): Boolean {
        val normalizedZoom = targetZoom.coerceIn(1, MAX_CANVAS_SIDE)
        if (!canApplyZoom(session, normalizedZoom)) {
            return false
        }
        if (session.appliedZoom == normalizedZoom && session.canvasCells.isNotEmpty()) {
            return true
        }

        previewCoordinator.restoreIfNeeded(player, session)
        resizeService.removeResizePreview(player, session)
        if (!canvasRenderer.renderCanvas(player, session, normalizedZoom)) {
            return false
        }

        session.appliedZoom = normalizedZoom
        previewCoordinator.clearStrokeState(session)
        previewCoordinator.clearSuppression(session)
        normalizeSelectedZoom(session)
        previewCoordinator.markCanvasTopologyChanged(session)
        return true
    }

    private fun canApplyZoom(session: PaintSession, targetZoom: Int): Boolean {
        return targetZoom == 1 || session.maxLogicalSide() * targetZoom <= MAX_CANVAS_SIDE
    }

    private fun normalizeSelectedZoom(session: PaintSession) {
        if (!canApplyZoom(session, session.selectedZoom)) {
            session.selectedZoom = 1
        }
    }

    private fun isFrameSpaceAvailable(player: Player, session: PaintSession, location: Location): Boolean {
        if (plotSquaredGate.isUsageForbidden(player, location, PlotPaintFlag.TRUE, PAINT_BYPASS_PERMISSION)) return false
        if (!location.block.type.isAir) return false
        return sessionManager.getAllSessions()
            .asSequence()
            .filter { it !== session }
            .flatMap { it.canvasCells.values.asSequence() }
            .none { other ->
                other.location.world == location.world &&
                    other.location.distanceSquared(location) < LOCATION_EPSILON
            }
    }

    private fun handleDirectUse(player: Player): Boolean {
        return directUseController.handleDirectUse(player)
    }

    private fun startViewerTask(playerId: UUID): Int {
        var taskId = -1
        taskId = hooker.tickScheduler.runRepeating(VIEWER_UPDATE_PERIOD_TICKS, VIEWER_UPDATE_PERIOD_TICKS) {
            val session = sessionManager.getSession(playerId) ?: run {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            val owner = Bukkit.getPlayer(playerId)
            if (owner == null || !owner.isOnline || owner.isDead) {
                owner?.let(::stopPainting)
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            viewerTracker.refreshViewers(owner, session)
        }
        return taskId
    }

    private fun startPreviewTask(playerId: UUID): Int {
        var taskId = -1
        taskId = hooker.tickScheduler.runRepeating(1L, 1L) {
            val session = sessionManager.getSession(playerId) ?: run {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            session.currentTick += 1
            val player = Bukkit.getPlayer(playerId) ?: return@runRepeating
            if (session.resizeMode) {
                previewCoordinator.restoreIfNeeded(player, session)
                resizeService.updateResizePreview(player, session)
                return@runRepeating
            }
            previewCoordinator.updatePreview(player, session)
        }
        return taskId
    }

    fun resyncHeldToolSlot(player: Player) {
        directUseController.resyncHeldToolSlot(player)
    }

    fun handleHeldToolChange(player: Player) {
        directUseController.handleHeldToolChange(player)
    }

    fun suppressDirectUseAfterDrop(player: Player) {
        directUseController.suppressDirectUseAfterDrop(player)
    }

    private fun isEmptyFrameSpace(player: Player, location: Location): Boolean {
        if (plotSquaredGate.isUsageForbidden(player, location, PlotPaintFlag.TRUE, PAINT_BYPASS_PERMISSION)) return false
        if (!location.block.type.isAir) return false
        return sessionManager.getAllSessions()
            .flatMap { it.canvasCells.values }
            .none { other ->
                other.location.world == location.world &&
                    other.location.distanceSquared(location) < LOCATION_EPSILON
            }
    }

    private fun clearCanvas(player: Player, session: PaintSession) {
        previewCoordinator.restoreIfNeeded(player, session)
        var changed = false
        session.logicalCells.values.forEach { colors ->
            var localChanged = false
            colors.indices.forEach { index ->
                if (colors[index] != BACKGROUND_COLOR_ID) {
                    colors[index] = BACKGROUND_COLOR_ID
                    localChanged = true
                }
            }
            changed = changed || localChanged
        }
        val patches = canvasRenderer.rerenderLogicalCells(session, session.logicalCells.keys)
        if (changed) {
            previewCoordinator.markCanvasChanged(session)
            MenuSoundSupport.itemFrameRemoveItem(player)
        }
        patches.forEach { patch ->
            mapDataSender.sendPatchesToViewers(session.viewers, listOf(patch))
        }
        previewCoordinator.clearStrokeState(session)
        previewCoordinator.clearHistoryState(session)
    }

    private fun scheduleDelayedMapDataRefresh(playerId: UUID, mapId: Int, viewerIds: Set<UUID>) {
        hooker.tickScheduler.runLater(1L) {
            val session = sessionManager.getSession(playerId) ?: return@runLater
            if (session.canvasCells.values.none { it.mapId == mapId }) return@runLater
            val snapshot = MapDataExtractor.extract(mapId) ?: return@runLater
            viewerIds.forEach { viewerId ->
                Bukkit.getPlayer(viewerId)?.let { viewer ->
                    mapDataSender.send(viewer, snapshot)
                }
            }
        }
    }

    private fun resolveTool(item: BukkitItemStack?): PaintToolDefinition? = toolInventoryService.resolve(item)

    companion object {
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
        private const val PAINT_USERS_PAGE_SIZE = 45
        private const val VIEWER_UPDATE_PERIOD_TICKS = 10L
        private const val DIRECT_USE_DEBOUNCE_MILLIS = 65L
        private const val DIRECT_USE_SUPPRESS_AFTER_DROP_MILLIS = 250L
        private const val DROP_ACTION_DEBOUNCE_MILLIS = 65L
        private const val PALETTE_ROTATION_DEBOUNCE_MILLIS = 100L
        private const val LOCATION_EPSILON = 0.01
        private const val MAX_CANVAS_SIDE = 4
        private const val MAX_SESSION_DISTANCE_BLOCKS = 200.0
        private const val PAINT_BYPASS_PERMISSION = "acreative.paint.bypass"
        private const val MIN_PAINT_ACTIVATION_TPS = 18.0
        private val BACKGROUND_COLOR_ID: Byte = MapDataExtractor.DEFAULT_CANVAS_COLOR_ID
    }
}
