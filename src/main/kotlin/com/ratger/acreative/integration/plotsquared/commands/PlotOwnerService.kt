package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.database.DBFunc
import com.plotsquared.core.plot.Plot
import java.util.UUID

internal class PlotOwnerService {

    sealed interface Outcome {
        data class Success(val affectedOwnerIds: Set<UUID>) : Outcome
        data object AlreadyOwner : Outcome
        data object NoFreeOwnerSlot : Outcome
        data object NotOwner : Outcome
        data object CannotRemoveLastOwner : Outcome
    }

    fun addOwner(referencePlot: Plot, newOwnerId: UUID): Outcome {
        val connectedPlots = orderedConnectedPlots(referencePlot)
        if (connectedPlots.any { it.ownerAbs == newOwnerId }) {
            return Outcome.AlreadyOwner
        }

        val ownerCounts = connectedPlots
            .mapNotNull(Plot::getOwnerAbs)
            .groupingBy { it }
            .eachCount()
        val donorPlot = connectedPlots
            .asSequence()
            .filter { current ->
                val currentOwner = current.ownerAbs ?: return@filter false
                ownerCounts.getValue(currentOwner) > 1
            }
            .sortedWith(compareBy({ if (it.isBasePlot) 1 else 0 }, { it.id.x }, { it.id.y }))
            .firstOrNull()
            ?: return Outcome.NoFreeOwnerSlot

        val previousOwnerId = donorPlot.ownerAbs ?: return Outcome.NoFreeOwnerSlot
        applyOwnerUpdates(
            updates = listOf(OwnerUpdate(donorPlot, previousOwnerId, newOwnerId)),
            deniedOwnersToRemove = setOf(newOwnerId)
        )
        return Outcome.Success(setOf(previousOwnerId, newOwnerId))
    }

    fun removeOwner(referencePlot: Plot, ownerId: UUID): Outcome {
        val connectedPlots = orderedConnectedPlots(referencePlot)
        val ownedPlots = connectedPlots.filter { it.ownerAbs == ownerId }
        if (ownedPlots.isEmpty()) {
            return Outcome.NotOwner
        }

        val fallbackOwnerId = resolveFallbackOwnerId(referencePlot, connectedPlots, ownerId)
            ?: return Outcome.CannotRemoveLastOwner
        applyOwnerUpdates(
            updates = ownedPlots.map { plot ->
                OwnerUpdate(plot = plot, oldOwnerId = ownerId, newOwnerId = fallbackOwnerId)
            },
            deniedOwnersToRemove = setOf(fallbackOwnerId)
        )
        return Outcome.Success(setOf(ownerId, fallbackOwnerId))
    }

    private fun applyOwnerUpdates(
        updates: List<OwnerUpdate>,
        deniedOwnersToRemove: Set<UUID> = emptySet()
    ) {
        if (updates.isEmpty()) {
            return
        }

        updates.forEach { update ->
            if (update.oldOwnerId == update.newOwnerId) {
                return@forEach
            }
            update.plot.setOwnerAbs(update.newOwnerId)
            DBFunc.setOwner(update.plot, update.newOwnerId)
        }

        val rootPlot = updates.first().plot.getBasePlot(false)
        deniedOwnersToRemove.forEach { deniedOwnerId ->
            if (rootPlot.isDenied(deniedOwnerId)) {
                rootPlot.removeDenied(deniedOwnerId)
            }
        }

        updates.firstOrNull { it.plot.isBasePlot }?.plot?.plotModificationManager?.setSign()
    }

    private fun orderedConnectedPlots(referencePlot: Plot): List<Plot> =
        referencePlot.connectedPlots
            .sortedWith(compareBy<Plot>({ it.id.x }, { it.id.y }))

    private fun resolveFallbackOwnerId(referencePlot: Plot, connectedPlots: List<Plot>, removedOwnerId: UUID): UUID? {
        val basePlotOwnerId = referencePlot.getBasePlot(false).ownerAbs
        if (basePlotOwnerId != null && basePlotOwnerId != removedOwnerId) {
            return basePlotOwnerId
        }

        return connectedPlots
            .asSequence()
            .mapNotNull(Plot::getOwnerAbs)
            .firstOrNull { it != removedOwnerId }
    }

    private data class OwnerUpdate(
        val plot: Plot,
        val oldOwnerId: UUID,
        val newOwnerId: UUID
    )
}
