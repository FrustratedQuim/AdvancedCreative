package com.ratger.acreative.commands.paint.resize

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.ratger.acreative.commands.paint.map.MapColorMatcher
import com.ratger.acreative.commands.paint.map.MapDataExtractor
import com.ratger.acreative.commands.paint.model.PaintGridPoint
import com.ratger.acreative.commands.paint.model.PaintResizePreview
import com.ratger.acreative.commands.paint.model.PaintSession
import com.ratger.acreative.commands.paint.rendering.CanvasRenderer
import com.ratger.acreative.commands.paint.rendering.EntityVisualFactory
import com.ratger.acreative.commands.paint.rendering.MapDataSender
import com.ratger.acreative.commands.paint.rendering.PaintPreviewCoordinator
import com.ratger.acreative.menus.common.MenuSoundSupport
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

class PaintResizeService(
    private val resizeTargetResolver: PaintResizeTargetResolver,
    private val canvasRenderer: CanvasRenderer,
    private val entityVisualFactory: EntityVisualFactory,
    private val mapDataSender: MapDataSender,
    private val previewCoordinator: PaintPreviewCoordinator,
    private val openEaselMenu: (Player, PaintSession) -> Unit,
    private val applyCanvasZoom: (Player, PaintSession, Int) -> Boolean,
    private val normalizeSelectedZoom: (PaintSession) -> Unit,
    private val clearHistory: (PaintSession) -> Unit,
    private val backgroundColorId: Byte
) {

    fun beginResizeMode(player: Player, session: PaintSession) {
        val desiredZoom = session.selectedZoom
        if (session.appliedZoom != 1 && !applyCanvasZoom(player, session, 1)) {
            return
        }
        session.selectedZoom = desiredZoom
        session.resizeMode = true
    }

    fun updateResizePreview(player: Player, session: PaintSession) {
        val target = resizeTargetResolver.resolveTarget(player, session)
        if (target == null) {
            removeResizePreview(player, session)
            return
        }

        val canAdd = target.state == PaintResizeTargetState.ADD
        val preview = session.resizePreview ?: createResizePreview(player, session, target.location, canAdd).also {
            session.resizePreview = it
        }

        preview.targetPoint = target.point
        if (preview.canAdd != canAdd) {
            sendResizeTeamRemove(player, preview.teamName)
            sendResizeTeamAdd(player, preview.teamName, preview.frame.uuid.toString(), canAdd)
            preview.canAdd = canAdd
        }
        preview.frame.teleport(PacketLocation(target.location.x, target.location.y, target.location.z, target.location.yaw, target.location.pitch))
    }

    fun handleResizeActivation(player: Player, session: PaintSession) {
        val target = resizeTargetResolver.resolveTarget(player, session)
        if (target == null) {
            openEaselMenu(player, session)
            return
        }

        when (target.state) {
            PaintResizeTargetState.ADD -> {
                if (!addCanvasCell(player, session, target.point)) {
                    openEaselMenu(player, session)
                    return
                }
                MenuSoundSupport.itemFramePlace(player)
                previewCoordinator.clearStrokeState(session)
                clearHistory(session)
                session.resizeMode = false
                removeResizePreview(player, session)
                openEaselMenu(player, session)
            }

            PaintResizeTargetState.REMOVE_OWN -> {
                if (session.logicalCells.size <= 1) {
                    openEaselMenu(player, session)
                    return
                }
                removeCanvasCell(session, target.point)
                MenuSoundSupport.itemFrameBreak(player)
                previewCoordinator.clearStrokeState(session)
                clearHistory(session)
                session.resizeMode = false
                removeResizePreview(player, session)
                openEaselMenu(player, session)
            }

            PaintResizeTargetState.INVALID -> openEaselMenu(player, session)
        }
    }

    fun removeResizePreview(player: Player, session: PaintSession) {
        val preview = session.resizePreview ?: return
        sendResizeTeamRemove(player, preview.teamName)
        preview.frame.remove()
        session.resizePreview = null
    }

    private fun addCanvasCell(player: Player, session: PaintSession, point: PaintGridPoint): Boolean {
        session.logicalCells[point] = ByteArray(MAP_AREA) { backgroundColorId }
        if (!canvasRenderer.renderCanvas(player, session, session.appliedZoom)) {
            session.logicalCells.remove(point)
            return false
        }
        normalizeSelectedZoom(session)
        previewCoordinator.markCanvasTopologyChanged(session)
        return true
    }

    private fun removeCanvasCell(session: PaintSession, point: PaintGridPoint) {
        val player = Bukkit.getPlayer(session.playerId) ?: return
        if (session.logicalCells.remove(point) == null) return
        canvasRenderer.renderCanvas(player, session, session.appliedZoom)
        normalizeSelectedZoom(session)
        previewCoordinator.markCanvasTopologyChanged(session)
    }

    private fun createResizePreview(
        player: Player,
        session: PaintSession,
        location: org.bukkit.Location,
        canAdd: Boolean
    ): PaintResizePreview {
        val frame = WrapperEntity(EntityTypes.GLOW_ITEM_FRAME)
        frame.addViewer(player.uniqueId)
        val meta = frame.entityMeta as ItemFrameMeta
        meta.orientation = session.frameDirection.orientation
        val transparentMap = resolveResizePreviewMap(player, session)
        transparentMap?.let { snapshot ->
            meta.item = entityVisualFactory.createMapItem(snapshot.mapId)
        }
        meta.isGlowing = true
        frame.spawn(PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch))
        transparentMap?.let { snapshot ->
            mapDataSender.send(player, snapshot)
        }

        val preview = PaintResizePreview(
            frame = frame,
            teamName = "paint_${player.uniqueId.toString().take(8)}",
            canAdd = canAdd
        )
        sendResizeTeamAdd(player, preview.teamName, frame.uuid.toString(), canAdd)
        return preview
    }

    private fun resolveResizePreviewMap(player: Player, session: PaintSession): MapDataExtractor.Snapshot? {
        session.resizePreviewMapId?.let { mapId ->
            MapDataExtractor.extract(mapId)?.let { snapshot -> return snapshot }
        }
        val snapshot = MapDataExtractor.createFilled(player.world, MapColorMatcher.TRANSPARENT_COLOR_ID) ?: return null
        session.resizePreviewMapId = snapshot.mapId
        return snapshot
    }

    private fun sendResizeTeamAdd(player: Player, teamName: String, entry: String, canAdd: Boolean) {
        val scoreboard = Scoreboard()
        val team = PlayerTeam(scoreboard, teamName)
        team.color = if (canAdd) RESIZE_PREVIEW_CAN_ADD_COLOR else RESIZE_PREVIEW_CANNOT_ADD_COLOR
        team.players.add(entry)
        val packet = ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    private fun sendResizeTeamRemove(player: Player, teamName: String) {
        val scoreboard = Scoreboard()
        val team = PlayerTeam(scoreboard, teamName)
        val packet = ClientboundSetPlayerTeamPacket.createRemovePacket(team)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    private companion object {
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
        private const val MAP_AREA = MAP_WIDTH * MAP_HEIGHT
        private val RESIZE_PREVIEW_CAN_ADD_COLOR = ChatFormatting.GREEN
        private val RESIZE_PREVIEW_CANNOT_ADD_COLOR = ChatFormatting.RED
    }
}
