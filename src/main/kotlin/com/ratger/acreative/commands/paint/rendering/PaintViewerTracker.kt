package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

class PaintViewerTracker(
    private val hooker: FunctionHooker,
    private val viewerManager: ViewerManager,
    private val mapDataSender: MapDataSender
) {

    fun refreshViewers(owner: Player, session: PaintSession) {
        val desiredViewers = resolveVisibleViewers(owner, session.anchorLocation).associateBy { it.uniqueId }
        val currentViewers = session.viewers.toSet()

        currentViewers
            .filter { it !in desiredViewers }
            .forEach { viewerId -> viewerManager.removeViewer(session, viewerId) }

        val enteringViewers = desiredViewers.values.filter { it.uniqueId !in currentViewers }
        enteringViewers.forEach { viewer ->
            session.canvasCells.values.forEach { cell ->
                MapDataExtractor.extract(cell.mapId)?.let { snapshot ->
                    mapDataSender.send(viewer, snapshot)
                }
            }
        }
        enteringViewers.forEach { viewer -> viewerManager.addViewer(session, viewer.uniqueId) }
    }

    fun resolveVisibleViewers(owner: Player, frameLocation: Location): List<Player> {
        val visibilityRadius = resolveVisibilityRadius()
        return frameLocation.world?.players
            ?.filter { viewer ->
                viewer.isOnline &&
                    viewer.world == owner.world &&
                    viewer.location.distanceSquared(frameLocation) <= visibilityRadius * visibilityRadius &&
                    !hooker.utils.isHiddenFromPlayer(viewer, owner)
            }
            ?: emptyList()
    }

    private fun resolveVisibilityRadius(): Double {
        return (Bukkit.getViewDistance() * CHUNK_SIZE).coerceAtLeast(MIN_VISIBILITY_RADIUS)
    }

    private companion object {
        private const val CHUNK_SIZE = 16.0
        private const val MIN_VISIBILITY_RADIUS = 32.0
    }
}
