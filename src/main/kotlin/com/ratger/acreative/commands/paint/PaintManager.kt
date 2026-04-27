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
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.map.MapView
import org.bukkit.util.Vector
import java.awt.Color
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor

class PaintManager(private val hooker: FunctionHooker) {

    private data class PaintPixelHit(
        val x: Int,
        val y: Int
    )

    private val sessions = mutableMapOf<UUID, PaintSession>()
    private val toolKey = NamespacedKey(hooker.plugin, "paint_tool_id")
    private val modeKey = NamespacedKey(hooker.plugin, "paint_tool_mode")

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

    fun handleFrameInteraction(entityId: Int): Boolean = sessions.values.any { it.frame.entityId == entityId }

    fun handleFrameUse(player: Player, entityId: Int): Boolean {
        val session = sessions.values.firstOrNull { it.frame.entityId == entityId } ?: return false
        if (session.playerId != player.uniqueId) return true
        handleInteract(player)
        return true
    }

    fun handleSwing(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        if (player.isSneaking) {
            clearStrokeState(session)
            rotatePaletteRows(player)
            return true
        }
        val tool = resolveTool(player.inventory.itemInMainHand) ?: return true
        commitCurrentLookPixel(player, session, tool)
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
        val tool = resolveTool(player.inventory.itemInMainHand) ?: return true
        commitCurrentLookPixel(player, session, tool)
        return true
    }

    fun stopPainting(player: Player) {
        val session = sessions.remove(player.uniqueId) ?: return
        hooker.tickScheduler.cancel(session.viewerTaskId)
        hooker.tickScheduler.cancel(session.paintTaskId)
        restorePreviewIfNeeded(player, session)
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
            hooker.tickScheduler.cancel(session.paintTaskId)
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

        val frameDirection = resolveDirection(player)
        val frameLocation = resolveFrameLocation(player, frameDirection)
        val frame = createFrame(player, mapSnapshot.mapId, frameLocation, frameDirection)
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
        val paintTaskId = startContinuousPaintTask(player.uniqueId)
        val session = PaintSession(
            playerId = player.uniqueId,
            frame = frame,
            frameLocation = frameLocation,
            frameDirection = frameDirection,
            mapId = mapSnapshot.mapId,
            sourceHandMapSlot = sourceHandMapSlot,
            inventorySnapshot = inventorySnapshot,
            viewerTaskId = viewerTaskId,
            paintTaskId = paintTaskId,
            viewers = resolveVisibleViewers(player, frameLocation).mapTo(mutableSetOf()) { it.uniqueId }
        )
        sessions[player.uniqueId] = session
        sendFullMapDataToViewers(session, mapSnapshot)
        scheduleDelayedMapDataRefresh(session.playerId, session.mapId, session.viewers.toSet())
        return true
    }

    private fun createFrame(
        player: Player,
        mapId: Int,
        frameLocation: Location,
        direction: PaintFrameDirection
    ): WrapperEntity {
        val frame = WrapperEntity(EntityTypes.ITEM_FRAME)
        resolveVisibleViewers(player, frameLocation).forEach { viewer ->
            frame.addViewer(viewer.uniqueId)
        }

        val meta = frame.entityMeta as ItemFrameMeta
        meta.orientation = direction.orientation
        meta.item = createMapItem(mapId)
        return frame
    }

