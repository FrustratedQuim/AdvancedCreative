package com.ratger.acreative.integration.plotsquared.commands

import com.google.common.eventbus.Subscribe
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
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
    private val spectatorTargets = mutableMapOf<UUID, UUID>()
    private val spectatorViewers = mutableMapOf<UUID, MutableSet<UUID>>()

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


    fun canRideHeadAtCarrierPlot(rider: Player, carrier: Player): Boolean {
        if (!enabled) {
            return true
        }

        val riderUuid = plotUuid(rider)
        val carrierPlot = effectivePlot(carrier) ?: return true
        return !carrierPlot.isDenied(riderUuid)
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

    fun clearPlayerSpectating(player: Player) {
        if (!enabled || !installed.get()) {
            removeSpectatorLink(player.uniqueId)
            return
        }

        removeSpectatorLink(player.uniqueId)?.let { targetId ->
            hooker.actionLogger.auditInfo {
                val targetRef = Bukkit.getPlayer(targetId)?.let { hooker.actionLogger.playerRef(it) } ?: targetId.toString()
                "Spectator detached viewer=${hooker.actionLogger.playerRef(player)} target=$targetRef reason=player-reset"
            }
        }

        releaseSpectatorsForTarget(player.uniqueId)

        if (player.isOnline && player.gameMode == GameMode.SPECTATOR) {
            player.spectatorTarget = null
        }
    }

    fun onPlayerStartSpectatingEntity(event: PlayerStartSpectatingEntityEvent) {
        if (!installed.get() || !enabled) {
            return
        }

        val viewer = event.player
        val target = event.newSpectatorTarget as? Player
        if (target == null) {
            removeSpectatorLink(viewer.uniqueId)
            return
        }

        if (!canRideHeadAtCarrierPlot(viewer, target)) {
            hooker.actionLogger.auditWarning {
                "Spectator start blocked viewer=${hooker.actionLogger.playerRef(viewer)} target=${hooker.actionLogger.playerRef(target)}"
            }
            event.isCancelled = true
            return
        }

        registerSpectatorLink(viewer.uniqueId, target.uniqueId)
        hooker.actionLogger.auditInfo {
            "Spectator start viewer=${hooker.actionLogger.playerRef(viewer)} target=${hooker.actionLogger.playerRef(target)}"
        }
    }

    fun onPlayerStopSpectatingEntity(event: PlayerStopSpectatingEntityEvent) {
        if (!installed.get() || !enabled) {
            return
        }

        val viewer = event.player
        val target = event.spectatorTarget as? Player
        val removedTargetId = removeSpectatorLink(viewer.uniqueId)
        if (removedTargetId != null) {
            hooker.actionLogger.auditInfo {
                val targetRef = target?.let { hooker.actionLogger.playerRef(it) }
                    ?: Bukkit.getPlayer(removedTargetId)?.let { hooker.actionLogger.playerRef(it) }
                    ?: removedTargetId.toString()
                "Spectator stop viewer=${hooker.actionLogger.playerRef(viewer)} target=$targetRef"
            }
        }
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
        releaseDeniedSpectatorsAttachedToCarrier(player, event.plot)
        releaseDeniedHeadPassengers(player, event.plot)
        enforceDeniedTarget(event.plot, player)
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
                enforceDeniedTarget(event.plot, target)
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
        val targets = check.targetNames.mapNotNull(::findOnlinePlayer)
        if (targets.isEmpty()) {
            return
        }

        targets.forEach { target ->
            enforceDeniedTarget(check.plot, target)
        }
    }

    private fun enforceDeniedTarget(plot: Plot, target: Player): Boolean {
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

        releaseDeniedSpectatorsAttachedToCarrier(target, plot)
        releaseHeadPassengerChain(target)

        if (hooker.sitManager.isSitting(target)) {
            hooker.sitManager.unsitPlayer(target)
        }
        kickFromPlot(plot, target)
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
        hooker.sitManager.unsitPlayer(deniedPassenger)
    }


    private fun releaseHeadPassengerChain(carrier: Player) {
        val chain = headPassengerChain(carrier)
        if (chain.isEmpty()) {
            return
        }

        chain.filter { passenger ->
            hooker.sitManager.getSitSession(passenger)?.style == SitStyle.HEAD
        }.forEach { passenger ->
            hooker.sitManager.unsitPlayer(passenger)
        }
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

    private fun kickFromPlot(plot: Plot, target: Player) {
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
            })
        }
    }

    private fun isInsidePlot(plot: Plot, target: Player, plotUuid: UUID): Boolean {
        if (plot.playersInPlot.any { it.uuid == plotUuid }) {
            return true
        }
        return effectivePlot(target) == plot
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
        return effectivePlot(player) ?: plotPlayer.currentPlot
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

    private fun releaseDeniedSpectatorsAttachedToCarrier(carrier: Player, plot: Plot) {
        val viewerIds = spectatorViewers[carrier.uniqueId]?.toList().orEmpty()
        if (viewerIds.isEmpty()) {
            return
        }

        viewerIds.forEach { viewerId ->
            val viewer = Bukkit.getPlayer(viewerId) ?: run {
                removeSpectatorLink(viewerId)
                return@forEach
            }

            val viewerUuid = plotUuid(viewer)
            if (!plot.isDenied(viewerUuid) || !isInsidePlot(plot, viewer, viewerUuid)) {
                return@forEach
            }
            stopSpectatingViewer(viewer)
        }
    }

    private fun releaseSpectatorsForTarget(targetId: UUID) {
        val viewerIds = spectatorViewers[targetId]?.toList().orEmpty()
        if (viewerIds.isEmpty()) {
            return
        }

        viewerIds.forEach { viewerId ->
            val viewer = Bukkit.getPlayer(viewerId)
            if (viewer != null) {
                stopSpectatingViewer(viewer)
            } else {
                removeSpectatorLink(viewerId)
            }
        }
    }

    private fun stopSpectatingViewer(viewer: Player) {
        removeSpectatorLink(viewer.uniqueId) ?: return
        if (viewer.isOnline && viewer.gameMode == GameMode.SPECTATOR) {
            viewer.spectatorTarget = null
        }
    }

    private fun registerSpectatorLink(viewerId: UUID, targetId: UUID) {
        val currentTargetId = spectatorTargets[viewerId]
        if (currentTargetId == targetId) {
            return
        }

        removeSpectatorLink(viewerId)
        spectatorTargets[viewerId] = targetId
        spectatorViewers.computeIfAbsent(targetId) { mutableSetOf() }.add(viewerId)
    }

    private fun removeSpectatorLink(viewerId: UUID): UUID? {
        val targetId = spectatorTargets.remove(viewerId) ?: return null
        spectatorViewers[targetId]?.let { viewers ->
            viewers.remove(viewerId)
            if (viewers.isEmpty()) {
                spectatorViewers.remove(targetId)
            }
        }
        return targetId
    }

    private fun spectatorTarget(player: Player): Player? {
        val targetId = spectatorTargets[player.uniqueId] ?: return null
        return Bukkit.getPlayer(targetId)
    }

    private fun effectivePlot(player: Player, visited: MutableSet<UUID> = mutableSetOf()): Plot? {
        if (!visited.add(player.uniqueId)) {
            return runCatching { PlotPlayer.from(player).currentPlot }.getOrNull()
        }

        val spectatedTarget = spectatorTarget(player)
        if (spectatedTarget != null) {
            return effectivePlot(spectatedTarget, visited)
                ?: runCatching { PlotPlayer.from(player).currentPlot }.getOrNull()
        }

        return runCatching { PlotPlayer.from(player).currentPlot }.getOrNull()
    }

    private companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        private const val EVERYONE_TOKEN = "*"
        private const val POST_COMMAND_CHECK_DELAY_TICKS = 1L
        private const val MAX_HEAD_PASSENGER_DEPTH = 10
        private val DENY_OR_KICK_COMMANDS = setOf("deny", "d", "ban", "kick", "k")
    }
}
