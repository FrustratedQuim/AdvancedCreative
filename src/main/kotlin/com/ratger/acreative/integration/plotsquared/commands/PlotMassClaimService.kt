package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.configuration.Settings
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

internal class PlotMassClaimService(
    private val hooker: FunctionHooker
) {
    private val massClaimLocks = ConcurrentHashMap.newKeySet<String>()

    fun handle(player: Player, widthToken: String?, heightToken: String?) {
        if (!hooker.serverPerformanceService.isStableForTickSensitiveActivation(MIN_MASSCLAIM_ACTIVATION_TPS)) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_SERVER_UNSTABLE)
            return
        }
        val w = widthToken?.trim()?.toIntOrNull()
        val h = heightToken?.trim()?.toIntOrNull()
        if (w == null || h == null) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_UNKNOWN_SIZE)
            return
        }
        if (w < 1 || h < 1) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_MIN_SIZE)
            return
        }
        if (w > MAX_MASSCLAIM_SIZE || h > MAX_MASSCLAIM_SIZE) {
            hooker.messageManager.sendChat(
                player,
                MessageKey.PLOT_MASSCLAIM_MAX_SIZE,
                mapOf("max" to "${MAX_MASSCLAIM_SIZE}x${MAX_MASSCLAIM_SIZE}")
            )
            return
        }

        val pp = PlotPlayer.from(player)
        val base = pp.currentPlot ?: run {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_EDIT_NOT_ON_PLOT)
            return
        }
        val needed = w * h
        val occupied = if (Settings.Limit.GLOBAL) pp.plotCount else pp.getPlotCount(pp.location.worldName)
        val allowed = pp.allowedPlots
        if (allowed < Settings.Limit.MAX_PLOTS && occupied + needed > allowed) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_LIMIT, mapOf("num" to allowed.toString()))
            return
        }

        val directions = facingDirections(player)
        val targets = ArrayList<Plot>()
        for (dy in 0 until h) for (dx in 0 until w) {
            val xOff = directions[0] * dx + directions[2] * dy
            val yOff = directions[1] * dx + directions[3] * dy
            val plot = base.getRelative(xOff, yOff) ?: run {
                hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
                return
            }
            if (plot.hasOwner() && !plot.isOwner(pp.uuid)) {
                hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
                return
            }
            targets += plot
        }

        val lockKey = buildLockKey(targets)
        if (!massClaimLocks.add(lockKey)) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
            return
        }

        try {
            for (plot in targets) {
                if (!plot.hasOwner()) {
                    val claimed = plot.setOwner(pp.uuid, pp)
                    if (!claimed) {
                        hooker.messageManager.sendChat(player, MessageKey.PLOT_MASSCLAIM_NO_SPACE)
                        return
                    }
                }
            }
            val baseId = "${base.id.x};${base.id.y}"
            Bukkit.dispatchCommand(player, "plot $baseId merge all")
        } finally {
            massClaimLocks.remove(lockKey)
        }
    }


    private fun facingDirections(player: Player): IntArray {
        val yaw = player.location.yaw
        return when (((yaw % 360) + 360).let { ((it + 45) / 90).toInt() % 4 }) {
            0 -> intArrayOf(0, 1, -1, 0)
            1 -> intArrayOf(-1, 0, 0, -1)
            2 -> intArrayOf(0, -1, 1, 0)
            else -> intArrayOf(1, 0, 0, 1)
        }
    }

    private fun buildLockKey(plots: List<Plot>): String =
        plots.asSequence().map { "${it.area?.id ?: "unknown"}:${it.id.x};${it.id.y}" }.sorted().joinToString("|")

    private companion object {
        private const val MAX_MASSCLAIM_SIZE = 10
        private const val MIN_MASSCLAIM_ACTIVATION_TPS = 18.0
    }
}
