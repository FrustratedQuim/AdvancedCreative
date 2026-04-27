package com.ratger.acreative.commands.paint

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.saveddata.maps.MapId
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import java.awt.Color

object MapDataExtractor {

    data class Snapshot(
        val mapId: Int,
        val scale: Byte,
        val locked: Boolean,
        val colors: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Snapshot

            if (mapId != other.mapId) return false
            if (scale != other.scale) return false
            if (locked != other.locked) return false
            if (!colors.contentEquals(other.colors)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mapId
            result = 31 * result + scale
            result = 31 * result + locked.hashCode()
            result = 31 * result + colors.contentHashCode()
            return result
        }
    }

    fun create(world: World): Snapshot? {
        val mapView = Bukkit.createMap(world)
        runCatching { mapView.isTrackingPosition = false }
        runCatching { mapView.isLocked = true }
        return fill(mapView.id, MapColor.SNOW.getPackedId(MapColor.Brightness.NORMAL)) ?: extract(mapView.id)
    }

    fun extract(mapId: Int): Snapshot? {
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null
        val colors = data.colors
        if (colors.size != FULL_MAP_SIZE) return null

        return Snapshot(
            mapId = mapId,
            scale = data.scale,
            locked = data.locked,
            colors = colors.copyOf()
        )
    }

    fun fill(mapId: Int, color: Byte): Snapshot? {
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null

        for (y in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                data.setColor(x, y, color)
            }
        }

        return extract(mapId)
    }

    fun copy(sourceMapId: Int, targetMapId: Int): Snapshot? {
        val source = extract(sourceMapId) ?: return null
        val mapView = Bukkit.getMap(targetMapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(targetMapId)) ?: return null

        for (y in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                data.setColor(x, y, source.colors[y * MAP_SIZE + x])
            }
        }

        return extract(targetMapId)
    }

    fun colorAt(mapId: Int, x: Int, y: Int): Byte? {
        if (x !in 0 until MAP_SIZE || y !in 0 until MAP_SIZE) return null
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null
        return data.colors[y * MAP_SIZE + x]
    }

    fun setPixels(mapId: Int, points: Collection<Pair<Int, Int>>, color: Byte): Snapshot? {
        if (points.isEmpty()) return extract(mapId)
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null

        points.forEach { (x, y) ->
            if (x in 0 until MAP_SIZE && y in 0 until MAP_SIZE) {
                data.setColor(x, y, color)
            }
        }

        return extract(mapId)
    }

    fun resolvePaletteColor(color: Byte): Color {
        val packedId = color.toInt() and 0xFF
        return Color(MapColor.getColorFromPackedId(packedId), true)
    }

    private fun serverLevel(world: World): ServerLevel = (world as CraftWorld).handle

    private const val MAP_SIZE = 128
    private const val FULL_MAP_SIZE = 128 * 128
}
