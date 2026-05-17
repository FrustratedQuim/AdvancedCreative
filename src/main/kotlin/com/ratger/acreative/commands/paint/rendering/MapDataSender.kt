package com.ratger.acreative.commands.paint.rendering

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData
import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class MapDataSender(
    @Suppress("unused") private val hooker: FunctionHooker
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

    fun sendToViewers(viewerIds: Collection<UUID>, snapshot: MapDataExtractor.Snapshot) {
        viewerIds.forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer -> send(viewer, snapshot) }
        }
    }

    fun sendPatchesToViewers(viewerIds: Collection<UUID>, patches: List<MapDataExtractor.Patch>) {
        viewerIds.forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                patches.forEach { patch -> send(viewer, patch) }
            }
        }
    }

    private companion object {
        const val MAP_WIDTH = 128
        const val MAP_HEIGHT = 128
    }
}
