package com.ratger.acreative.paint.plot

import com.plotsquared.core.location.Location as PlotLocation
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.flag.GlobalFlagContainer
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PaintPlotSquaredGate(pluginManager: PluginManager) {

    private val enabled = pluginManager.getPlugin(PLOT_SQUARED_PLUGIN_NAME)?.isEnabled == true

    private data class GateCacheKey(
        val playerId: UUID,
        val worldName: String,
        val blockX: Int,
        val blockY: Int,
        val blockZ: Int
    )

    private data class GateCacheEntry(
        val expiresAtNanos: Long,
        val forbidden: Boolean
    )

    private val gateCache = ConcurrentHashMap<GateCacheKey, GateCacheEntry>()


    fun registerFlagIfNeeded() {
        if (!enabled) return
        val container = GlobalFlagContainer.getInstance() ?: return
        if (container.getFlagFromString(FLAG_NAME) != null) return
        container.addFlag(PlotPaintFlag.TRUE)
    }

    fun isPaintForbidden(player: Player): Boolean = isPaintForbidden(player, player.location)

    fun isPaintForbidden(player: Player, location: Location): Boolean {
        if (!enabled) return false
        if (player.hasPermission(PAINT_BYPASS_PERMISSION)) return false

        val worldName = location.world?.name ?: return false
        val now = System.nanoTime()
        val cacheKey = GateCacheKey(player.uniqueId, worldName, location.blockX, location.blockY, location.blockZ)
        gateCache[cacheKey]?.takeIf { it.expiresAtNanos > now }?.let { return it.forbidden }

        val plot = Plot.getPlot(PlotLocation.at(worldName, location.blockX, location.blockY, location.blockZ))
        val forbidden = when {
            plot == null -> false
            plot.isAdded(player.uniqueId) -> false
            else -> !plot.getFlag(PlotPaintFlag::class.java)
        }

        if (gateCache.size >= MAX_CACHE_SIZE) {
            gateCache.clear()
        }
        gateCache[cacheKey] = GateCacheEntry(now + CACHE_TTL_NANOS, forbidden)
        return forbidden
    }

    companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        const val FLAG_NAME = "plot-paint"
        private const val PAINT_BYPASS_PERMISSION = "advancedcreative.paint.bypass"
        private const val MAX_CACHE_SIZE = 4_096
        private const val CACHE_TTL_NANOS = 500_000_000L
    }
}
