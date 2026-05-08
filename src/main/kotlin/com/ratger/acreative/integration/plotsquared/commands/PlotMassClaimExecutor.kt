package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot

internal class PlotMassClaimExecutor {

    fun execute(plan: PlotMassClaimPlan, plotPlayer: PlotPlayer<*>): Boolean {
        val claimedPlots = ArrayList<Plot>(plan.newClaims)
        for (plot in plan.plots) {
            if (plot.hasOwner()) {
                continue
            }
            if (!plot.setOwner(plotPlayer.uuid, plotPlayer)) {
                rollback(claimedPlots)
                return false
            }
            claimedPlots += plot
        }

        if (!plan.requiresMerge) {
            return true
        }
        if (plan.area.mergePlots(plan.mergePlotIds, true)) {
            return true
        }

        rollback(claimedPlots)
        return false
    }

    private fun rollback(claimedPlots: List<Plot>) {
        claimedPlots.asReversed().forEach { plot ->
            runCatching { plot.unclaim() }
        }
    }
}
