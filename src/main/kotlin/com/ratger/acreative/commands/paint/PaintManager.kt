package com.ratger.acreative.commands.paint

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.edit.map.MapItemSupport
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.map.MapView
import java.util.UUID

class PaintManager(private val hooker: FunctionHooker) {

    private val sessions = mutableMapOf<UUID, PaintSession>()

    fun handlePaintCommand(player: Player, args: Array<out String>) {
        if (args.isNotEmpty()) {
            hooker.messageManager.sendChat(player, MessageKey.USAGE_PAINT)
            return
        }

        if (isPainting(player)) {
            stopPainting(player)
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_OFF)
            return
        }

        if (startPainting(player)) {
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_ON)
        }
    }

    fun isPainting(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun handleFrameInteraction(entityId: Int): Boolean {
        return sessions.values.any { it.frame.entityId == entityId }
    }

    fun handleFrameUse(player: Player, entityId: Int): Boolean {
        val session = sessions.values.firstOrNull { it.frame.entityId == entityId } ?: return false
        if (session.playerId != player.uniqueId) return true
        handleInteract(player)
        return true
    }

    fun handlePlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        if (!isPainting(player)) return

        stopPainting(player)
        event.keepInventory = true
        event.drops.clear()
    }

    fun cleanupSessionsForPlayer(player: Player) {
        stopPainting(player)
    }

    fun handleInteract(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        val paint = PaintPalette.fromMaterial(player.inventory.itemInMainHand.type) ?: return true
        val snapshot = MapDataExtractor.fill(session.mapId, paint.mapColor)
        if (snapshot == null) {
            hooker.messageManager.sendChat(
                player,
                MessageKey.ERROR_PAINT_MAP_MISSING,
                mapOf("map" to MapDataExtractor.describeMissing(session.mapId))
            )
            stopPainting(player)
            return true
        }
        sendFullMapDataToViewers(session, snapshot)
        scheduleDelayedMapDataRefresh(session.playerId, session.mapId, session.viewers.toSet())
        return true
    }

    fun stopPainting(player: Player) {
        val session = sessions.remove(player.uniqueId) ?: return
        hooker.tickScheduler.cancel(session.viewerTaskId)
        session.frame.remove()
        clearPaintInventory(player)
        session.inventorySnapshot.restore(player)
        if (!replaceSourceHandMap(player, session)) {
            giveResultMap(player, session.mapId)
        }
        hooker.playerStateManager.deactivateState(player, PlayerStateType.PAINTING)
    }

    fun releaseAll() {
        sessions.entries.toList().forEach { (playerId, session) ->
            val player = Bukkit.getPlayer(playerId)
            if (player != null) {
                stopPainting(player)
                return@forEach
            }
            hooker.tickScheduler.cancel(session.viewerTaskId)
            session.frame.remove()
            sessions.remove(playerId)
        }
    }

    private fun startPainting(player: Player): Boolean {
        val sourceHandMapSlot = resolveSourceHandMapSlot(player)
        val sourceHandMapId = resolveSourceHandMapId(player)
        val createdMapSnapshot = MapDataExtractor.create(player.world)
        if (createdMapSnapshot == null) {
            hooker.messageManager.sendChat(
                player,
                MessageKey.ERROR_PAINT_MAP_MISSING,
                mapOf("map" to "новая карта для мира ${player.world.name}")
            )
            return false
        }
        val mapSnapshot = sourceHandMapId?.let { MapDataExtractor.copy(it, createdMapSnapshot.mapId) } ?: createdMapSnapshot

        val inventorySnapshot = PaintInventorySnapshot.capture(player)
        hooker.playerStateManager.activateState(player, PlayerStateType.PAINTING)
        preparePaintInventory(player)

        val frameLocation = resolveFrameLocation(player)
        val frame = createFrame(player, mapSnapshot.mapId, frameLocation)
        val packetLocation = PacketLocation(
            frameLocation.x,
            frameLocation.y,
            frameLocation.z,
            frameLocation.yaw,
            frameLocation.pitch
        )
        if (!frame.spawn(packetLocation)) {
            clearPaintInventory(player)
            inventorySnapshot.restore(player)
            hooker.playerStateManager.deactivateState(player, PlayerStateType.PAINTING)
            return false
        }

        val viewerTaskId = startViewerTask(player.uniqueId)
        val session = PaintSession(
            playerId = player.uniqueId,
            frame = frame,
            frameLocation = frameLocation,
            mapId = mapSnapshot.mapId,
            sourceHandMapSlot = sourceHandMapSlot,
            inventorySnapshot = inventorySnapshot,
            viewerTaskId = viewerTaskId,
            viewers = resolveVisibleViewers(player, frameLocation).mapTo(mutableSetOf()) { it.uniqueId }
        )
        sessions[player.uniqueId] = session
        sendFullMapDataToViewers(session, mapSnapshot)
        scheduleDelayedMapDataRefresh(session.playerId, session.mapId, session.viewers.toSet())
        return true
    }

    private fun createFrame(player: Player, mapId: Int, frameLocation: Location): WrapperEntity {
        val direction = resolveDirection(player)
        val frame = WrapperEntity(EntityTypes.ITEM_FRAME)
        resolveVisibleViewers(player, frameLocation).forEach { viewer ->
            frame.addViewer(viewer.uniqueId)
        }

        val meta = frame.entityMeta as ItemFrameMeta
        meta.setOrientation(direction.orientation)
        meta.setItem(createMapItem(mapId))

        return frame
    }

    private fun resolveFrameLocation(player: Player): Location {
        val direction = resolveDirection(player)
        val baseLocation = player.location
        val y = player.eyeLocation.y - FRAME_EYE_OFFSET
        return Location(
            baseLocation.world,
            baseLocation.x + direction.offsetX * FRAME_DISTANCE,
            y,
            baseLocation.z + direction.offsetZ * FRAME_DISTANCE,
            direction.spawnYaw,
            0f
        )
    }

    private fun createMapItem(mapId: Int): ItemStack {
        val legacyTag = NBTCompound().apply {
            setTag("map", NBTInt(mapId))
            setTag("map_id", NBTInt(mapId))
        }
        return ItemStack.builder()
            .type(ItemTypes.FILLED_MAP)
            .nbt(legacyTag)
            .component(ComponentTypes.MAP_ID, mapId)
            .build()
    }

    private fun sendFullMapData(player: Player, snapshot: MapDataExtractor.Snapshot) {
        val packet = WrapperPlayServerMapData(
            snapshot.mapId,
            snapshot.scale,
            false,
            snapshot.locked,
            null,
            MAP_WIDTH,
            MAP_HEIGHT,
            0,
            0,
            snapshot.colors.copyOf()
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun sendFullMapDataToViewers(session: PaintSession, snapshot: MapDataExtractor.Snapshot) {
        session.viewers.forEach { viewerId ->
            val viewer = Bukkit.getPlayer(viewerId) ?: return@forEach
            sendFullMapData(viewer, snapshot)
        }
    }

    private fun scheduleDelayedMapDataRefresh(playerId: UUID, mapId: Int, viewerIds: Set<UUID>) {
        hooker.tickScheduler.runLater(1L) {
            if (sessions[playerId]?.mapId != mapId) return@runLater
            val snapshot = MapDataExtractor.extract(mapId) ?: return@runLater
            viewerIds.forEach { viewerId ->
                val viewer = Bukkit.getPlayer(viewerId) ?: return@forEach
                sendFullMapData(viewer, snapshot)
            }
        }
    }

    private fun startViewerTask(playerId: UUID): Int {
        var taskId = 0
        taskId = hooker.tickScheduler.runRepeating(VIEWER_UPDATE_PERIOD_TICKS, VIEWER_UPDATE_PERIOD_TICKS) {
            val session = sessions[playerId] ?: run {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            val owner = Bukkit.getPlayer(playerId)
            if (owner == null) {
                hooker.tickScheduler.cancel(taskId)
                session.frame.remove()
                sessions.remove(playerId)
                return@runRepeating
            }
            if (!owner.isOnline || owner.isDead) {
                stopPainting(owner)
                return@runRepeating
            }
            refreshViewers(owner, session)
        }
        return taskId
    }

    private fun refreshViewers(owner: Player, session: PaintSession) {
        val desiredViewers = resolveVisibleViewers(owner, session.frameLocation).associateBy { it.uniqueId }
        val currentViewers = session.viewers.toSet()

        currentViewers
            .filter { it !in desiredViewers }
            .forEach { viewerId ->
                session.frame.removeViewer(viewerId)
                session.viewers.remove(viewerId)
            }

        val snapshot = MapDataExtractor.extract(session.mapId)
        desiredViewers.values
            .filter { it.uniqueId !in currentViewers }
            .forEach { viewer ->
                session.frame.addViewer(viewer.uniqueId)
                session.viewers.add(viewer.uniqueId)
                if (snapshot != null) {
                    sendFullMapData(viewer, snapshot)
                }
                scheduleDelayedMapDataRefresh(session.playerId, session.mapId, setOf(viewer.uniqueId))
            }
    }

    private fun resolveVisibleViewers(owner: Player, frameLocation: Location): List<Player> {
        val visibilityRadius = resolveVisibilityRadius()
        return frameLocation.world?.players
            ?.filter { viewer ->
                viewer.isOnline &&
                    viewer.world == owner.world &&
                    viewer.location.distanceSquared(frameLocation) <= visibilityRadius * visibilityRadius &&
                    !hooker.utils.isHiddenFromPlayer(viewer, owner)
            }
            ?: emptyList()
    }

    private fun resolveVisibilityRadius(): Double = (Bukkit.getViewDistance() * CHUNK_SIZE).coerceAtLeast(MIN_VISIBILITY_RADIUS)

    private fun preparePaintInventory(player: Player) {
        clearPaintInventory(player)
        player.inventory.setItem(0, BukkitItemStack(Material.WHITE_DYE))
        player.inventory.setItem(1, BukkitItemStack(Material.RED_DYE))
        player.inventory.setItem(2, BukkitItemStack(Material.GREEN_DYE))
        player.inventory.setItem(3, BukkitItemStack(Material.BLUE_DYE))
        player.inventory.heldItemSlot = 0
    }

    private fun clearPaintInventory(player: Player) {
        player.inventory.storageContents = arrayOfNulls(36)
        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.extraContents = arrayOfNulls(player.inventory.extraContents.size.coerceAtLeast(1))
    }

    private fun giveResultMap(player: Player, mapId: Int) {
        val mapItem = createResultMap(mapId)
        val leftovers = player.inventory.addItem(mapItem)
        leftovers.values.forEach { item ->
            player.world.dropItem(player.location.clone().add(0.0, 1.0, 0.0), item)
        }
    }

    private fun replaceSourceHandMap(player: Player, session: PaintSession): Boolean {
        val slot = session.sourceHandMapSlot ?: return false
        val mapView = resolveMapView(session.mapId) ?: return false
        val item = player.inventory.getItem(slot)?.clone()?.takeIf { it.type == Material.FILLED_MAP }
            ?: BukkitItemStack(Material.FILLED_MAP)
        MapItemSupport.setMapView(item, mapView)
        player.inventory.setItem(slot, item)
        return true
    }

    private fun createResultMap(mapId: Int): BukkitItemStack {
        val item = BukkitItemStack(Material.FILLED_MAP)
        val mapView = resolveMapView(mapId)
        if (mapView != null) {
            MapItemSupport.setMapView(item, mapView)
        }
        return item
    }

    private fun resolveMapView(mapId: Int): MapView? = MapItemSupport.resolveMapView(mapId)

    private fun resolveSourceHandMapSlot(player: Player): Int? {
        return if (player.inventory.itemInMainHand.type == Material.FILLED_MAP) {
            player.inventory.heldItemSlot
        } else {
            null
        }
    }

    private fun resolveSourceHandMapId(player: Player): Int? {
        val item = player.inventory.itemInMainHand
        if (item.type != Material.FILLED_MAP) return null
        return MapItemSupport.mapId(item)
    }

    private fun resolveDirection(player: Player): PaintDirection {
        val normalizedYaw = ((player.location.yaw % 360f) + 360f) % 360f
        return when {
            normalizedYaw >= 45f && normalizedYaw < 135f -> PaintDirection.WEST
            normalizedYaw >= 135f && normalizedYaw < 225f -> PaintDirection.NORTH
            normalizedYaw >= 225f && normalizedYaw < 315f -> PaintDirection.EAST
            else -> PaintDirection.SOUTH
        }
    }

    private enum class PaintDirection(
        val offsetX: Double,
        val offsetZ: Double,
        val spawnYaw: Float,
        val orientation: ItemFrameMeta.Orientation
    ) {
        NORTH(0.0, -1.0, 180f, ItemFrameMeta.Orientation.SOUTH),
        SOUTH(0.0, 1.0, 0f, ItemFrameMeta.Orientation.NORTH),
        EAST(1.0, 0.0, 270f, ItemFrameMeta.Orientation.WEST),
        WEST(-1.0, 0.0, 90f, ItemFrameMeta.Orientation.EAST)
    }

    private companion object {
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
        private const val FRAME_DISTANCE = 0.75
        private const val FRAME_EYE_OFFSET = 0.25
        private const val VIEWER_UPDATE_PERIOD_TICKS = 10L
        private const val CHUNK_SIZE = 16.0
        private const val MIN_VISIBILITY_RADIUS = 32.0
    }
}
