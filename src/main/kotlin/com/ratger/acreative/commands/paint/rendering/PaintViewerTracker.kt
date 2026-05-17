package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintSession
import org.bukkit.entity.Player

class PaintViewerTracker(
    private val audienceResolver: PaintAudienceResolver,
    private val viewerManager: ViewerManager,
    private val mapDataSender: MapDataSender
) {

    fun refreshViewers(owner: Player, session: PaintSession) {
        val viewersByCell = session.canvasCells.values.associateWith { cell ->
            audienceResolver.resolveVisiblePlayers(owner, listOf(cell.location))
        }
        val desiredViewers = viewersByCell.values
            .flatten()
            .associateBy { it.uniqueId }
        val currentViewers = session.viewers.toSet()

        currentViewers
            .filter { it !in desiredViewers }
            .forEach { viewerId -> viewerManager.removeViewer(session, viewerId) }

        val enteringViewers = desiredViewers.values.filter { it.uniqueId !in currentViewers }
        val enteringViewerIds = enteringViewers.mapTo(linkedSetOf()) { it.uniqueId }
        enteringViewers.forEach { viewer ->
            val visibleCells = viewersByCell
                .filterValues { viewers -> viewers.any { visibleViewer -> visibleViewer.uniqueId == viewer.uniqueId } }
                .keys
            viewerManager.addViewer(session, viewer.uniqueId, visibleCells)
        }
        if (enteringViewers.isEmpty()) return

        viewersByCell.forEach { (cell, viewers) ->
            val viewersForCell = viewers.filter { viewer ->
                viewer.uniqueId in enteringViewerIds
            }
            if (viewersForCell.isEmpty()) return@forEach

            MapDataExtractor.extract(cell.mapId)?.let { snapshot ->
                mapDataSender.send(viewersForCell, snapshot)
            }
        }
    }
}
