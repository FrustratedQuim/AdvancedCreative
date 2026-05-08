package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.PlotArea
import com.plotsquared.core.plot.PlotId
import kotlin.math.max
import kotlin.math.min

internal data class PlotMassClaimSize(
    val width: Int,
    val height: Int
)

internal data class PlotMassClaimPlan(
    val area: PlotArea,
    val plots: List<Plot>,
    val mergePlotIds: List<PlotId>,
    val newClaims: Int,
    val requiresMerge: Boolean
)

internal enum class PlotMassClaimOrientation(
    private val rightX: Int,
    private val rightY: Int,
    private val forwardX: Int,
    private val forwardY: Int
) {
    SOUTH(rightX = -1, rightY = 0, forwardX = 0, forwardY = 1),
    WEST(rightX = 0, rightY = -1, forwardX = -1, forwardY = 0),
    NORTH(rightX = 1, rightY = 0, forwardX = 0, forwardY = -1),
    EAST(rightX = 0, rightY = 1, forwardX = 1, forwardY = 0);

    fun xOffset(dx: Int, dy: Int): Int = rightX * dx + forwardX * dy

    fun yOffset(dx: Int, dy: Int): Int = rightY * dx + forwardY * dy

    companion object {
        fun fromYaw(yaw: Float): PlotMassClaimOrientation =
            when (((yaw % 360) + 360).let { ((it + 45) / 90).toInt() % 4 }) {
                0 -> SOUTH
                1 -> WEST
                2 -> NORTH
                else -> EAST
            }
    }
}

internal class PlotMassClaimPlanner {

    fun plan(
        basePlot: Plot,
        plotPlayer: PlotPlayer<*>,
        orientation: PlotMassClaimOrientation,
        size: PlotMassClaimSize
    ): PlotMassClaimPlan? {
        val area = basePlot.area ?: return null
        val plots = buildSelection(basePlot, plotPlayer, orientation, size) ?: return null
        val targetIds = plots.asSequence().map { it.id }.toSet()

        if (plots.any { plot -> !plot.hasOwner() && !canClaim(plotPlayer, plot, area) }) {
            return null
        }
        if (plots.any { plot -> !staysInsideSelection(plot, area, targetIds) }) {
            return null
        }

        val mergePlotIds = buildMergePlotIds(targetIds)
        val requiresMerge = plots.size > 1 && plots.first().connectedPlots.asSequence().map { it.id }.toSet() != targetIds
        return PlotMassClaimPlan(
            area = area,
            plots = plots,
            mergePlotIds = mergePlotIds,
            newClaims = plots.count { !it.hasOwner() },
            requiresMerge = requiresMerge
        )
    }

    private fun buildSelection(
        basePlot: Plot,
        plotPlayer: PlotPlayer<*>,
        orientation: PlotMassClaimOrientation,
        size: PlotMassClaimSize
    ): List<Plot>? {
        val result = ArrayList<Plot>(size.width * size.height)
        for (dy in 0 until size.height) {
            for (dx in 0 until size.width) {
                val plot = basePlot.getRelative(
                    orientation.xOffset(dx, dy),
                    orientation.yOffset(dx, dy)
                ) ?: return null
                if (plot.hasOwner() && !plot.isOwner(plotPlayer.uuid)) {
                    return null
                }
                result += plot
            }
        }
        return result
    }

    private fun canClaim(plotPlayer: PlotPlayer<*>, plot: Plot, area: PlotArea): Boolean {
        if (!plot.canClaim(plotPlayer)) {
            return false
        }

        val border = area.getBorder(false)
        return border == Int.MAX_VALUE || plot.distanceFromOrigin <= border
    }

    private fun staysInsideSelection(plot: Plot, area: PlotArea, targetIds: Set<PlotId>): Boolean {
        if (!plot.isMerged) {
            return true
        }

        return plot.connectedPlots.all { connected ->
            connected.area == area && connected.id in targetIds
        }
    }

    private fun buildMergePlotIds(targetIds: Set<PlotId>): List<PlotId> {
        val minX = targetIds.minOf { it.x }
        val maxX = targetIds.maxOf { it.x }
        val minY = targetIds.minOf { it.y }
        val maxY = targetIds.maxOf { it.y }
        val ordered = ArrayList<PlotId>(max(1, (maxX - minX + 1) * (maxY - minY + 1)))
        for (x in min(minX, maxX)..max(minX, maxX)) {
            for (y in min(minY, maxY)..max(minY, maxY)) {
                ordered += PlotId.of(x, y)
            }
        }
        return ordered
    }
}
