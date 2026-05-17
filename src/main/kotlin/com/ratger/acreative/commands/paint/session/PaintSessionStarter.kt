package com.ratger.acreative.commands.paint.session

import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintCanvasCell
import com.ratger.acreative.commands.paint.model.PaintCanvasSize
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintInventorySnapshot
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.rendering.EntityVisualFactory
import com.ratger.acreative.commands.paint.rendering.PaintCanvasPlacementService
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.paint.PaintToolInventoryService
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import com.ratger.acreative.utils.SeriesCodeGenerator
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class PaintSessionStarter(
    private val hooker: FunctionHooker,
    private val sessionManager: PaintSessionManager,
    private val entityVisualFactory: EntityVisualFactory,
    private val toolInventoryService: PaintToolInventoryService,
    private val canvasPlacementService: PaintCanvasPlacementService,
    private val isEmptyFrameSpace: (Player, Location) -> Boolean,
    private val resolveVisibleViewers: (Player, Location) -> List<Player>,
    private val sendFullMapDataToViewers: (Collection<UUID>, MapDataExtractor.Snapshot) -> Unit,
    private val startViewerTask: (UUID) -> Int,
    private val startPreviewTask: (UUID) -> Int,
    private val scheduleDelayedMapDataRefresh: (UUID, Int, Set<UUID>) -> Unit
) {

    fun startPainting(player: Player, size: PaintCanvasSize): Boolean {
        val frameDirection = canvasPlacementService.resolveDirection(player)
        val anchorLocation = canvasPlacementService.resolveFrameLocation(player, frameDirection)
        val points = size.initialPoints()
        val frameLocations = points.associateWith { point ->
            canvasPlacementService.resolveCellLocation(anchorLocation, size.basePoint, point, frameDirection)
        }

        if (frameLocations.values.any { !isEmptyFrameSpace(player, it) }) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_NO_SPACE)
            return false
        }

        val mapSnapshots = linkedMapOf<PaintGridPoint, MapDataExtractor.Snapshot>()
        points.forEach { point ->
            val snapshot = MapDataExtractor.create(player.world)
            if (snapshot == null) {
                hooker.messageManager.sendChat(
                    player,
                    MessageKey.ERROR_PAINT_MAP_MISSING,
                    mapOf("map" to "карта мира ${player.world.name}")
                )
                return false
            }
            mapSnapshots[point] = snapshot
        }

        val inventorySnapshot = PaintInventorySnapshot.capture(player)
        val visibleViewers = resolveVisibleViewers(player, anchorLocation).mapTo(mutableSetOf()) { it.uniqueId }
        mapSnapshots.values.forEach { snapshot ->
            sendFullMapDataToViewers(visibleViewers, snapshot)
        }

        val canvasCells = linkedMapOf<PaintGridPoint, PaintCanvasCell>()
        val logicalCells = linkedMapOf<PaintGridPoint, ByteArray>()
        mapSnapshots.forEach { (point, snapshot) ->
            val location = frameLocations.getValue(point)
            val visuals = entityVisualFactory.createCanvasCellVisuals(snapshot.mapId, frameDirection, visibleViewers)
            if (!entityVisualFactory.spawnVisuals(visuals, location)) {
                canvasCells.values.forEach(entityVisualFactory::removeVisuals)
                entityVisualFactory.removeVisuals(visuals)
                return false
            }
            canvasCells[point] = PaintCanvasCell(point, snapshot.mapId, visuals.frame, visuals.backPanel, location)
            logicalCells[point] = snapshot.colors.copyOf()
        }

        val session = PaintSession(
            playerId = player.uniqueId,
            frameDirection = frameDirection,
            anchorLocation = anchorLocation,
            anchorPoint = size.basePoint,
            initialSize = size,
            inventorySnapshot = inventorySnapshot,
            viewerTaskId = -1,
            previewTaskId = -1,
            viewers = visibleViewers,
            canvasCells = canvasCells,
            logicalCells = logicalCells,
            seriesCode = SeriesCodeGenerator.generate()
        )

        sessionManager.registerSession(session)
        hooker.playerStateManager.activateState(player, PlayerStateType.PAINTING)
        player.gameMode = GameMode.CREATIVE
        toolInventoryService.prepare(player, session)

        session.viewerTaskId = startViewerTask(player.uniqueId)
        session.previewTaskId = startPreviewTask(player.uniqueId)

        mapSnapshots.values.forEach { snapshot ->
            scheduleDelayedMapDataRefresh(session.playerId, snapshot.mapId, session.viewers.toSet())
        }
        return true
    }
}
