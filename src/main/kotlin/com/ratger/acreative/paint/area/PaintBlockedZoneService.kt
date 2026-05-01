package com.ratger.acreative.paint.area

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import kotlin.math.max
import kotlin.math.min

class PaintBlockedZoneService(section: ConfigurationSection?) {

    private val zones: List<PaintBlockedZone> = loadZones(section)

    fun isBlocked(location: Location): Boolean {
        if (zones.isEmpty()) return false

        val worldName = location.world?.name
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        for (zone in zones) {
            if (zone.contains(worldName, x, y, z)) {
                return true
            }
        }
        return false
    }

    private fun loadZones(section: ConfigurationSection?): List<PaintBlockedZone> {
        if (section == null) return emptyList()

        return section.getKeys(false).mapNotNull { zoneName ->
            val zoneSection = section.getConfigurationSection(zoneName) ?: return@mapNotNull null
            val first = readPoint(zoneSection, FIRST_POINT_PATH) ?: return@mapNotNull null
            val second = readPoint(zoneSection, SECOND_POINT_PATH) ?: return@mapNotNull null
            val worldName = zoneSection.getString(WORLD_PATH)?.trim()?.takeIf { it.isNotEmpty() }

            PaintBlockedZone(
                worldName = worldName,
                minX = min(first.x, second.x),
                maxX = max(first.x, second.x),
                minY = min(first.y, second.y),
                maxY = max(first.y, second.y),
                minZ = min(first.z, second.z),
                maxZ = max(first.z, second.z)
            )
        }
    }

    private fun readPoint(section: ConfigurationSection, path: String): PaintZonePoint? {
        val list = section.getIntegerList(path)
        if (list.size >= COORDINATE_COUNT) {
            return PaintZonePoint(list[0], list[1], list[2])
        }

        val text = section.getString(path)
        if (!text.isNullOrBlank()) {
            val values = text.trim()
                .split(POINT_SPLIT_REGEX)
                .mapNotNull { it.toIntOrNull() }
            if (values.size >= COORDINATE_COUNT) {
                return PaintZonePoint(values[0], values[1], values[2])
            }
        }

        val pointSection = section.getConfigurationSection(path) ?: return null
        if (!pointSection.contains(X_PATH) || !pointSection.contains(Y_PATH) || !pointSection.contains(Z_PATH)) {
            return null
        }
        return PaintZonePoint(
            x = pointSection.getInt(X_PATH),
            y = pointSection.getInt(Y_PATH),
            z = pointSection.getInt(Z_PATH)
        )
    }

    private data class PaintBlockedZone(
        val worldName: String?,
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int,
        val minZ: Int,
        val maxZ: Int
    ) {
        fun contains(locationWorldName: String?, x: Int, y: Int, z: Int): Boolean {
            if (
                worldName != null &&
                (locationWorldName == null || !worldName.equals(locationWorldName, ignoreCase = true))
            ) {
                return false
            }
            return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
        }
    }

    private data class PaintZonePoint(
        val x: Int,
        val y: Int,
        val z: Int
    )

    private companion object {
        private const val WORLD_PATH = "world"
        private const val FIRST_POINT_PATH = "point-1"
        private const val SECOND_POINT_PATH = "point-2"
        private const val X_PATH = "x"
        private const val Y_PATH = "y"
        private const val Z_PATH = "z"
        private const val COORDINATE_COUNT = 3
        private val POINT_SPLIT_REGEX = Regex("[,\\s]+")
    }
}
