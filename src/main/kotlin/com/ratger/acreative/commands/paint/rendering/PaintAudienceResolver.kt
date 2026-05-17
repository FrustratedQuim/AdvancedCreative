package com.ratger.acreative.commands.paint.rendering

import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class PaintAudienceResolver(
    private val hooker: FunctionHooker
) {

    fun resolveViewerIds(owner: Player, locations: Collection<Location>): MutableSet<UUID> {
        return resolveVisiblePlayers(owner, locations)
            .mapTo(linkedSetOf()) { it.uniqueId }
    }

    fun resolveTrackedViewers(session: PaintSession): List<Player> {
        return resolveTrackedViewers(session, session.canvasCells.values.map { it.location })
    }

    fun resolveTrackedViewers(session: PaintSession, locations: Collection<Location>): List<Player> {
        if (session.viewers.isEmpty()) return emptyList()
        val owner = Bukkit.getPlayer(session.playerId) ?: return emptyList()
        val trackedViewerIds = session.viewers
        return resolveVisiblePlayers(owner, locations)
            .filter { it.uniqueId in trackedViewerIds }
    }

    fun resolveTrackedViewerIds(session: PaintSession, locations: Collection<Location>): MutableSet<UUID> {
        return resolveTrackedViewers(session, locations)
            .mapTo(linkedSetOf()) { it.uniqueId }
    }

    fun resolveVisiblePlayers(owner: Player, locations: Collection<Location>): List<Player> {
        if (locations.isEmpty()) return emptyList()

        val chunkKeys = locations.asSequence()
            .filter { location -> location.world == owner.world }
            .map(::ChunkKey)
            .distinct()
            .toList()
        if (chunkKeys.isEmpty()) return emptyList()

        val viewersById = linkedMapOf<UUID, Player>()
        chunkKeys.forEach { chunkKey ->
            owner.world.getPlayersSeeingChunk(chunkKey.x, chunkKey.z).forEach { viewer ->
                if (!viewer.isOnline) return@forEach
                if (hooker.utils.isHiddenFromPlayer(viewer, owner)) return@forEach
                viewersById.putIfAbsent(viewer.uniqueId, viewer)
            }
        }
        return viewersById.values.toList()
    }

    private data class ChunkKey(
        val x: Int,
        val z: Int
    ) {
        constructor(location: Location) : this(location.blockX shr 4, location.blockZ shr 4)
    }
}
