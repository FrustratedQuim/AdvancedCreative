package com.ratger.acreative.commands.paint.rendering

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData
import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintSession
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class MapDataSender(
    private val audienceResolver: PaintAudienceResolver
) {
    fun send(player: Player, snapshot: MapDataExtractor.Snapshot) {
        val packet = WrapperPlayServerMapData(
            snapshot.mapId,
            snapshot.scale,
            false,
            snapshot.locked,
            null,
            MAP_WIDTH,
            MAP_HEIGHT,
            0,
            0,
            snapshot.colors.copyOf()
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    fun send(players: Iterable<Player>, snapshot: MapDataExtractor.Snapshot) {
        players.forEach { player -> send(player, snapshot) }
    }

    fun send(player: Player, patch: MapDataExtractor.Patch) {
        val packet = WrapperPlayServerMapData(
            patch.mapId,
            patch.scale,
            false,
            patch.locked,
            null,
            patch.width,
            patch.height,
            patch.startX,
            patch.startY,
            patch.colors
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    fun sendPatches(players: Iterable<Player>, patches: List<MapDataExtractor.Patch>) {
        if (patches.isEmpty()) return
        players.forEach { player ->
            patches.forEach { patch -> send(player, patch) }
        }
    }

    fun sendToViewers(viewerIds: Collection<UUID>, snapshot: MapDataExtractor.Snapshot) {
        send(resolveOnlinePlayers(viewerIds), snapshot)
    }

    fun sendPatchesToViewers(viewerIds: Collection<UUID>, patches: List<MapDataExtractor.Patch>) {
        sendPatches(resolveOnlinePlayers(viewerIds), patches)
    }

    fun sendToSessionViewers(session: PaintSession, snapshot: MapDataExtractor.Snapshot) {
        sendToSessionViewers(session, snapshot, null)
    }

    fun sendPatchesToSessionViewers(session: PaintSession, patches: List<MapDataExtractor.Patch>) {
        patches.forEach { patch ->
            sendPatches(resolveMapViewers(session, patch.mapId), listOf(patch))
        }
    }

    fun sendToSessionViewers(session: PaintSession, snapshot: MapDataExtractor.Snapshot, location: Location?) {
        send(resolveMapViewers(session, snapshot.mapId, location), snapshot)
    }

    fun resolveSessionViewerIds(session: PaintSession, location: Location): MutableSet<UUID> {
        return audienceResolver.resolveTrackedViewerIds(session, listOf(location))
    }

    private fun resolveOnlinePlayers(viewerIds: Collection<UUID>): List<Player> {
        return viewerIds.mapNotNull(Bukkit::getPlayer)
    }

    private fun resolveMapViewers(session: PaintSession, mapId: Int, location: Location? = null): List<Player> {
        val targetLocation = location ?: session.canvasCells.values.firstOrNull { cell -> cell.mapId == mapId }?.location
        return if (targetLocation == null) {
            audienceResolver.resolveTrackedViewers(session)
        } else {
            audienceResolver.resolveTrackedViewers(session, listOf(targetLocation))
        }
    }

    private companion object {
        const val MAP_WIDTH = 128
        const val MAP_HEIGHT = 128
    }
}
