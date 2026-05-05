package com.ratger.acreative.integration.plotsquared

import com.plotsquared.core.location.Location as PlotLocation
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.flag.GlobalFlagContainer
import com.plotsquared.core.plot.flag.types.BooleanFlag
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlotSquaredFlagGate(pluginManager: PluginManager) {

    private val enabled = pluginManager.getPlugin(PLOT_SQUARED_PLUGIN_NAME)?.isEnabled == true

    private data class GateCacheKey(
        val playerId: UUID,
        val worldName: String,
        val blockX: Int,
        val blockY: Int,
        val blockZ: Int,
        val flagName: String
    )

    private data class GateCacheEntry(
        val expiresAtNanos: Long,
        val forbidden: Boolean
    )

    private val gateCache = ConcurrentHashMap<GateCacheKey, GateCacheEntry>()

    fun registerFlagIfNeeded(flag: BooleanFlag<*>) {
        if (!enabled) return
        val container = GlobalFlagContainer.getInstance() ?: return
        if (container.getFlagFromString(flag.name) != null) return
        container.addFlag(flag)
    }

    fun isUsageForbidden(player: Player, flag: BooleanFlag<*>, bypassPermission: String): Boolean {
        return isUsageForbidden(player, player.location, flag, bypassPermission)
    }

    fun isUsageForbidden(player: Player, location: Location, flag: BooleanFlag<*>, bypassPermission: String): Boolean {
        if (!enabled) return false
        if (player.hasPermission(bypassPermission)) return false

        val worldName = location.world?.name ?: return false
        val now = System.nanoTime()
        val cacheKey = GateCacheKey(
            playerId = player.uniqueId,
            worldName = worldName,
            blockX = location.blockX,
            blockY = location.blockY,
            blockZ = location.blockZ,
            flagName = flag.name
        )
        gateCache[cacheKey]?.takeIf { it.expiresAtNanos > now }?.let { return it.forbidden }

        val plot = Plot.getPlot(PlotLocation.at(worldName, location.blockX, location.blockY, location.blockZ))
        val forbidden = when {
            plot == null -> false
            plot.isAdded(player.uniqueId) -> false
            else -> !plot.getFlag(flag.javaClass)
        }

        if (gateCache.size >= MAX_CACHE_SIZE) {
            gateCache.clear()
        }
        gateCache[cacheKey] = GateCacheEntry(now + CACHE_TTL_NANOS, forbidden)
        return forbidden
    }

    companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        private const val MAX_CACHE_SIZE = 4_096
        private const val CACHE_TTL_NANOS = 500_000_000L
    }
}
