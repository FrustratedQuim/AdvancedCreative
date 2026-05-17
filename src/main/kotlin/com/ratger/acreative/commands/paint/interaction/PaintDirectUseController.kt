package com.ratger.acreative.commands.paint.interaction

import com.ratger.acreative.commands.paint.model.PaintInputKind
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.rendering.PaintPreviewCoordinator
import com.ratger.acreative.commands.paint.resize.PaintResizeService
import com.ratger.acreative.commands.paint.session.PaintSessionManager
import com.ratger.acreative.commands.paint.tools.HistoryManager
import com.ratger.acreative.commands.paint.tools.PaintToolApplier
import com.ratger.acreative.menus.paint.PaintMenuController
import com.ratger.acreative.menus.paint.PaintToolInventoryService
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack as BukkitItemStack

class PaintDirectUseController(
    private val sessionManager: PaintSessionManager,
    private val hitDetectionService: HitDetectionService,
    private val toolInventoryService: PaintToolInventoryService,
    private val menuController: PaintMenuController,
    private val previewCoordinator: PaintPreviewCoordinator,
    private val resizeService: PaintResizeService,
    private val historyManager: HistoryManager,
    private val toolApplier: PaintToolApplier,
    private val directUseSuppression: PaintDirectUseSuppression
) {

    fun shouldCancelWorldAction(player: Player): Boolean {
        val session = sessionManager.getSession(player.uniqueId) ?: return false
        return session.resizeMode || hasWorkToolInHands(player)
    }

    fun handleDirectUse(player: Player): Boolean {
        val session = sessionManager.getSession(player.uniqueId) ?: return false
        if (session.isMenuOpen) return true
        if (directUseSuppression.isSuppressed(player.uniqueId)) {
            return true
        }
        if (session.previewPaused) {
            session.previewPaused = false
            previewCoordinator.clearSuppression(session)
        }

        if (!tryConsumeInput(session, PaintInputKind.DIRECT_USE, DIRECT_USE_DEBOUNCE_MILLIS)) {
            return true
        }

        if (session.resizeMode) {
            resizeService.handleResizeActivation(player, session)
            return true
        }

        if (player.isSneaking) {
            val now = System.currentTimeMillis()
            if (now - session.lastPaletteRotationAtMillis < PALETTE_ROTATION_DEBOUNCE_MILLIS) {
                return true
            }
            session.lastPaletteRotationAtMillis = now
            previewCoordinator.clearStrokeState(session)
            previewCoordinator.clearLineAnchorState(session)
            session.paletteRotation = (session.paletteRotation + 1) % 4
            toolInventoryService.refresh(player, session)
            return true
        }

        val tool = toolInventoryService.resolve(player.inventory.itemInMainHand) ?: return true
        if (!previewCoordinator.isLineShapeTool(tool, session)) {
            previewCoordinator.clearLineAnchorState(session)
        }
        val hit = hitDetectionService.resolveHitPixel(player, session, allowMissingCell = false, clampToCanvasBounds = true) ?: return true
        toolApplier.executeTool(player, session, tool, hit)
        return true
    }

    fun handleDropAction(player: Player, stackDrop: Boolean): Boolean {
        val session = sessionManager.getSession(player.uniqueId) ?: return false
        val inputKind = if (stackDrop) PaintInputKind.DROP_STACK else PaintInputKind.DROP_SINGLE
        if (!tryConsumeInput(session, inputKind, DROP_ACTION_DEBOUNCE_MILLIS)) {
            return true
        }
        session.previewPaused = true
        if (session.resizeMode) {
            menuController.openEaselMenu(player, session)
            return true
        }
        if (stackDrop) {
            historyManager.undoLastAction(player, session)
            return true
        }
        if (!isWorkTool(player.inventory.itemInMainHand)) {
            return true
        } else {
            menuController.openSettingsForCurrentTool(player, session)
        }
        return true
    }

    fun resyncHeldToolSlot(player: Player) {
        val session = sessionManager.getSession(player.uniqueId) ?: return
        toolInventoryService.resyncHeldToolSlot(player, session)
    }

    fun handleHeldToolChange(player: Player) {
        val session = sessionManager.getSession(player.uniqueId) ?: return
        previewCoordinator.clearLineAnchorState(session)
        previewCoordinator.clearStrokeState(session)
        previewCoordinator.clearSuppression(session)
        previewCoordinator.restoreIfNeeded(player, session)
    }

    fun suppressDirectUseAfterDrop(player: Player) {
        directUseSuppression.suppress(player.uniqueId, DIRECT_USE_SUPPRESS_AFTER_DROP_MILLIS)
    }

    private fun tryConsumeInput(session: PaintSession, inputKind: PaintInputKind, debounceMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        if (session.lastInputKind == inputKind && now - session.lastInputAtMillis < debounceMillis) {
            return false
        }
        session.lastInputKind = inputKind
        session.lastInputAtMillis = now
        return true
    }

    private fun isWorkTool(item: BukkitItemStack?): Boolean = toolInventoryService.isWorkTool(item)

    private fun hasWorkToolInHands(player: Player): Boolean {
        return isWorkTool(player.inventory.itemInMainHand) || isWorkTool(player.inventory.itemInOffHand)
    }

    private companion object {
        private const val DIRECT_USE_DEBOUNCE_MILLIS = 65L
        private const val DIRECT_USE_SUPPRESS_AFTER_DROP_MILLIS = 250L
        private const val DROP_ACTION_DEBOUNCE_MILLIS = 65L
        private const val PALETTE_ROTATION_DEBOUNCE_MILLIS = 100L
    }
}
