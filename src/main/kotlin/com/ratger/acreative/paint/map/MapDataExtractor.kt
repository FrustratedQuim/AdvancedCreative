package com.ratger.acreative.paint.map

import com.ratger.acreative.paint.model.PaintPixelChange
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.saveddata.maps.MapId
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import java.awt.Color

object MapDataExtractor {
    val DEFAULT_CANVAS_COLOR_ID: Byte = MapColor.SNOW.getPackedId(MapColor.Brightness.NORMAL)

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

    data class Patch(
        val mapId: Int,
        val scale: Byte,
        val locked: Boolean,
        val width: Int,
        val height: Int,
        val startX: Int,
        val startY: Int,
        val colors: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Patch

            if (mapId != other.mapId) return false
            if (scale != other.scale) return false
            if (locked != other.locked) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (startX != other.startX) return false
            if (startY != other.startY) return false
            if (!colors.contentEquals(other.colors)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mapId
            result = 31 * result + scale
            result = 31 * result + locked.hashCode()
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + startX
            result = 31 * result + startY
            result = 31 * result + colors.contentHashCode()
            return result
        }
    }

    fun create(world: World): Snapshot? = createFilled(world, DEFAULT_CANVAS_COLOR_ID)

    fun createFilled(world: World, color: Byte): Snapshot? {
        val mapView = Bukkit.createMap(world)
        runCatching { mapView.isTrackingPosition = false }
        runCatching { mapView.isLocked = true }
        return fill(mapView.id, color) ?: extract(mapView.id)
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

    fun colorsView(mapId: Int): ByteArray? {
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null
        return data.colors.takeIf { it.size == FULL_MAP_SIZE }
    }

    fun setPixelChangesPatch(mapId: Int, changes: List<PaintPixelChange>, useOldColor: Boolean = false): Patch? {
        if (changes.isEmpty()) return null
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null

        var minX = MAP_SIZE
        var minY = MAP_SIZE
        var maxX = -1
        var maxY = -1
        changes.forEach { change ->
            if (change.x in 0 until MAP_SIZE && change.y in 0 until MAP_SIZE) {
                data.setColor(change.x, change.y, if (useOldColor) change.oldColor else change.newColor)
                minX = minOf(minX, change.x)
                minY = minOf(minY, change.y)
                maxX = maxOf(maxX, change.x)
                maxY = maxOf(maxY, change.y)
            }
        }

        if (maxX < minX || maxY < minY) return null
        return buildPatch(mapId, data.scale, data.locked, data.colors, minX, minY, maxX, maxY)
    }

    fun extractPatch(mapId: Int, indices: IntArray, overrideColors: ByteArray? = null): Patch? {
        if (indices.isEmpty()) return null
        val mapView = Bukkit.getMap(mapId) ?: return null
        val world = mapView.world ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val data = serverLevel(world).getMapData(MapId(mapId)) ?: return null

        var minX = MAP_SIZE
        var minY = MAP_SIZE
        var maxX = -1
        var maxY = -1
        indices.forEach { index ->
            if (index !in 0 until FULL_MAP_SIZE) return@forEach
            val x = index % MAP_SIZE
            val y = index / MAP_SIZE
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
        }

        if (maxX < minX || maxY < minY) return null
        val patch = buildPatch(mapId, data.scale, data.locked, data.colors, minX, minY, maxX, maxY)
        if (overrideColors == null) return patch

        indices.forEachIndexed { offset, index ->
            if (index !in 0 until FULL_MAP_SIZE || offset !in overrideColors.indices) return@forEachIndexed
            val x = index % MAP_SIZE
            val y = index / MAP_SIZE
            patch.colors[(y - patch.startY) * patch.width + (x - patch.startX)] = overrideColors[offset]
        }
        return patch
    }

    fun resolvePaletteColor(color: Byte): Color {
        val packedId = color.toInt() and 0xFF
        return Color(MapColor.getColorFromPackedId(packedId), true)
    }

    private fun buildPatch(
        mapId: Int,
        scale: Byte,
        locked: Boolean,
        sourceColors: ByteArray,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): Patch {
        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val colors = ByteArray(width * height)
        for (row in 0 until height) {
            sourceColors.copyInto(
                destination = colors,
                destinationOffset = row * width,
                startIndex = (minY + row) * MAP_SIZE + minX,
                endIndex = (minY + row) * MAP_SIZE + minX + width
            )
        }
        return Patch(
            mapId = mapId,
            scale = scale,
            locked = locked,
            width = width,
            height = height,
            startX = minX,
            startY = minY,
            colors = colors
        )
    }

    private fun serverLevel(world: World): ServerLevel = (world as CraftWorld).handle

    private const val MAP_SIZE = 128
    private const val FULL_MAP_SIZE = 128 * 128
}
