package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintCanvasCell
import com.ratger.acreative.commands.paint.model.PaintFrameDirection
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintSession
import org.bukkit.Location
import java.util.UUID

class CanvasCellFactory(
    private val entityVisualFactory: EntityVisualFactory,
    private val mapDataSender: MapDataSender
) {

    fun createCell(
        snapshot: MapDataExtractor.Snapshot,
        direction: PaintFrameDirection,
        viewerIds: Collection<UUID>,
        location: Location
    ): PaintCanvasCell? {
        val visuals = entityVisualFactory.createCanvasCellVisuals(snapshot.mapId, direction, viewerIds)

        if (!entityVisualFactory.spawnVisuals(visuals, location)) {
            entityVisualFactory.removeVisuals(visuals)
            return null
        }

        mapDataSender.sendToViewers(viewerIds, snapshot)

        return PaintCanvasCell(
            point = PaintGridPoint(0, 0),
            mapId = snapshot.mapId,
            frame = visuals.frame,
            backPanel = visuals.backPanel,
            location = location
        )
    }

    fun createCell(
        point: PaintGridPoint,
        snapshot: MapDataExtractor.Snapshot,
        direction: PaintFrameDirection,
        viewerIds: Collection<UUID>,
        location: Location
    ): PaintCanvasCell? {
        val cell = createCell(snapshot, direction, viewerIds, location) ?: return null
        return PaintCanvasCell(
            point = point,
            mapId = cell.mapId,
            frame = cell.frame,
            backPanel = cell.backPanel,
            location = cell.location
        )
    }

    fun reuseCell(
        session: PaintSession,
        cell: PaintCanvasCell,
        newLocation: Location,
        newColors: ByteArray
    ): PaintCanvasCell {
        entityVisualFactory.teleportCell(cell, newLocation)

        MapDataExtractor.replaceColors(cell.mapId, newColors)?.let { snapshot ->
            mapDataSender.sendToSessionViewers(session, snapshot, newLocation)
        }

        return PaintCanvasCell(
            point = cell.point,
            mapId = cell.mapId,
            frame = cell.frame,
            backPanel = cell.backPanel,
            location = newLocation
        )
    }

    fun removeCell(cell: PaintCanvasCell) {
        entityVisualFactory.removeVisuals(cell)
    }
}
