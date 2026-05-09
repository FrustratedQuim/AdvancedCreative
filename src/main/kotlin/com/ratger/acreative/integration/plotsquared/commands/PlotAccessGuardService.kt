package com.ratger.acreative.integration.plotsquared.commands

import com.google.common.eventbus.Subscribe
import com.plotsquared.bukkit.util.BukkitUtil
import com.plotsquared.core.PlotAPI
import com.plotsquared.core.events.PlayerEnterPlotEvent
import com.plotsquared.core.events.PlayerPlotDeniedEvent
import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.ratger.acreative.commands.sit.SitStyle
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.lang.Runnable
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class PlotAccessGuardService(
    private val hooker: FunctionHooker
) {

    private data class PostCommandCheck(
        val executorId: UUID,
        val plot: Plot,
        val targetNames: List<String>,
        val commandName: String
    )

    private val enabled = hooker.plugin.server.pluginManager.getPlugin(PLOT_SQUARED_PLUGIN_NAME)?.isEnabled == true
    private val installed = AtomicBoolean(false)

    fun install() {
        if (!enabled || !installed.compareAndSet(false, true)) {
            return
        }
        PlotAPI().registerListener(this)
    }

    fun uninstall() {
        installed.set(false)
    }

    fun schedulePostCommandAccessCheck(executor: Player, args: Array<out String>) {
        val check = createPostCommandCheck(executor, args) ?: return
        Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
            if (!installed.get()) {
                return@Runnable
            }
            enforceDeniedTargets(check)
        }, POST_COMMAND_CHECK_DELAY_TICKS)
    }

    fun deniedOnlinePlayerNamesInside(plot: Plot): List<String> {
        if (!enabled) {
            return emptyList()
        }

        return Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { player ->
                val plotUuid = plotUuid(player)
                plot.isDenied(plotUuid) && isInsidePlot(plot, player, plotUuid)
            }
            .map { it.name }
            .toList()
    }

    @Subscribe
    @Suppress("unused")
    fun onPlayerEnterPlot(event: PlayerEnterPlotEvent) {
        if (!installed.get()) {
            return
        }

        val player = event.plotPlayer.platformPlayer as? Player
            ?: Bukkit.getPlayer(event.plotPlayer.uuid)
            ?: return
        releaseDeniedHeadPassengers(player, event.plot)
    }

    @Subscribe
    @Suppress("unused")
    fun onPlayerPlotDenied(event: PlayerPlotDeniedEvent) {
        if (!installed.get() || !event.wasAdded()) {
            return
        }

        Bukkit.getPlayer(event.player)?.let { target ->
            Bukkit.getScheduler().runTaskLater(hooker.plugin, Runnable {
                if (!installed.get()) {
                    return@Runnable
                }
                enforceDeniedTarget(event.plot, target, "deny-event")
            }, POST_COMMAND_CHECK_DELAY_TICKS)
        }
    }

    private fun createPostCommandCheck(executor: Player, args: Array<out String>): PostCommandCheck? {
        if (!enabled || args.isEmpty()) {
            return null
        }

        val commandIndex = resolveCommandIndex(args)
        val commandName = args.getOrNull(commandIndex)?.lowercase(Locale.ROOT) ?: return null
        if (commandName !in DENY_OR_KICK_COMMANDS) {
            return null
        }

        val targetArgument = args.getOrNull(commandIndex + 1) ?: return null
        val targetNames = targetArgument.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != EVERYONE_TOKEN }
        if (targetNames.isEmpty()) {
            return null
        }

        val plot = resolveTargetPlot(executor, args) ?: return null
        return PostCommandCheck(executor.uniqueId, plot, targetNames, commandName)
    }

    private fun enforceDeniedTargets(check: PostCommandCheck) {
        val executor = Bukkit.getPlayer(check.executorId)
        val targets = check.targetNames.mapNotNull(::findOnlinePlayer)
        if (targets.isEmpty()) {
            return
        }

        targets.forEach { target ->
            if (enforceDeniedTarget(check.plot, target, check.commandName)) {
                hooker.actionLogger.info {
                    "Plot access guard kicked denied player=${hooker.actionLogger.playerRef(target)} plot=${check.plot} command=${check.commandName} executor=${executor?.let { hooker.actionLogger.playerRef(it) } ?: check.executorId}"
                }
            }
        }
    }

    private fun enforceDeniedTarget(plot: Plot, target: Player, reason: String): Boolean {
        if (!target.isOnline) {
            return false
        }

        val plotUuid = plotUuid(target)
        if (!plot.isDenied(plotUuid) || !isInsidePlot(plot, target, plotUuid)) {
            return false
        }

        if (target.gameMode != GameMode.CREATIVE) {
            target.gameMode = GameMode.CREATIVE
        }
        if (hooker.sitManager.isSitting(target)) {
            hooker.sitManager.unsitPlayer(target)
        }
        kickFromPlot(plot, target, reason)
        return true
    }

    private fun releaseDeniedHeadPassengers(carrier: Player, plot: Plot) {
        val chain = headPassengerChain(carrier)
        if (chain.isEmpty()) {
            return
        }

        val deniedPassenger = chain.firstOrNull { passenger ->
            plot.isDenied(plotUuid(passenger)) &&
                hooker.sitManager.getSitSession(passenger)?.style == SitStyle.HEAD
        } ?: return

        hooker.actionLogger.info {
            "Plot enter released denied head passenger carrier=${hooker.actionLogger.playerRef(carrier)} passenger=${hooker.actionLogger.playerRef(deniedPassenger)} plot=$plot"
        }
        hooker.sitManager.unsitPlayer(deniedPassenger)
    }

    private fun headPassengerChain(carrier: Player): List<Player> {
        val result = mutableListOf<Player>()
        val seen = mutableSetOf<UUID>()
        var current: Player? = hooker.sitManager.getHeadPassenger(carrier)
        var depth = 0

        while (current != null && depth < MAX_HEAD_PASSENGER_DEPTH && seen.add(current.uniqueId)) {
            result += current
            current = hooker.sitManager.getHeadPassenger(current)
            depth++
        }
        return result
    }

    private fun kickFromPlot(plot: Plot, target: Player, reason: String) {
        plot.getSide { plotLocation ->
            Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                if (!target.isOnline) {
                    return@Runnable
                }

                val kickLocation = runCatching {
                    BukkitUtil.adapt(plotLocation).add(0.5, 0.0, 0.5)
                }.getOrElse {
                    target.world.spawnLocation
                }
                target.teleport(kickLocation)
                hooker.actionLogger.info {
                    "Plot access guard teleported denied player=${hooker.actionLogger.playerRef(target)} reason=$reason to=${hooker.actionLogger.locationRef(kickLocation)}"
                }
            })
        }
    }

    private fun isInsidePlot(plot: Plot, target: Player, plotUuid: UUID): Boolean {
        if (plot.playersInPlot.any { it.uuid == plotUuid }) {
            return true
        }
        val currentPlot = runCatching { PlotPlayer.from(target).currentPlot }.getOrNull() ?: return false
        return currentPlot == plot
    }

    private fun plotUuid(player: Player): UUID =
        runCatching { PlotPlayer.from(player).uuid }.getOrDefault(player.uniqueId)

    private fun findOnlinePlayer(name: String): Player? =
        Bukkit.getOnlinePlayers().firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun resolveTargetPlot(player: Player, args: Array<out String>): Plot? {
        val plotPlayer = PlotPlayer.from(player)
        if (args.isNotEmpty() && looksLikePlotArgument(args[0])) {
            return runCatching { Plot.getPlotFromString(plotPlayer, args[0], false) }.getOrNull()
                ?: plotPlayer.currentPlot
        }
        return plotPlayer.currentPlot
    }

    private fun resolveCommandIndex(args: Array<out String>): Int {
        if (args.size > 1 && looksLikePlotArgument(args[0])) {
            return 1
        }
        return 0
    }

    private fun looksLikePlotArgument(token: String): Boolean {
        val trimmed = token.trim()
        if (';' !in trimmed || ',' in trimmed) {
            return false
        }
        return trimmed.count { it == ';' } >= 1
    }

    private companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        private const val EVERYONE_TOKEN = "*"
        private const val POST_COMMAND_CHECK_DELAY_TICKS = 1L
        private const val MAX_HEAD_PASSENGER_DEPTH = 10
        private val DENY_OR_KICK_COMMANDS = setOf("deny", "d", "ban", "kick")
    }
}
