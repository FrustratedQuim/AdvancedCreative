package com.ratger.acreative.commands.paint.session

import com.ratger.acreative.commands.paint.artwork.PaintArtworkService
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.rendering.CanvasRenderer
import com.ratger.acreative.commands.paint.rendering.PaintPreviewCoordinator
import com.ratger.acreative.commands.paint.resize.PaintResizeService
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.paint.PaintToolInventoryService
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PaintSessionTerminator(
    private val hooker: FunctionHooker,
    private val sessionManager: PaintSessionManager,
    private val previewCoordinator: PaintPreviewCoordinator,
    private val resizeService: PaintResizeService,
    private val canvasRenderer: CanvasRenderer,
    private val toolInventoryService: PaintToolInventoryService,
    private val artworkService: PaintArtworkService,
    private val applyCanvasZoom: (Player, PaintSession, Int) -> Boolean
) {

    fun stopPainting(player: Player) {
        val session = sessionManager.getSession(player.uniqueId) ?: return
        sessionManager.removeSession(player.uniqueId)
        if (session.appliedZoom != 1) {
            applyCanvasZoom(player, session, 1)
        }
        val shouldGiveArtwork = artworkService.hasArtwork(session)
        // Capture the artwork before canvas teardown clears the rendered cell collection.
        val artworkCells = session.cellsSortedTopLeft()
        previewCoordinator.clearRuntimeState(player.uniqueId)
        hooker.tickScheduler.cancel(session.viewerTaskId)
        hooker.tickScheduler.cancel(session.previewTaskId)
        previewCoordinator.restoreIfNeeded(player, session)
        resizeService.removeResizePreview(player, session)
        canvasRenderer.removeCanvas(session)
        toolInventoryService.clear(player)
        session.inventorySnapshot.restore(player)
        if (shouldGiveArtwork) {
            artworkService.giveResult(player, artworkCells, player.name, session.seriesCode)
        }
        hooker.playerStateManager.deactivateState(player, PlayerStateType.PAINTING)
        MenuSoundSupport.paintComplete(player)
    }

    fun releaseAll() {
        sessionManager.getAllSessions().map { it.playerId }.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let(::stopPainting) ?: run {
                previewCoordinator.clearRuntimeState(playerId)
                sessionManager.getSession(playerId)?.let { session ->
                    sessionManager.removeSession(playerId)
                    hooker.tickScheduler.cancel(session.viewerTaskId)
                    hooker.tickScheduler.cancel(session.previewTaskId)
                    canvasRenderer.removeCanvas(session)
                    session.resizePreview?.frame?.remove()
                }
            }
        }
    }
}
