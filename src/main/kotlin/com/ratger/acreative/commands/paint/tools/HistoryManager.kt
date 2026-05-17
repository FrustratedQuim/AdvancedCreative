package com.ratger.acreative.commands.paint.tools

import com.ratger.acreative.commands.paint.model.PaintHistoryEntry
import com.ratger.acreative.commands.paint.model.PaintLineAnchor
import com.ratger.acreative.commands.paint.model.PaintLogicalPixelChange
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.rendering.MapDataSender
import com.ratger.acreative.commands.paint.rendering.PaintCanvasPixelProjector
import com.ratger.acreative.commands.paint.rendering.PaintPreviewCoordinator
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class HistoryManager(
    private val canvasPixelProjector: PaintCanvasPixelProjector,
    private val previewCoordinator: PaintPreviewCoordinator,
    private val mapDataSender: MapDataSender
) {
    fun applyHistoryChanges(
        session: PaintSession,
        pixelChanges: List<PaintLogicalPixelChange>,
        lineAnchorBefore: PaintLineAnchor? = null,
        lineAnchorAfter: PaintLineAnchor? = null
    ) {
        val hasLineAnchorChange = lineAnchorBefore != lineAnchorAfter
        if (pixelChanges.isEmpty() && !hasLineAnchorChange) return
        session.previewPaused = false
        previewCoordinator.clearSuppression(session)
        Bukkit.getPlayer(session.playerId)?.let { previewCoordinator.restoreIfNeeded(it, session) }

        val patches = canvasPixelProjector.applyLogicalPixelChanges(session, pixelChanges)
        if (hasLineAnchorChange) {
            session.shapeLineAnchor = lineAnchorAfter
        }

        val historyEntry = PaintHistoryEntry(
            pixelChanges = pixelChanges,
            estimatedBytes = estimateHistoryEntryBytes(pixelChanges.size, hasLineAnchorChange),
            lineAnchorBefore = lineAnchorBefore,
            lineAnchorAfter = lineAnchorAfter
        )
        session.history += historyEntry
        session.historyBytes += historyEntry.estimatedBytes
        trimHistory(session)

        if (pixelChanges.isNotEmpty() || patches.isNotEmpty()) {
            previewCoordinator.markCanvasChanged(session)
        }
        mapDataSender.sendPatchesToSessionViewers(session, patches)
    }

    fun undoLastAction(player: Player, session: PaintSession) {
        if (session.history.isEmpty()) {
            return
        }
        val entry = session.history.removeLast()
        session.historyBytes = (session.historyBytes - entry.estimatedBytes).coerceAtLeast(0L)
        session.previewSuppressionKey = previewCoordinator.buildCurrentPreviewSuppressionKey(player, session)
        previewCoordinator.restoreIfNeeded(player, session)
        val revertedChanges = entry.pixelChanges.map { change ->
            PaintLogicalPixelChange(change.globalX, change.globalY, change.newColor, change.oldColor)
        }
        val patches = canvasPixelProjector.applyLogicalPixelChanges(session, revertedChanges)
        if (entry.hasLineAnchorChange) {
            session.shapeLineAnchor = entry.lineAnchorBefore
        }
        if (revertedChanges.isNotEmpty() || patches.isNotEmpty()) {
            previewCoordinator.markCanvasChanged(session)
        }
        mapDataSender.sendPatchesToSessionViewers(session, patches)
        previewCoordinator.clearStrokeState(session)
    }

    private fun trimHistory(session: PaintSession) {
        while (session.historyBytes > MAX_HISTORY_BYTES && session.history.isNotEmpty()) {
            val removed = session.history.removeFirst()
            session.historyBytes = (session.historyBytes - removed.estimatedBytes).coerceAtLeast(0L)
        }
    }

    private fun estimateHistoryEntryBytes(
        changeCount: Int,
        hasLineAnchorChange: Boolean
    ): Long {
        val lineAnchorBytes = if (hasLineAnchorChange) HISTORY_LINE_ANCHOR_ESTIMATE_BYTES else 0L
        return HISTORY_ENTRY_BASE_BYTES + lineAnchorBytes + changeCount * HISTORY_PIXEL_ESTIMATE_BYTES
    }

    private companion object {
        const val HISTORY_PIXEL_ESTIMATE_BYTES = 40L
        const val HISTORY_ENTRY_BASE_BYTES = 64L
        const val HISTORY_LINE_ANCHOR_ESTIMATE_BYTES = 32L
        const val MAX_HISTORY_BYTES = 32L * 1024L * 1024L
    }
}
