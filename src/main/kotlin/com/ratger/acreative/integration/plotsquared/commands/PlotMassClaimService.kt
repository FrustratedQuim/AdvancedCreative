package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.configuration.Settings
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.PlotId
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

internal class PlotMassClaimService(
    private val hooker: FunctionHooker,
    private val planner: PlotMassClaimPlanner = PlotMassClaimPlanner(),
    private val executor: PlotMassClaimExecutor = PlotMassClaimExecutor(),
    private val usageInfoService: PlotUsageInfoService = PlotUsageInfoService()
) {
    private val massClaimLocks = ConcurrentHashMap.newKeySet<String>()

    fun handle(player: Player, widthToken: String?, heightToken: String?) {
        if (!hooker.serverPerformanceService.isStableForTickSensitiveActivation(MIN_MASSCLAIM_ACTIVATION_TPS)) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_SERVER_UNSTABLE)
            return
        }
        val size = parseSize(player, widthToken, heightToken) ?: return

        val plotPlayer = PlotPlayer.from(player)
        val basePlot = plotPlayer.currentPlot ?: run {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_EDIT_NOT_ON_PLOT)
            return
        }
        val plan = planner.plan(
            basePlot = basePlot,
            plotPlayer = plotPlayer,
            orientation = PlotMassClaimOrientation.fromYaw(player.location.yaw),
            size = size
        ) ?: run {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
            return
        }

        val allowed = plotPlayer.allowedPlots
        if (!hasEnoughPlotAllowance(plotPlayer, plan.newClaims)) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_LIMIT, mapOf("num" to allowed.toString()))
            return
        }

        val lockKey = buildLockKey(plan.area.id, plan.mergePlotIds)
        if (!massClaimLocks.add(lockKey)) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
            return
        }

        try {
            if (!executor.execute(plan, plotPlayer)) {
                hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
                return
            }
            val usage = usageInfoService.snapshot(plotPlayer)
            hooker.messageManager.sendChat(
                player,
                MessageKey.PLOT_MASSCLAIM_SUCCESS,
                mapOf(
                    "occupied" to usage.occupied.toString(),
                    "total" to usage.totalText
                )
            )
        } finally {
            massClaimLocks.remove(lockKey)
        }
    }

    private fun parseSize(player: Player, widthToken: String?, heightToken: String?): PlotMassClaimSize? {
        val width = widthToken?.trim()?.toIntOrNull()
        val height = heightToken?.trim()?.toIntOrNull()
        if (width == null || height == null) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_UNKNOWN_SIZE)
            return null
        }
        if (width < 1 || height < 1) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_MIN_SIZE)
            return null
        }
        if (width > MAX_MASSCLAIM_SIZE || height > MAX_MASSCLAIM_SIZE) {
            hooker.messageManager.sendChat(
                player,
                MessageKey.PLOT_MASSCLAIM_MAX_SIZE,
                mapOf("max" to "${MAX_MASSCLAIM_SIZE}x${MAX_MASSCLAIM_SIZE}")
            )
            return null
        }
        return PlotMassClaimSize(width, height)
    }

    private fun hasEnoughPlotAllowance(plotPlayer: PlotPlayer<*>, newClaims: Int): Boolean {
        if (newClaims <= 0) {
            return true
        }

        val occupied = usageInfoService.snapshot(plotPlayer).occupied
        val allowed = plotPlayer.allowedPlots
        return allowed >= Settings.Limit.MAX_PLOTS || occupied + newClaims <= allowed
    }

    private fun buildLockKey(areaId: String, plotIds: List<PlotId>): String =
        plotIds.asSequence()
            .map { "$areaId:${it.x};${it.y}" }
            .sorted()
            .joinToString("|")

    companion object {
        internal const val MAX_MASSCLAIM_SIZE = 10
        private const val MIN_MASSCLAIM_ACTIVATION_TPS = 18.0
    }
}
