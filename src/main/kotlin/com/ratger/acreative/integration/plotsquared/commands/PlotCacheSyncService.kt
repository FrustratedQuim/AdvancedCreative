package com.ratger.acreative.integration.plotsquared.commands

import com.google.common.eventbus.Subscribe
import com.plotsquared.core.PlotAPI
import com.plotsquared.core.events.PlayerClaimPlotEvent
import com.plotsquared.core.events.post.PostPlotChangeOwnerEvent
import com.plotsquared.core.events.post.PostPlotDeleteEvent
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import java.lang.Runnable
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class PlotCacheSyncService(
    private val hooker: FunctionHooker,
    private val plotCommandService: PlotCommandService
) {

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

    @Subscribe
    @Suppress("unused")
    fun onPostPlotChangeOwner(event: PostPlotChangeOwnerEvent) {
        if (!installed.get()) {
            return
        }

        val oldOwner = event.oldOwner
        val newOwners = event.plot.owners
        val newOwner = newOwners.firstOrNull()
        plotCommandService.invalidateOwnerSuggestions()
        plotCommandService.invalidateHomeCounts(oldOwner, newOwner)
    }

    @Subscribe
    @Suppress("unused")
    fun onPostPlotDelete(event: PostPlotDeleteEvent) {
        if (!installed.get()) {
            return
        }

        val deletedOwners = event.plot.owners
        plotCommandService.invalidateOwnerSuggestions()
        plotCommandService.invalidateHomeCounts(*deletedOwners.toTypedArray())
    }

    @Subscribe
    @Suppress("unused")
    fun onPlayerClaimPlot(event: PlayerClaimPlotEvent) {
        if (!installed.get()) {
            return
        }

        Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
            if (!installed.get()) {
                return@Runnable
            }
            val owners = event.plot.owners
            val owner = owners.firstOrNull()
            plotCommandService.invalidateOwnerSuggestions()
            plotCommandService.invalidateHomeCounts(owner)
        })
    }

    private fun PlotCommandService.invalidateHomeCounts(vararg ownerIds: UUID?) {
        ownerIds.filterNotNull().forEach(::invalidateHomeCount)
    }

    private companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
    }
}