    private fun resolveFrameLocation(player: Player, direction: PaintFrameDirection): Location {
        val baseLocation = player.location
        val desiredX = baseLocation.x + direction.offsetX * FRAME_DISTANCE
        val desiredY = player.eyeLocation.y - FRAME_EYE_OFFSET
        val desiredZ = baseLocation.z + direction.offsetZ * FRAME_DISTANCE
        val anchorX = floor(desiredX + direction.normalX * FRAME_HANGING_CENTER_OFFSET)
        val anchorY = floor(desiredY)
        val anchorZ = floor(desiredZ + direction.normalZ * FRAME_HANGING_CENTER_OFFSET)
        return Location(
            baseLocation.world,
            anchorX + 0.5 - direction.normalX * FRAME_HANGING_CENTER_OFFSET,
            anchorY + 0.5,
            anchorZ + 0.5 - direction.normalZ * FRAME_HANGING_CENTER_OFFSET,
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

    private fun startContinuousPaintTask(playerId: UUID): Int {
        var taskId = 0
        taskId = hooker.tickScheduler.runRepeating(1L, 1L) {
            val session = sessions[playerId] ?: run {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            val player = Bukkit.getPlayer(playerId) ?: return@runRepeating
            updatePreview(player, session)
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
        player.inventory.storageContents = PaintToolCatalog.buildLayout(toolKey, modeKey)
        player.inventory.heldItemSlot = 0
    }

    private fun clearPaintInventory(player: Player) {
        player.inventory.storageContents = arrayOfNulls(36)
        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.extraContents = arrayOfNulls(player.inventory.extraContents.size.coerceAtLeast(1))
    }

    private fun rotatePaletteRows(player: Player) {
        val storage = player.inventory.storageContents.copyOf()
        if (storage.size < 36) return

        val hotbar = storage.sliceArray(0..8)
        val lower = storage.sliceArray(9..17)
        val middle = storage.sliceArray(18..26)
        val upper = storage.sliceArray(27..35)

        upper.copyInto(storage, 0)
        hotbar.copyInto(storage, 9)
        lower.copyInto(storage, 18)
        middle.copyInto(storage, 27)

        player.inventory.storageContents = storage
    }

    private fun commitCurrentLookPixel(player: Player, session: PaintSession, tool: PaintToolDefinition) {
        val now = System.currentTimeMillis()
        if (now - session.lastUseAtMillis <= CLICK_DEBOUNCE_MILLIS) return
        val hit = resolveHitPixel(player, session) ?: return
        val brushColor = resolveStrokeColor(tool) ?: return
        session.lastUseAtMillis = now
        val strokePoints = resolveStrokePoints(session, hit, brushColor, now)
        if (strokePoints.isEmpty()) return

        val hasEffectiveChange = strokePoints.any { (x, y) -> resolveActualColor(session, x, y) != brushColor }
        if (!hasEffectiveChange) {
            rememberStroke(session, hit, brushColor, now)
            return
        }

        val previewOverwritten = strokePoints.any { (x, y) -> session.previewPixelX == x && session.previewPixelY == y }
        val snapshot = MapDataExtractor.setPixels(session.mapId, strokePoints, brushColor) ?: return
        if (previewOverwritten) {
            clearPreviewState(session)
        }
        rememberStroke(session, hit, brushColor, now)
        sendFullMapDataToViewers(session, snapshot)
        sendFullMapData(player, snapshot)
    }

    private fun updatePreview(player: Player, session: PaintSession) {
        val tool = resolveTool(player.inventory.itemInMainHand)
        val brushColor = tool?.let(::resolveStrokeColor)
        if (brushColor == null) {
            clearStrokeState(session)
            restorePreviewIfNeeded(player, session)
            return
        }

        val hit = resolveHitPixel(player, session)
        if (hit == null) {
            clearStrokeState(session)
            restorePreviewIfNeeded(player, session)
            return
        }

        val actualColor = resolveActualColor(session, hit.x, hit.y) ?: run {
            restorePreviewIfNeeded(player, session)
            return
        }
        if (actualColor == brushColor) {
            restorePreviewIfNeeded(player, session)
            return
        }
        val previewColor = blendPreviewColor(actualColor, brushColor)

        if (session.previewPixelX == hit.x &&
            session.previewPixelY == hit.y &&
            session.previewOriginalColor == actualColor &&
            session.previewShownColor == previewColor
        ) {
            return
        }

        restorePreviewIfNeeded(player, session)

        val snapshot = MapDataExtractor.extract(session.mapId) ?: return
        val previewColors = snapshot.colors.copyOf()
        previewColors[hit.y * MAP_WIDTH + hit.x] = previewColor
        sendPreviewMapData(player, snapshot, previewColors)
        session.previewPixelX = hit.x
        session.previewPixelY = hit.y
        session.previewOriginalColor = actualColor
        session.previewShownColor = previewColor
    }

    private fun restorePreviewIfNeeded(player: Player, session: PaintSession) {
        if (session.previewPixelX == null || session.previewPixelY == null) return
        val snapshot = MapDataExtractor.extract(session.mapId) ?: run {
            clearPreviewState(session)
            return
        }
        sendFullMapData(player, snapshot)
        clearPreviewState(session)
    }

    private fun clearPreviewState(session: PaintSession) {
        session.previewPixelX = null
        session.previewPixelY = null
        session.previewOriginalColor = null
        session.previewShownColor = null
    }

    private fun clearStrokeState(session: PaintSession) {
        session.lastStrokePixelX = null
        session.lastStrokePixelY = null
        session.lastStrokeColor = null
        session.lastStrokeAtMillis = 0L
    }

    private fun resolveActualColor(session: PaintSession, x: Int, y: Int): Byte? {
        return MapDataExtractor.colorAt(session.mapId, x, y)
    }

    private fun sendPreviewMapData(player: Player, snapshot: MapDataExtractor.Snapshot, colors: ByteArray) {
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
            colors
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun blendPreviewColor(baseColorId: Byte, brushColorId: Byte): Byte {
        val base = runCatching { MapDataExtractor.resolvePaletteColor(baseColorId) }.getOrNull() ?: return brushColorId
        val brush = runCatching { MapDataExtractor.resolvePaletteColor(brushColorId) }.getOrNull() ?: return brushColorId
        if (base.rgb == brush.rgb) return baseColorId

        val alpha = PREVIEW_ALPHA
        val red = (base.red * (1.0 - alpha) + brush.red * alpha).toInt().coerceIn(0, 255)
        val green = (base.green * (1.0 - alpha) + brush.green * alpha).toInt().coerceIn(0, 255)
        val blue = (base.blue * (1.0 - alpha) + brush.blue * alpha).toInt().coerceIn(0, 255)
        return MapColorMatcher.match(Color(red, green, blue))
    }

    private fun resolveStrokePoints(
        session: PaintSession,
        hit: PaintPixelHit,
        brushColor: Byte,
        now: Long
    ): List<Pair<Int, Int>> {
        val previousX = session.lastStrokePixelX
        val previousY = session.lastStrokePixelY
        val canContinueStroke =
            previousX != null &&
                previousY != null &&
                session.lastStrokeColor == brushColor &&
                now - session.lastStrokeAtMillis <= STROKE_CONTINUE_WINDOW_MILLIS

        return if (!canContinueStroke) {
            listOf(hit.x to hit.y)
        } else {
            traceLine(previousX, previousY, hit.x, hit.y)
        }
    }

    private fun rememberStroke(session: PaintSession, hit: PaintPixelHit, brushColor: Byte, now: Long) {
        session.lastStrokePixelX = hit.x
        session.lastStrokePixelY = hit.y
        session.lastStrokeColor = brushColor
        session.lastStrokeAtMillis = now
    }

    private fun traceLine(startX: Int, startY: Int, endX: Int, endY: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        var x = startX
        var y = startY
        val dx = abs(endX - startX)
        val dy = abs(endY - startY)
        val sx = if (startX < endX) 1 else -1
        val sy = if (startY < endY) 1 else -1
        var error = dx - dy

        while (true) {
            points += x to y
            if (x == endX && y == endY) {
                return points
            }

            val doubledError = error * 2
            if (doubledError > -dy) {
                error -= dy
                x += sx
            }
            if (doubledError < dx) {
                error += dx
                y += sy
            }
        }
    }

    private fun resolveHitPixel(player: Player, session: PaintSession): PaintPixelHit? {
        val origin = player.eyeLocation.toVector()
        val direction = player.eyeLocation.direction.clone().normalize()
        val planePoint = resolveRenderPlaneCenter(session).toVector()
        val planeNormal = Vector(session.frameDirection.normalX, 0.0, session.frameDirection.normalZ)
        val facingDot = direction.dot(planeNormal.clone().multiply(-1.0))
        if (facingDot <= MIN_CANVAS_FACING_DOT) return null

        val projectedDirection = if (facingDot >= 0.0) {
            direction
        } else {
            // Preserve sideways/up-down aim, but mirror the depth component back toward the canvas.
            direction.clone().subtract(planeNormal.clone().multiply(2.0 * direction.dot(planeNormal))).normalize()
        }

        val denominator = projectedDirection.dot(planeNormal)
        if (abs(denominator) <= PLANE_EPSILON) return null

        val t = planePoint.clone().subtract(origin).dot(planeNormal) / denominator
        if (t <= 0.0) return null

        val hit = origin.clone().add(projectedDirection.multiply(t))
        val relative = hit.clone().subtract(planePoint)
        val horizontal = relative.x * session.frameDirection.rightAxisX + relative.z * session.frameDirection.rightAxisZ
        val vertical = relative.y
        val half = FRAME_RENDER_SIZE / 2.0
        val clampedHorizontal = horizontal.coerceIn(-half, half)
        val clampedVertical = vertical.coerceIn(-half, half)

        val pixelX = floor(((clampedHorizontal + half) / FRAME_RENDER_SIZE) * MAP_WIDTH).toInt().coerceIn(0, MAP_WIDTH - 1)
        val pixelY = floor(((half - clampedVertical) / FRAME_RENDER_SIZE) * MAP_HEIGHT).toInt().coerceIn(0, MAP_HEIGHT - 1)
        return PaintPixelHit(pixelX, pixelY)
    }

    private fun resolveRenderPlaneCenter(session: PaintSession): Location {
        val anchorX = floor(session.frameLocation.x) + 0.5
        val anchorY = floor(session.frameLocation.y) + 0.5
        val anchorZ = floor(session.frameLocation.z) + 0.5
        return Location(
            session.frameLocation.world,
            anchorX - session.frameDirection.normalX * FRAME_HANGING_CENTER_OFFSET,
            anchorY,
            anchorZ - session.frameDirection.normalZ * FRAME_HANGING_CENTER_OFFSET
        )
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
        return if (player.inventory.itemInMainHand.type == Material.FILLED_MAP) player.inventory.heldItemSlot else null
    }

    private fun resolveSourceHandMapId(player: Player): Int? {
        val item = player.inventory.itemInMainHand
        if (item.type != Material.FILLED_MAP) return null
        return MapItemSupport.mapId(item)
    }

    private fun resolveStrokeColor(tool: PaintToolDefinition): Byte? = when (tool.mode) {
        PaintToolMode.COLOR_BRUSH,
        PaintToolMode.CUT -> tool.mapColor
        else -> null
    }

    private fun resolveTool(item: BukkitItemStack?): PaintToolDefinition? = PaintToolCatalog.resolve(item, toolKey)

    private fun resolveDirection(player: Player): PaintFrameDirection {
        val normalizedYaw = ((player.location.yaw % 360f) + 360f) % 360f
        return when {
            normalizedYaw >= 45f && normalizedYaw < 135f -> PaintFrameDirection.WEST
            normalizedYaw >= 135f && normalizedYaw < 225f -> PaintFrameDirection.NORTH
            normalizedYaw >= 225f && normalizedYaw < 315f -> PaintFrameDirection.EAST
            else -> PaintFrameDirection.SOUTH
        }
    }

    companion object {
        private const val MAP_WIDTH = 128
        private const val MAP_HEIGHT = 128
        private const val FRAME_DISTANCE = 0.75
        private const val FRAME_EYE_OFFSET = 0.25
        private const val FRAME_RENDER_SIZE = 1.05
        private const val FRAME_HANGING_CENTER_OFFSET = 0.46875
        private const val VIEWER_UPDATE_PERIOD_TICKS = 10L
        private const val CHUNK_SIZE = 16.0
        private const val MIN_VISIBILITY_RADIUS = 32.0
        private const val CLICK_DEBOUNCE_MILLIS = 12L
        private const val STROKE_CONTINUE_WINDOW_MILLIS = 250L
        private const val PLANE_EPSILON = 1.0E-6
        private const val MIN_CANVAS_FACING_DOT = -0.1
        private const val PREVIEW_ALPHA = 0.42
    }
}
