package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.configuration.Settings
import com.plotsquared.core.player.PlotPlayer

internal data class PlotUsageInfo(
    val occupied: Int,
    val totalText: String
)

internal class PlotUsageInfoService {

    fun snapshot(plotPlayer: PlotPlayer<*>): PlotUsageInfo {
        val occupied = if (Settings.Limit.GLOBAL) {
            plotPlayer.plotCount
        } else {
            plotPlayer.getPlotCount(plotPlayer.location.worldName)
        }
        val allowed = plotPlayer.allowedPlots
        val totalText = if (allowed >= Settings.Limit.MAX_PLOTS) UNLIMITED_TOTAL else allowed.toString()
        return PlotUsageInfo(occupied = occupied, totalText = totalText)
    }

    private companion object {
        private const val UNLIMITED_TOTAL = "∞"
    }
}
