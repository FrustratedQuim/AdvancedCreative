package com.ratger.acreative.commands.paint.rendering

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Vector3f
import com.ratger.acreative.commands.paint.model.PaintCanvasCell
import com.ratger.acreative.commands.paint.model.PaintFrameDirection
import me.tofaa.entitylib.meta.display.BlockDisplayMeta
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Location
import java.util.UUID

class EntityVisualFactory {
    data class CanvasCellVisuals(
        val frame: WrapperEntity,
        val backPanel: WrapperEntity
    )

    fun createCanvasCellVisuals(
        mapId: Int,
        direction: PaintFrameDirection,
        viewerIds: Collection<UUID>
    ): CanvasCellVisuals {
        return CanvasCellVisuals(
            frame = createFrame(mapId, direction, viewerIds),
            backPanel = createBackPanel(direction, viewerIds)
        )
    }

    fun spawnVisuals(visuals: CanvasCellVisuals, location: Location): Boolean {
        val backPanelLocation = PacketLocation(location.x, location.y, location.z, 0f, 0f)
        if (!visuals.backPanel.spawn(backPanelLocation)) {
            return false
        }

        val frameLocation = PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch)
        if (!visuals.frame.spawn(frameLocation)) {
            visuals.backPanel.remove()
            return false
        }

        return true
    }

    fun teleportCell(cell: PaintCanvasCell, location: Location) {
        cell.backPanel.teleport(PacketLocation(location.x, location.y, location.z, 0f, 0f))
        cell.frame.teleport(PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch))
    }

    fun removeVisuals(cell: PaintCanvasCell) {
        cell.frame.remove()
        cell.backPanel.remove()
    }

    fun removeVisuals(visuals: CanvasCellVisuals) {
        visuals.frame.remove()
        visuals.backPanel.remove()
    }

    fun addViewer(cell: PaintCanvasCell, viewerId: UUID) {
        cell.backPanel.addViewer(viewerId)
        cell.frame.addViewer(viewerId)
    }

    fun removeViewer(cell: PaintCanvasCell, viewerId: UUID) {
        cell.frame.removeViewer(viewerId)
        cell.backPanel.removeViewer(viewerId)
    }

    fun createMapItem(mapId: Int): PacketItemStack {
        val legacyTag = NBTCompound().apply {
            setTag("map", NBTInt(mapId))
            setTag("map_id", NBTInt(mapId))
        }
        return PacketItemStack.builder()
            .type(ItemTypes.FILLED_MAP)
            .nbt(legacyTag)
            .component(ComponentTypes.MAP_ID, mapId)
            .build()
    }

    private fun createFrame(
        mapId: Int,
        direction: PaintFrameDirection,
        viewerIds: Collection<UUID>
    ): WrapperEntity {
        val frame = WrapperEntity(EntityTypes.GLOW_ITEM_FRAME)
        viewerIds.forEach(frame::addViewer)
        val meta = frame.entityMeta as ItemFrameMeta
        meta.orientation = direction.orientation
        meta.item = createMapItem(mapId)
        return frame
    }

    private fun createBackPanel(
        direction: PaintFrameDirection,
        viewerIds: Collection<UUID>
    ): WrapperEntity {
        val backPanel = WrapperEntity(EntityTypes.BLOCK_DISPLAY)
        viewerIds.forEach(backPanel::addViewer)

        val meta = backPanel.entityMeta as BlockDisplayMeta
        val blockState = WrappedBlockState.getDefaultState(
            PacketEvents.getAPI().serverManager.version.toClientVersion(),
            StateTypes.OAK_PLANKS
        )
        meta.blockId = blockState.globalId
        applyBackPanelTransform(meta, direction)
        return backPanel
    }

    private fun applyBackPanelTransform(meta: BlockDisplayMeta, direction: PaintFrameDirection) {
        val half = BACK_PANEL_SIZE / 2f
        when (direction) {
            PaintFrameDirection.NORTH -> {
                meta.scale = Vector3f(BACK_PANEL_SIZE, BACK_PANEL_SIZE, BACK_PANEL_DEPTH)
                meta.translation = Vector3f(-half, -half, -(BACK_PANEL_GAP + BACK_PANEL_DEPTH))
            }

            PaintFrameDirection.SOUTH -> {
                meta.scale = Vector3f(BACK_PANEL_SIZE, BACK_PANEL_SIZE, BACK_PANEL_DEPTH)
                meta.translation = Vector3f(-half, -half, BACK_PANEL_GAP)
            }

            PaintFrameDirection.EAST -> {
                meta.scale = Vector3f(BACK_PANEL_DEPTH, BACK_PANEL_SIZE, BACK_PANEL_SIZE)
                meta.translation = Vector3f(BACK_PANEL_GAP, -half, -half)
            }

            PaintFrameDirection.WEST -> {
                meta.scale = Vector3f(BACK_PANEL_DEPTH, BACK_PANEL_SIZE, BACK_PANEL_SIZE)
                meta.translation = Vector3f(-(BACK_PANEL_GAP + BACK_PANEL_DEPTH), -half, -half)
            }
        }
    }

    private companion object {
        const val BACK_PANEL_SIZE = 1.0f
        const val BACK_PANEL_DEPTH = 0.1f
        const val BACK_PANEL_GAP = 0.02f
    }
}
