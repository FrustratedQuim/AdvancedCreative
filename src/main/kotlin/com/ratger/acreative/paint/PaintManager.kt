package com.ratger.acreative.paint

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.paint.PaintMenuCallbacks
import com.ratger.acreative.menus.paint.PaintMenuController
import com.ratger.acreative.menus.paint.PaintToolDefinition
import com.ratger.acreative.menus.paint.PaintToolInventoryService
import com.ratger.acreative.menus.paint.PaintToolMarker
import com.ratger.acreative.paint.artwork.PaintArtworkService
import com.ratger.acreative.paint.map.MapColorMatcher
import com.ratger.acreative.paint.map.MapDataExtractor
import com.ratger.acreative.paint.model.PaintBinaryBrushSettings
import com.ratger.acreative.paint.model.PaintBrushSettings
import com.ratger.acreative.paint.model.PaintCanvasBounds
import com.ratger.acreative.paint.model.PaintCanvasCell
import com.ratger.acreative.paint.model.PaintCanvasSize
import com.ratger.acreative.paint.model.PaintGridPoint
import com.ratger.acreative.paint.model.PaintHistoryEntry
import com.ratger.acreative.paint.model.PaintInputKind
import com.ratger.acreative.paint.model.PaintInventorySnapshot
import com.ratger.acreative.paint.model.PaintLineAnchor
import com.ratger.acreative.paint.model.PaintPixelChange
import com.ratger.acreative.paint.model.PaintResizePreview
import com.ratger.acreative.paint.model.PaintSession
import com.ratger.acreative.paint.model.PaintFrameDirection
import com.ratger.acreative.paint.model.PaintShade
import com.ratger.acreative.paint.model.PaintShapeSettings
import com.ratger.acreative.paint.model.PaintShapeType
import com.ratger.acreative.paint.model.PaintSurfacePixel
import com.ratger.acreative.paint.model.PaintToolMode
import com.ratger.acreative.paint.palette.PaintPalette
import com.ratger.acreative.paint.palette.PaintPaletteEntry
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import com.ratger.acreative.utils.SeriesCodeGenerator
import me.tofaa.entitylib.meta.other.ItemFrameMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.util.Vector
import java.awt.Color
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PaintManager(private val hooker: FunctionHooker) {

    private data class ResizeTarget(
        val point: PaintGridPoint,
        val location: Location,
        val state: ResizeTargetState
    )

    private enum class ResizeTargetState {
        ADD,
        REMOVE_OWN,
        INVALID
    }

    private data class PreviewMapOverlay(
        val mapId: Int,
        val indices: IntArray,
        val colors: ByteArray,
        val fingerprint: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PreviewMapOverlay

            if (mapId != other.mapId) return false
            if (fingerprint != other.fingerprint) return false
            if (!indices.contentEquals(other.indices)) return false
            if (!colors.contentEquals(other.colors)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mapId
            result = 31 * result + fingerprint.hashCode()
            result = 31 * result + indices.contentHashCode()
            result = 31 * result + colors.contentHashCode()
            return result
        }
    }

    private data class FillPreviewCacheEntry(
        val key: String,
        val overlays: List<PreviewMapOverlay>
    )

    private data class BrushPreviewCacheEntry(
        val key: String,
        val overlays: List<PreviewMapOverlay>
    )

    private data class BrushPixelCacheEntry(
        val key: String,
        val pixels: List<BrushMapPixels>
    )

    private data class FillComponentCache(
        val revision: Long,
        val ignoreShade: Boolean,
        val minGlobalX: Int,
        val minGlobalY: Int,
        val width: Int,
        val height: Int,
        val labels: IntArray,
        val colors: ByteArray,
        val componentSizes: IntArray,
        val componentStarts: IntArray,
        val componentIndices: IntArray
    ) {
        fun indexOf(globalX: Int, globalY: Int): Int? {
            val localX = globalX - minGlobalX
            val localY = globalY - minGlobalY
            if (localX !in 0 until width || localY !in 0 until height) {
                return null
            }
            return localY * width + localX
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FillComponentCache

            if (revision != other.revision) return false
            if (ignoreShade != other.ignoreShade) return false
            if (minGlobalX != other.minGlobalX) return false
            if (minGlobalY != other.minGlobalY) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (!labels.contentEquals(other.labels)) return false
            if (!colors.contentEquals(other.colors)) return false
            if (!componentSizes.contentEquals(other.componentSizes)) return false
            if (!componentStarts.contentEquals(other.componentStarts)) return false
            if (!componentIndices.contentEquals(other.componentIndices)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = revision.hashCode()
            result = 31 * result + ignoreShade.hashCode()
            result = 31 * result + minGlobalX
            result = 31 * result + minGlobalY
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + labels.contentHashCode()
            result = 31 * result + colors.contentHashCode()
            result = 31 * result + componentSizes.contentHashCode()
            result = 31 * result + componentStarts.contentHashCode()
            result = 31 * result + componentIndices.contentHashCode()
            return result
        }
    }

    private data class FillArea(
        val cache: FillComponentCache,
        val componentId: Int,
        val start: Int,
        val size: Int
    ) {
        fun isEmpty(): Boolean = size <= 0

        inline fun forEachIndex(action: (Int) -> Unit) {
            for (offset in 0 until size) {
                action(cache.componentIndices[start + offset])
            }
        }
    }

    private data class BrushMapPixels(
        val mapId: Int,
        val localIndices: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BrushMapPixels

            if (mapId != other.mapId) return false
            if (!localIndices.contentEquals(other.localIndices)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mapId
            result = 31 * result + localIndices.contentHashCode()
            return result
        }
    }

    private data class BrushRowInterval(
        val startX: Int,
        val endX: Int
    )

    private data class CanvasPlaneBounds(
        val minCellX: Int,
        val maxCellX: Int,
        val minCellY: Int,
        val maxCellY: Int,
        val minHorizontal: Double,
        val maxHorizontal: Double,
        val minVertical: Double,
        val maxVertical: Double
    )

    private class BrushMapPixelBuilder(private val mapId: Int) {
        private val seen = BooleanArray(128 * 128)
        private var indices = IntArray(512)
        private var size = 0

        fun put(index: Int) {
            if (index !in seen.indices || seen[index]) return
            ensureCapacity(size + 1)
            seen[index] = true
            indices[size] = index
            size += 1
        }

        fun build(): BrushMapPixels = BrushMapPixels(mapId, indices.copyOf(size))

        private fun ensureCapacity(required: Int) {
            if (required <= indices.size) return
            val newSize = max(required, indices.size * 2)
            indices = indices.copyOf(newSize)
        }
    }

    private class PreviewOverlayBuilder(private val mapId: Int) {
        private val positionsByMapIndex = IntArray(128 * 128) { -1 }
        private var indices = IntArray(256)
        private var colors = ByteArray(256)
        private var size = 0

        fun put(index: Int, color: Byte) {
            val existingPosition = positionsByMapIndex[index]
            if (existingPosition >= 0) {
                colors[existingPosition] = color
                return
            }
            ensureCapacity(size + 1)
            positionsByMapIndex[index] = size
            indices[size] = index
            colors[size] = color
            size += 1
        }

        fun build(): PreviewMapOverlay {
            val compactIndices = indices.copyOf(size)
            val compactColors = colors.copyOf(size)
            var hash = 1_125_899_906_842_597L
            for (i in 0 until size) {
                hash = hash * 1_099_511_628_211L + compactIndices[i]
                hash = hash * 1_099_511_628_211L + (compactColors[i].toInt() and 0xFF)
            }
            return PreviewMapOverlay(mapId, compactIndices, compactColors, hash)
        }

        private fun ensureCapacity(required: Int) {
            if (required <= indices.size) return
            val newSize = max(required, indices.size * 2)
            indices = indices.copyOf(newSize)
            colors = colors.copyOf(newSize)
        }
    }

    private val sessions = mutableMapOf<UUID, PaintSession>()
    private val fillPreviewCache = mutableMapOf<UUID, FillPreviewCacheEntry>()
    private val brushPreviewCache = mutableMapOf<UUID, BrushPreviewCacheEntry>()
    private val brushPixelCache = mutableMapOf<UUID, BrushPixelCacheEntry>()
    private val activePreviewOverlays = mutableMapOf<UUID, List<PreviewMapOverlay>>()
    private val fillComponentCaches = mutableMapOf<UUID, FillComponentCache>()
    private val brushStampCache = mutableMapOf<Int, IntArray>()
    private val previewBlendCache = mutableMapOf<Int, Byte>()
    private val parser = MiniMessageParser()
    private val artworkService = PaintArtworkService(hooker, parser)
    private val toolInventoryService = PaintToolInventoryService(PaintToolMarker.key(hooker.plugin), parser)
    private val menuController = PaintMenuController(
        hooker = hooker,
        parser = parser,
        callbacks = object : PaintMenuCallbacks {
            override fun isPainting(player: Player): Boolean = this@PaintManager.isPainting(player)

            override fun session(playerId: UUID): PaintSession? = sessions[playerId]

            override fun refreshTools(player: Player, session: PaintSession) {
                toolInventoryService.refresh(player, session)
            }

            override fun resolveTool(item: BukkitItemStack?): PaintToolDefinition? = this@PaintManager.resolveTool(item)

            override fun clearCanvas(player: Player, session: PaintSession) {
                this@PaintManager.clearCanvas(player, session)
            }

            override fun removeResizePreview(player: Player, session: PaintSession) {
                this@PaintManager.removeResizePreview(player, session)
            }
        }
    )
    private val suppressedDirectUseUntilMillis = ConcurrentHashMap<UUID, Long>()

    fun handlePaintCommand(player: Player, args: Array<out String>) {
        if (args.size > 1) {
            hooker.messageManager.sendChat(player, MessageKey.USAGE_PAINT)
            return
        }

        val requestedSize = PaintCanvasSize.parse(args.firstOrNull())
        if (requestedSize == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_PAINT_INVALID_SIZE)
            return
        }

        if (isPainting(player)) {
            stopPainting(player)
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_OFF)
            return
        }

        if (startPainting(player, requestedSize)) {
            hooker.messageManager.sendChat(player, MessageKey.INFO_PAINT_ON)
        }
    }

    fun isPainting(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun handleFrameInteraction(entityId: Int): Boolean = sessions.values.any { session ->
        session.canvasCells.values.any { it.frame.entityId == entityId } ||
            session.resizePreview?.frame?.entityId == entityId
    }

    fun handleFrameUse(player: Player, entityId: Int): Boolean {
        val session = sessions.values.firstOrNull { current ->
            current.canvasCells.values.any { it.frame.entityId == entityId } ||
                current.resizePreview?.frame?.entityId == entityId
        } ?: return false

        if (session.playerId != player.uniqueId) return true
        if (session.resizePreview?.frame?.entityId == entityId) {
            handleResizeActivation(player, session)
            return true
        }
        handleDirectUse(player)
        return true
    }

    fun handleSwing(player: Player): Boolean = handleDirectUse(player)

    fun handleInteract(player: Player): Boolean = handleDirectUse(player)

    fun handleLeftClick(player: Player): Boolean = handleDirectUse(player)

    fun handleDropAction(player: Player, stackDrop: Boolean): Boolean {
        val session = sessions[player.uniqueId] ?: return false
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
            undoLastAction(player, session)
            return true
        }
        if (!isWorkTool(player.inventory.itemInMainHand)) {
            return true
        } else {
            menuController.openSettingsForCurrentTool(player, session)
        }
        return true
    }

    fun handleInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return

        if (session.isMenuOpen) {
            return
        }

        val clicked = event.currentItem
        if (isWorkTool(clicked)) {
            event.isCancelled = true
            if (isDropClick(event.click)) {
                handleDropAction(player, event.click == ClickType.CONTROL_DROP)
            }
            return
        }

        val hotbarButton = event.hotbarButton
        if (hotbarButton in 0..8 && isWorkTool(player.inventory.getItem(hotbarButton))) {
            event.isCancelled = true
            return
        }

        if (isWorkTool(event.cursor)) {
            event.isCancelled = true
        }
    }

    fun handleInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        if (session.isMenuOpen) {
            return
        }
        if (event.newItems.values.any(::isWorkTool)) {
            event.isCancelled = true
            return
        }
        val topInventorySize = event.view.topInventory.size
        if (event.rawSlots.any { slot ->
                if (slot < topInventorySize) {
                    return@any false
                }
                val inventorySlot = event.view.convertSlot(slot)
                isWorkTool(player.inventory.getItem(inventorySlot))
            }
        ) {
            event.isCancelled = true
        }
    }

    fun handleInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        if (!session.isMenuOpen) return
        menuController.handleInventoryClose(player, session)
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

    fun stopPainting(player: Player) {
        val session = sessions.remove(player.uniqueId) ?: return
        fillPreviewCache.remove(player.uniqueId)
        brushPreviewCache.remove(player.uniqueId)
        brushPixelCache.remove(player.uniqueId)
        activePreviewOverlays.remove(player.uniqueId)
        fillComponentCaches.remove(player.uniqueId)
        hooker.tickScheduler.cancel(session.viewerTaskId)
        hooker.tickScheduler.cancel(session.previewTaskId)
        restorePreviewIfNeeded(player, session)
        removeResizePreview(player, session)
        session.canvasCells.values.forEach { it.frame.remove() }
        toolInventoryService.clear(player)
        session.inventorySnapshot.restore(player)
        artworkService.giveResult(player, session)
        hooker.playerStateManager.deactivateState(player, PlayerStateType.PAINTING)
    }

    fun releaseAll() {
        sessions.keys.toList().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let(::stopPainting) ?: run {
                fillPreviewCache.remove(playerId)
                brushPreviewCache.remove(playerId)
                brushPixelCache.remove(playerId)
                activePreviewOverlays.remove(playerId)
                fillComponentCaches.remove(playerId)
                sessions.remove(playerId)?.let { session ->
                    hooker.tickScheduler.cancel(session.viewerTaskId)
                    hooker.tickScheduler.cancel(session.previewTaskId)
                    session.canvasCells.values.forEach { it.frame.remove() }
                    session.resizePreview?.frame?.remove()
                }
            }
        }
    }

    private fun startPainting(player: Player, size: PaintCanvasSize): Boolean {
        val mapSnapshots = linkedMapOf<PaintGridPoint, MapDataExtractor.Snapshot>()
        size.initialPoints().forEach { point ->
            val snapshot = MapDataExtractor.create(player.world)
            if (snapshot == null) {
                hooker.messageManager.sendChat(
                    player,
                    MessageKey.ERROR_PAINT_MAP_MISSING,
                    mapOf("map" to "новая карта для мира ${player.world.name}")
                )
                return false
            }
            mapSnapshots[point] = snapshot
        }

        val inventorySnapshot = PaintInventorySnapshot.capture(player)
        val frameDirection = resolveDirection(player)
        val anchorLocation = resolveFrameLocation(player, frameDirection)
        val visibleViewers = resolveVisibleViewers(player, anchorLocation).mapTo(mutableSetOf()) { it.uniqueId }

        val canvasCells = linkedMapOf<PaintGridPoint, PaintCanvasCell>()
        mapSnapshots.forEach { (point, snapshot) ->
            val location = resolveCellLocation(anchorLocation, size.basePoint, point, frameDirection)
            val frame = createFrame(player, snapshot.mapId, location, frameDirection)
            if (!frame.spawn(PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch))) {
                canvasCells.values.forEach { it.frame.remove() }
                return false
            }
            canvasCells[point] = PaintCanvasCell(point, snapshot.mapId, frame, location)
        }

        val session = PaintSession(
            playerId = player.uniqueId,
            frameDirection = frameDirection,
            anchorLocation = anchorLocation,
            anchorPoint = size.basePoint,
            initialSize = size,
            inventorySnapshot = inventorySnapshot,
            viewerTaskId = -1,
            previewTaskId = -1,
            viewers = visibleViewers,
            canvasCells = canvasCells,
            seriesCode = SeriesCodeGenerator.generate()
        )

        sessions[player.uniqueId] = session
        hooker.playerStateManager.activateState(player, PlayerStateType.PAINTING)
        toolInventoryService.prepare(player, session)

        session.viewerTaskId = startViewerTask(player.uniqueId)
        session.previewTaskId = startPreviewTask(player.uniqueId)

        mapSnapshots.values.forEach { snapshot ->
            sendFullMapDataToViewers(session, snapshot)
            scheduleDelayedMapDataRefresh(session.playerId, snapshot.mapId, session.viewers.toSet())
        }
        return true
    }

    private fun createFrame(
        player: Player,
        mapId: Int,
        frameLocation: Location,
        direction: PaintFrameDirection
    ): WrapperEntity {
        val frame = WrapperEntity(EntityTypes.GLOW_ITEM_FRAME)
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

    private fun resolveCellLocation(
        anchorLocation: Location,
        anchorPoint: PaintGridPoint,
        point: PaintGridPoint,
        direction: PaintFrameDirection
    ): Location {
        val horizontalOffset = point.x - anchorPoint.x
        val verticalOffset = anchorPoint.y - point.y
        return anchorLocation.clone().add(
            direction.rightAxisX * horizontalOffset,
            verticalOffset.toDouble(),
            direction.rightAxisZ * horizontalOffset
        )
    }

    private fun createMapItem(mapId: Int): PacketItemStack {
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

    private fun handleDirectUse(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        if (session.isMenuOpen) return true
        if (isDirectUseSuppressed(player.uniqueId)) {
            return true
        }
        if (session.previewPaused) {
            session.previewPaused = false
            clearPreviewSuppression(session)
        }

        if (!tryConsumeInput(session, PaintInputKind.DIRECT_USE, DIRECT_USE_DEBOUNCE_MILLIS)) {
            return true
        }

        if (session.resizeMode) {
            handleResizeActivation(player, session)
            return true
        }

        if (player.isSneaking) {
            val now = System.currentTimeMillis()
            if (now - session.lastPaletteRotationAtMillis < PALETTE_ROTATION_DEBOUNCE_MILLIS) {
                return true
            }
            session.lastPaletteRotationAtMillis = now
            clearStrokeState(session)
            clearLineAnchorState(session)
            session.paletteRotation = (session.paletteRotation + 1) % 4
            toolInventoryService.refresh(player, session)
            return true
        }

        val tool = resolveTool(player.inventory.itemInMainHand) ?: return true
        if (!isLineShapeTool(tool, session)) {
            clearLineAnchorState(session)
        }
        val hit = resolveHitPixel(player, session, allowMissingCell = false, clampToCanvasBounds = true) ?: return true
        executeTool(player, session, tool, hit)
        return true
    }

    private fun executeTool(
        player: Player,
        session: PaintSession,
        tool: PaintToolDefinition,
        hit: PaintSurfacePixel
    ) {
        when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val paletteKey = requireNotNull(tool.fixedPaletteKey)
                applyBrushLikeTool(
                    session = session,
                    hit = hit,
                    settings = session.toolSettings.basicBrush(paletteKey),
                    paletteKey = paletteKey
                )
            }

            PaintToolMode.CUSTOM_BRUSH -> {
                applyBrushLikeTool(
                    session = session,
                    hit = hit,
                    settings = session.toolSettings.customBrush,
                    paletteKey = session.toolSettings.customBrush.paletteKey
                )
            }

            PaintToolMode.ERASER -> {
                applyBinaryBrushTool(session, hit, session.toolSettings.eraser, BACKGROUND_COLOR_ID)
            }

            PaintToolMode.SHEARS -> {
                applyBinaryBrushTool(session, hit, session.toolSettings.shears, MapColorMatcher.TRANSPARENT_COLOR_ID)
            }

            PaintToolMode.FILL -> {
                applyFillTool(session, hit)
            }

            PaintToolMode.SHAPE -> {
                applyShapeTool(session, hit)
            }

            PaintToolMode.EASEL -> menuController.openEaselMenu(player, session)
        }
    }

    private fun applyBrushLikeTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBrushSettings,
        paletteKey: String
    ) {
        val entry = PaintPalette.entry(paletteKey)
        val color = entry.packed(settings.shade)
        val brushPixels = resolveBrushStrokePixels(session, hit, settings.normalizedSize())
        if (brushPixels.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        val fillPercent = settings.normalizedFillPercent()
        val colors = resolveMixedPaletteColors(entry, settings.shade, settings.normalizedShadeMix())
        for (group in brushPixels) {
            val mapColors = MapDataExtractor.colorsView(group.mapId) ?: continue
            for (localIndex in group.localIndices) {
                if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) continue
                val oldColor = mapColors[localIndex]
                val newColor = randomColor(colors)
                if (oldColor == newColor) continue
                changes.getOrPut(group.mapId) { mutableListOf() }.add(
                    PaintPixelChange(localIndex % MAP_WIDTH, localIndex / MAP_WIDTH, oldColor, newColor)
                )
            }
        }

        suppressLargeBrushPreview(session, settings.normalizedSize())
        rememberStroke(session, hit, color)
        applyHistoryChanges(session, changes)
    }

    private fun applyBinaryBrushTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBinaryBrushSettings,
        color: Byte
    ) {
        val brushPixels = resolveBrushStrokePixels(session, hit, settings.normalizedSize())
        if (brushPixels.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        val fillPercent = settings.normalizedFillPercent()
        for (group in brushPixels) {
            val mapColors = MapDataExtractor.colorsView(group.mapId) ?: continue
            for (localIndex in group.localIndices) {
                if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) continue
                val oldColor = mapColors[localIndex]
                if (oldColor == color) continue
                changes.getOrPut(group.mapId) { mutableListOf() }.add(
                    PaintPixelChange(localIndex % MAP_WIDTH, localIndex / MAP_WIDTH, oldColor, color)
                )
            }
        }

        suppressLargeBrushPreview(session, settings.normalizedSize())
        rememberStroke(session, hit, color)
        applyHistoryChanges(session, changes)
    }

    private fun applyFillTool(session: PaintSession, hit: PaintSurfacePixel) {
        val now = System.currentTimeMillis()
        if (now < session.fillCooldownUntilMillis) return
        session.fillCooldownUntilMillis = now + FILL_COOLDOWN_MILLIS

        val settings = session.toolSettings.fill
        val area = resolveFillArea(session, hit, settings.ignoreShade) ?: return
        if (area.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        val entry = PaintPalette.entry(settings.paletteKey)
        val fillPercent = settings.normalizedFillPercent()
        val colors = resolveMixedPaletteColors(entry, settings.baseShade, settings.normalizedShadeMix())
        area.forEachIndex { index ->
            val oldColor = area.cache.colors[index]
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) return@forEachIndex
            val newColor = randomColor(colors)
            if (oldColor == newColor) return@forEachIndex
            val globalX = area.cache.minGlobalX + index % area.cache.width
            val globalY = area.cache.minGlobalY + index / area.cache.width
            val cellPoint = PaintGridPoint(floorDiv(globalX, MAP_WIDTH), floorDiv(globalY, MAP_HEIGHT))
            val mapId = session.canvasCells[cellPoint]?.mapId ?: return@forEachIndex
            val localX = floorMod(globalX, MAP_WIDTH)
            val localY = floorMod(globalY, MAP_HEIGHT)
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(localX, localY, oldColor, newColor)
            )
        }

        clearStrokeState(session)
        applyHistoryChanges(session, changes)
    }

    private fun applyShapeTool(session: PaintSession, hit: PaintSurfacePixel) {
        val settings = session.toolSettings.shape
        if (settings.shapeType == PaintShapeType.LINE) {
            applyLineShapeTool(session, hit, settings)
            return
        }

        val entry = PaintPalette.entry(settings.paletteKey)
        val pixels = resolveShapePixels(session, hit, settings)
        if (pixels.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        val colorsByPoint = snapshotMapColorsByPoint(session)
        val fillPercent = settings.normalizedFillPercent()
        val colors = resolveMixedPaletteColors(entry, settings.shade, settings.normalizedShadeMix())
        pixels.forEach { pixel ->
            val oldColor = resolveSnapshotColor(colorsByPoint, pixel) ?: return@forEach
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) return@forEach
            val newColor = randomColor(colors)
            if (oldColor == newColor) return@forEach
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(pixel.localX, pixel.localY, oldColor, newColor)
            )
        }

        clearStrokeState(session)
        applyHistoryChanges(session, changes)
    }

    private fun applyLineShapeTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintShapeSettings
    ) {
        val previousAnchor = session.shapeLineAnchor
        val nextAnchor = PaintLineAnchor(hit.globalX, hit.globalY)
        if (previousAnchor == null) {
            clearStrokeState(session)
            applyHistoryChanges(
                session = session,
                changesByMapId = emptyMap(),
                lineAnchorBefore = null,
                lineAnchorAfter = nextAnchor
            )
            return
        }

        val entry = PaintPalette.entry(settings.paletteKey)
        val pixels = resolveLineShapePixels(
            session = session,
            startX = previousAnchor.globalX,
            startY = previousAnchor.globalY,
            endX = hit.globalX,
            endY = hit.globalY,
            settings = settings
        )
        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        val colorsByPoint = snapshotMapColorsByPoint(session)
        val fillPercent = settings.normalizedFillPercent()
        val colors = resolveMixedPaletteColors(entry, settings.shade, settings.normalizedShadeMix())
        pixels.forEach { pixel ->
            val oldColor = resolveSnapshotColor(colorsByPoint, pixel) ?: return@forEach
            if (fillPercent < 100 && Random.nextInt(100) >= fillPercent) return@forEach
            val newColor = randomColor(colors)
            if (oldColor == newColor) return@forEach
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(pixel.localX, pixel.localY, oldColor, newColor)
            )
        }

        clearStrokeState(session)
        applyHistoryChanges(
            session = session,
            changesByMapId = changes,
            lineAnchorBefore = previousAnchor,
            lineAnchorAfter = nextAnchor
        )
    }

    private fun applyHistoryChanges(
        session: PaintSession,
        changesByMapId: Map<Int, List<PaintPixelChange>>,
        lineAnchorBefore: PaintLineAnchor? = null,
        lineAnchorAfter: PaintLineAnchor? = null
    ) {
        val hasLineAnchorChange = lineAnchorBefore != lineAnchorAfter
        if (changesByMapId.isEmpty() && !hasLineAnchorChange) return
        session.previewPaused = false
        clearPreviewSuppression(session)
        Bukkit.getPlayer(session.playerId)?.let { restorePreviewIfNeeded(it, session) }

        val deduplicated = changesByMapId.filterValues { it.isNotEmpty() }

        val patches = mutableListOf<MapDataExtractor.Patch>()
        deduplicated.forEach { (mapId, changes) ->
            val patch = MapDataExtractor.setPixelChangesPatch(mapId, changes) ?: return@forEach
            patches += patch
        }
        if (deduplicated.isNotEmpty() && patches.isEmpty()) return
        if (hasLineAnchorChange) {
            session.shapeLineAnchor = lineAnchorAfter
        }

        val historyEntry = PaintHistoryEntry(
            changesByMapId = deduplicated,
            estimatedBytes = estimateHistoryEntryBytes(deduplicated, hasLineAnchorChange),
            lineAnchorBefore = lineAnchorBefore,
            lineAnchorAfter = lineAnchorAfter
        )
        session.history += historyEntry
        session.historyBytes += historyEntry.estimatedBytes
        while (session.historyBytes > MAX_HISTORY_BYTES && session.history.isNotEmpty()) {
            val removed = session.history.removeFirst()
            session.historyBytes = (session.historyBytes - removed.estimatedBytes).coerceAtLeast(0L)
        }

        if (patches.isNotEmpty()) {
            markCanvasChanged(session)
        }
        patches.forEach { patch ->
            sendMapPatchDataToViewers(session, patch)
        }
    }

    private fun undoLastAction(player: Player, session: PaintSession) {
        if (session.history.isEmpty()) {
            return
        }
        val entry = session.history.removeLast()
        session.historyBytes = (session.historyBytes - entry.estimatedBytes).coerceAtLeast(0L)
        session.previewSuppressionKey = buildCurrentPreviewSuppressionKey(player, session)
        restorePreviewIfNeeded(player, session)
        val patches = mutableListOf<MapDataExtractor.Patch>()
        entry.changesByMapId.forEach { (mapId, changes) ->
            val patch = MapDataExtractor.setPixelChangesPatch(mapId, changes, useOldColor = true) ?: return@forEach
            patches += patch
        }
        if (entry.hasLineAnchorChange) {
            session.shapeLineAnchor = entry.lineAnchorBefore
        }
        if (patches.isNotEmpty()) {
            markCanvasChanged(session)
        }
        patches.forEach { patch ->
            sendMapPatchDataToViewers(session, patch)
        }
        clearStrokeState(session)
    }

    private fun startViewerTask(playerId: UUID): Int {
        var taskId = -1
        taskId = hooker.tickScheduler.runRepeating(VIEWER_UPDATE_PERIOD_TICKS, VIEWER_UPDATE_PERIOD_TICKS) {
            val session = sessions[playerId] ?: run {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            val owner = Bukkit.getPlayer(playerId)
            if (owner == null || !owner.isOnline || owner.isDead) {
                owner?.let(::stopPainting)
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            refreshViewers(owner, session)
        }
        return taskId
    }

    private fun startPreviewTask(playerId: UUID): Int {
        var taskId = -1
        taskId = hooker.tickScheduler.runRepeating(1L, 1L) {
            val session = sessions[playerId] ?: run {
                hooker.tickScheduler.cancel(taskId)
                return@runRepeating
            }
            session.currentTick += 1
            val player = Bukkit.getPlayer(playerId) ?: return@runRepeating
            if (session.resizeMode) {
                restorePreviewIfNeeded(player, session)
                updateResizePreview(player, session)
                return@runRepeating
            }
            updatePreview(player, session)
        }
        return taskId
    }

    private fun refreshViewers(owner: Player, session: PaintSession) {
        val desiredViewers = resolveVisibleViewers(owner, session.anchorLocation).associateBy { it.uniqueId }
        val currentViewers = session.viewers.toSet()

        currentViewers
            .filter { it !in desiredViewers }
            .forEach { viewerId ->
                session.canvasCells.values.forEach { it.frame.removeViewer(viewerId) }
                session.viewers.remove(viewerId)
            }

        desiredViewers.values
            .filter { it.uniqueId !in currentViewers }
            .forEach { viewer ->
                session.canvasCells.values.forEach { it.frame.addViewer(viewer.uniqueId) }
                session.viewers.add(viewer.uniqueId)
                session.canvasCells.values.forEach { cell ->
                    MapDataExtractor.extract(cell.mapId)?.let { snapshot ->
                        sendFullMapData(viewer, snapshot)
                    }
                }
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

    private fun updatePreview(player: Player, session: PaintSession) {
        if (session.isMenuOpen) {
            clearPreviewSuppression(session)
            restorePreviewIfNeeded(player, session)
            return
        }
        if (session.previewPaused && isDirectUseSuppressed(player.uniqueId)) {
            restorePreviewIfNeeded(player, session)
            return
        }
        if (session.previewPaused) {
            session.previewPaused = false
            clearPreviewSuppression(session)
        }

        val tool = resolveTool(player.inventory.itemInMainHand) ?: run {
            clearPreviewSuppression(session)
            clearStrokeState(session)
            clearLineAnchorState(session)
            restorePreviewIfNeeded(player, session)
            return
        }
        if (!isLineShapeTool(tool, session)) {
            clearLineAnchorState(session)
        }
        if (tool.mode == PaintToolMode.EASEL) {
            clearPreviewSuppression(session)
            clearStrokeState(session)
            clearLineAnchorState(session)
            restorePreviewIfNeeded(player, session)
            return
        }
        if (shouldSkipBrushPreviewDuringActiveStroke(session, tool)) {
            restorePreviewIfNeeded(player, session)
            return
        }

        val hit = resolveHitPixel(player, session, allowMissingCell = false, clampToCanvasBounds = true) ?: run {
            clearPreviewSuppression(session)
            clearStrokeState(session)
            restorePreviewIfNeeded(player, session)
            return
        }

        val overlays = resolvePreviewOverlays(session, tool, hit)
        val fingerprint = overlays.joinToString("|") { overlay ->
            "${overlay.mapId}:${overlay.indices.size}:${overlay.fingerprint}"
        }
        val suppressionKey = buildPreviewSuppressionKey(tool.id, fingerprint)

        if (session.previewSuppressionKey == suppressionKey) {
            restorePreviewIfNeeded(player, session)
            return
        }
        if (session.previewSuppressionKey != null) {
            clearPreviewSuppression(session)
        }

        if (fingerprint == session.previewFingerprint) {
            return
        }

        restorePreviewIfNeeded(player, session)
        overlays.forEach { overlay ->
            val patch = MapDataExtractor.extractPatch(overlay.mapId, overlay.indices, overlay.colors) ?: return@forEach
            sendMapPatchData(player, patch)
        }
        session.previewMapIds = overlays.mapTo(mutableSetOf()) { it.mapId }
        session.previewFingerprint = fingerprint
        activePreviewOverlays[session.playerId] = overlays
    }

    private fun resolvePreviewOverlays(
        session: PaintSession,
        tool: PaintToolDefinition,
        hit: PaintSurfacePixel
    ): List<PreviewMapOverlay> {
        return when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val paletteKey = requireNotNull(tool.fixedPaletteKey)
                val settings = session.toolSettings.basicBrush(paletteKey)
                val palette = PaintPalette.entry(paletteKey)
                resolveBrushPreviewOverlays(
                    session = session,
                    toolId = tool.id,
                    hit = hit,
                    size = settings.normalizedSize(),
                    previewColor = palette.packed(settings.shade)
                )
            }

            PaintToolMode.CUSTOM_BRUSH -> {
                val settings = session.toolSettings.customBrush
                resolveBrushPreviewOverlays(
                    session = session,
                    toolId = tool.id,
                    hit = hit,
                    size = settings.normalizedSize(),
                    previewColor = PaintPalette.entry(settings.paletteKey).packed(settings.shade)
                )
            }

            PaintToolMode.ERASER -> resolveBrushPreviewOverlays(
                session = session,
                toolId = tool.id,
                hit = hit,
                size = session.toolSettings.eraser.normalizedSize(),
                previewColor = BACKGROUND_COLOR_ID
            )

            PaintToolMode.SHEARS -> resolveBrushPreviewOverlays(
                session = session,
                toolId = tool.id,
                hit = hit,
                size = session.toolSettings.shears.normalizedSize(),
                previewColor = MapColorMatcher.TRANSPARENT_COLOR_ID
            )

            PaintToolMode.FILL -> resolveFillPreviewOverlays(session, hit)

            PaintToolMode.SHAPE -> {
                val settings = session.toolSettings.shape
                val shapePixels = if (settings.shapeType == PaintShapeType.LINE) {
                    val anchor = session.shapeLineAnchor ?: return emptyList()
                    resolveLineShapePixels(session, anchor.globalX, anchor.globalY, hit.globalX, hit.globalY, settings)
                } else {
                    resolveShapePixels(session, hit, settings)
                }
                buildPreviewOverlays(session, shapePixels, PaintPalette.entry(settings.paletteKey).packed(settings.shade))
            }

            PaintToolMode.EASEL -> emptyList()
        }
    }

    private fun resolveFillPreviewOverlays(session: PaintSession, hit: PaintSurfacePixel): List<PreviewMapOverlay> {
        val settings = session.toolSettings.fill
        val (cache, componentId) = resolveFillComponent(session, hit, settings.ignoreShade) ?: return emptyList()
        val cacheKey = buildFillPreviewCacheKey(session, cache, componentId)
        fillPreviewCache[session.playerId]?.takeIf { it.key == cacheKey }?.let { return it.overlays }
        val area = resolveFillArea(cache, componentId) ?: return emptyList()

        val overlays = buildFillPreviewOverlays(
            session = session,
            area = area,
            previewColor = PaintPalette.entry(settings.paletteKey).packed(settings.baseShade)
        )
        fillPreviewCache[session.playerId] = FillPreviewCacheEntry(cacheKey, overlays)
        return overlays
    }

    private fun resolveBrushPreviewOverlays(
        session: PaintSession,
        toolId: String,
        hit: PaintSurfacePixel,
        size: Int,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        val now = System.currentTimeMillis()
        val strokeStart = resolveStrokeContinuationStart(session, now)
        val cacheKey = buildBrushPreviewCacheKey(session, toolId, hit, size, previewColor, strokeStart)
        brushPreviewCache[session.playerId]?.takeIf { it.key == cacheKey }?.let { return it.overlays }

        val overlays = buildBrushPreviewOverlays(session, hit, strokeStart, size, previewColor)
        brushPreviewCache[session.playerId] = BrushPreviewCacheEntry(cacheKey, overlays)
        return overlays
    }

    private fun buildBrushPreviewOverlays(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        val brushPixels = resolveCachedBrushPixels(session, hit, strokeStart, size)
        if (brushPixels.isEmpty()) return emptyList()
        val overlays = mutableListOf<PreviewMapOverlay>()
        for (group in brushPixels) {
            val mapColors = MapDataExtractor.colorsView(group.mapId) ?: continue
            val builder = PreviewOverlayBuilder(group.mapId)
            for (localIndex in group.localIndices) {
                builder.put(localIndex, blendPreviewColor(mapColors[localIndex], previewColor))
            }
            overlays += builder.build()
        }
        return overlays
    }

    private fun buildBrushPreviewCacheKey(
        session: PaintSession,
        toolId: String,
        hit: PaintSurfacePixel,
        size: Int,
        previewColor: Byte,
        strokeStart: Pair<Int, Int>?
    ): String {
        return listOf(
            session.canvasRevision,
            toolId,
            hit.globalX,
            hit.globalY,
            size,
            previewColor.toInt() and 0xFF,
            strokeStart?.first ?: "x",
            strokeStart?.second ?: "y"
        ).joinToString("|")
    }

    private fun buildPreviewOverlays(
        session: PaintSession,
        pixels: List<PaintSurfacePixel>,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        if (pixels.isEmpty()) return emptyList()
        val colorsByMapId = snapshotMapColorsById(session)
        val grouped = mutableMapOf<Int, PreviewOverlayBuilder>()
        pixels.forEach { pixel ->
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            val localIndex = pixel.localY * MAP_WIDTH + pixel.localX
            val mapColors = colorsByMapId[mapId] ?: return@forEach
            if (localIndex !in mapColors.indices) return@forEach
            val oldColor = mapColors[localIndex]
            val blended = blendPreviewColor(oldColor, previewColor)
            grouped.getOrPut(mapId) { PreviewOverlayBuilder(mapId) }.put(localIndex, blended)
        }

        return grouped.values.map { it.build() }
    }

    private fun snapshotMapColorsById(session: PaintSession): Map<Int, ByteArray?> {
        return session.canvasCells.values.associate { cell ->
            cell.mapId to MapDataExtractor.extract(cell.mapId)?.colors
        }
    }

    private fun snapshotMapColorsByPoint(session: PaintSession): Map<PaintGridPoint, ByteArray?> {
        return session.canvasCells.mapValues { (_, cell) ->
            MapDataExtractor.extract(cell.mapId)?.colors
        }
    }

    private fun resolveSnapshotColor(
        colorsByPoint: Map<PaintGridPoint, ByteArray?>,
        pixel: PaintSurfacePixel
    ): Byte? {
        val colors = colorsByPoint[pixel.cellPoint] ?: return null
        val index = pixel.localY * MAP_WIDTH + pixel.localX
        if (index !in colors.indices) return null
        return colors[index]
    }

    private fun buildFillPreviewOverlays(
        session: PaintSession,
        area: FillArea,
        previewColor: Byte
    ): List<PreviewMapOverlay> {
        if (area.isEmpty()) return emptyList()
        val grouped = mutableMapOf<Int, PreviewOverlayBuilder>()
        area.forEachIndex { index ->
            val globalX = area.cache.minGlobalX + index % area.cache.width
            val globalY = area.cache.minGlobalY + index / area.cache.width
            val cellPoint = PaintGridPoint(floorDiv(globalX, MAP_WIDTH), floorDiv(globalY, MAP_HEIGHT))
            val mapId = session.canvasCells[cellPoint]?.mapId ?: return@forEachIndex
            val localIndex = floorMod(globalY, MAP_HEIGHT) * MAP_WIDTH + floorMod(globalX, MAP_WIDTH)
            val blended = blendPreviewColor(area.cache.colors[index], previewColor)
            grouped.getOrPut(mapId) { PreviewOverlayBuilder(mapId) }.put(localIndex, blended)
        }
        return grouped.values.map { it.build() }
    }

    private fun buildFillPreviewCacheKey(
        session: PaintSession,
        cache: FillComponentCache,
        componentId: Int
    ): String {
        val settings = session.toolSettings.fill
        return listOf(
            session.canvasRevision,
            componentId,
            settings.paletteKey,
            settings.baseShade.name,
            cache.ignoreShade
        ).joinToString("|")
    }

    private fun restorePreviewIfNeeded(player: Player, session: PaintSession) {
        val overlays = activePreviewOverlays.remove(session.playerId)
        if (overlays.isNullOrEmpty()) {
            if (session.previewMapIds.isEmpty()) return
            session.previewMapIds.forEach { mapId ->
                MapDataExtractor.extract(mapId)?.let { snapshot ->
                    sendFullMapData(player, snapshot)
                }
            }
        } else {
            overlays.forEach { overlay ->
                val patch = MapDataExtractor.extractPatch(overlay.mapId, overlay.indices) ?: return@forEach
                sendMapPatchData(player, patch)
            }
        }
        session.previewMapIds.clear()
        session.previewFingerprint = null
    }

    private fun clearStrokeState(session: PaintSession) {
        session.lastStrokeGlobalX = null
        session.lastStrokeGlobalY = null
        session.lastStrokeColor = null
        session.lastStrokeAtMillis = 0L
        session.brushPreviewSuppressedUntilMillis = 0L
        brushPreviewCache.remove(session.playerId)
    }

    private fun clearLineAnchorState(session: PaintSession) {
        session.shapeLineAnchor = null
    }

    private fun clearPreviewSuppression(session: PaintSession) {
        session.previewSuppressionKey = null
    }

    private fun suppressLargeBrushPreview(session: PaintSession, brushSize: Int) {
        if (brushSize < LARGE_BRUSH_PREVIEW_SUPPRESS_SIZE) return
        session.brushPreviewSuppressedUntilMillis = System.currentTimeMillis() + LARGE_BRUSH_PREVIEW_SUPPRESS_MILLIS
    }

    private fun shouldSkipBrushPreviewDuringActiveStroke(session: PaintSession, tool: PaintToolDefinition): Boolean {
        if (System.currentTimeMillis() > session.brushPreviewSuppressedUntilMillis) return false
        return when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH,
            PaintToolMode.CUSTOM_BRUSH,
            PaintToolMode.ERASER,
            PaintToolMode.SHEARS -> true
            else -> false
        }
    }

    private fun isDirectUseSuppressed(playerId: UUID): Boolean {
        val untilMillis = suppressedDirectUseUntilMillis[playerId] ?: return false
        val now = System.currentTimeMillis()
        if (now > untilMillis) {
            suppressedDirectUseUntilMillis.remove(playerId, untilMillis)
            return false
        }
        return true
    }

    private fun clearHistory(session: PaintSession) {
        session.history.clear()
        session.historyBytes = 0L
        clearLineAnchorState(session)
        clearFillPreviewCache(session)
    }

    private fun markCanvasChanged(session: PaintSession) {
        session.canvasRevision += 1
        brushPreviewCache.remove(session.playerId)
        clearFillPreviewCache(session)
        fillComponentCaches.remove(session.playerId)
    }

    private fun markCanvasTopologyChanged(session: PaintSession) {
        session.canvasTopologyRevision += 1
        brushPixelCache.remove(session.playerId)
        markCanvasChanged(session)
    }

    private fun clearFillPreviewCache(session: PaintSession) {
        fillPreviewCache.remove(session.playerId)
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

    fun resyncHeldToolSlot(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        toolInventoryService.resyncHeldToolSlot(player, session)
    }

    fun handleHeldToolChange(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        clearLineAnchorState(session)
        clearStrokeState(session)
        clearPreviewSuppression(session)
        restorePreviewIfNeeded(player, session)
    }

    fun suppressDirectUseAfterDrop(player: Player) {
        val untilMillis = System.currentTimeMillis() + DIRECT_USE_SUPPRESS_AFTER_DROP_MILLIS
        suppressedDirectUseUntilMillis[player.uniqueId] = untilMillis
    }

    private fun buildCurrentPreviewSuppressionKey(player: Player, session: PaintSession): String? {
        val tool = resolveTool(player.inventory.itemInMainHand) ?: return null
        val fingerprint = session.previewFingerprint ?: return null
        return buildPreviewSuppressionKey(tool.id, fingerprint)
    }

    private fun buildPreviewSuppressionKey(toolId: String, fingerprint: String): String {
        return "$toolId|$fingerprint"
    }

    private fun estimateHistoryEntryBytes(
        changesByMapId: Map<Int, List<PaintPixelChange>>,
        hasLineAnchorChange: Boolean
    ): Long {
        val lineAnchorBytes = if (hasLineAnchorChange) HISTORY_LINE_ANCHOR_ESTIMATE_BYTES else 0L
        return HISTORY_ENTRY_BASE_BYTES + lineAnchorBytes + changesByMapId.entries.sumOf { (_, changes) ->
            HISTORY_MAP_GROUP_BASE_BYTES + changes.size * HISTORY_PIXEL_ESTIMATE_BYTES
        }
    }

    private fun blendPreviewColor(baseColorId: Byte, brushColorId: Byte): Byte {
        if (brushColorId == MapColorMatcher.TRANSPARENT_COLOR_ID) {
            return brushColorId
        }
        val cacheKey = ((baseColorId.toInt() and 0xFF) shl 8) or (brushColorId.toInt() and 0xFF)
        previewBlendCache[cacheKey]?.let { return it }
        val base = runCatching { MapDataExtractor.resolvePaletteColor(baseColorId) }.getOrNull() ?: return brushColorId
        val brush = runCatching { MapDataExtractor.resolvePaletteColor(brushColorId) }.getOrNull() ?: return brushColorId
        if (base.rgb == brush.rgb) return baseColorId.also { previewBlendCache[cacheKey] = it }

        val alpha = PREVIEW_ALPHA
        val red = (base.red * (1.0 - alpha) + brush.red * alpha).toInt().coerceIn(0, 255)
        val green = (base.green * (1.0 - alpha) + brush.green * alpha).toInt().coerceIn(0, 255)
        val blue = (base.blue * (1.0 - alpha) + brush.blue * alpha).toInt().coerceIn(0, 255)
        return MapColorMatcher.match(Color(red, green, blue)).also { previewBlendCache[cacheKey] = it }
    }

    private fun resolveBrushStrokePixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        size: Int
    ): List<BrushMapPixels> {
        return resolveCachedBrushPixels(
            session = session,
            hit = hit,
            strokeStart = resolveStrokeContinuationStart(session, System.currentTimeMillis()),
            size = size
        )
    }

    private fun resolveStrokeContinuationStart(session: PaintSession, now: Long): Pair<Int, Int>? {
        val previousX = session.lastStrokeGlobalX
        val previousY = session.lastStrokeGlobalY
        val canContinueStroke =
            previousX != null &&
                previousY != null &&
                now - session.lastStrokeAtMillis <= STROKE_CONTINUE_WINDOW_MILLIS
        return if (canContinueStroke) previousX to previousY else null
    }

    private fun resolveCachedBrushPixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int
    ): List<BrushMapPixels> {
        val cacheKey = buildBrushPixelCacheKey(session, hit, strokeStart, size)
        brushPixelCache[session.playerId]?.takeIf { it.key == cacheKey }?.let { return it.pixels }
        val pixels = collectBrushPixels(session, hit, strokeStart, size)
        brushPixelCache[session.playerId] = BrushPixelCacheEntry(cacheKey, pixels)
        return pixels
    }

    private fun buildBrushPixelCacheKey(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int
    ): String {
        return listOf(
            session.canvasTopologyRevision,
            hit.globalX,
            hit.globalY,
            size,
            strokeStart?.first ?: "x",
            strokeStart?.second ?: "y"
        ).joinToString("|")
    }

    private fun collectBrushPixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        strokeStart: Pair<Int, Int>?,
        size: Int
    ): List<BrushMapPixels> {
        if (strokeStart != null) {
            return collectBrushLinePixels(session, strokeStart.first, strokeStart.second, hit.globalX, hit.globalY, size)
        }
        return collectBrushStampPixels(session, hit.globalX, hit.globalY, size)
    }

    private fun collectBrushStampPixels(
        session: PaintSession,
        centerX: Int,
        centerY: Int,
        size: Int
    ): List<BrushMapPixels> {
        val offsets = brushStampOffsets(size)
        val grouped = mutableMapOf<Int, BrushMapPixelBuilder>()
        val cellCache = mutableMapOf<Long, PaintCanvasCell?>()
        var offsetIndex = 0
        while (offsetIndex < offsets.size) {
            putBrushPixel(
                session = session,
                grouped = grouped,
                cellCache = cellCache,
                pixelGlobalX = centerX + offsets[offsetIndex],
                pixelGlobalY = centerY + offsets[offsetIndex + 1]
            )
            offsetIndex += 2
        }
        return grouped.values.map { it.build() }
    }

    private fun collectBrushLinePixels(
        session: PaintSession,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        size: Int
    ): List<BrushMapPixels> {
        val radius = max(0, size.coerceIn(1, 50) - 1)
        if (radius == 0) {
            val grouped = mutableMapOf<Int, BrushMapPixelBuilder>()
            val cellCache = mutableMapOf<Long, PaintCanvasCell?>()
            traceLine(startX, startY, endX, endY).forEach { (globalX, globalY) ->
                putBrushPixel(session, grouped, cellCache, globalX, globalY)
            }
            return grouped.values.map { it.build() }
        }
        if (startX == endX && startY == endY) {
            return collectBrushStampPixels(session, endX, endY, size)
        }

        val brushRadius = radius.toDouble()
        val grouped = mutableMapOf<Int, BrushMapPixelBuilder>()
        val cellCache = mutableMapOf<Long, PaintCanvasCell?>()
        for (globalY in minOf(startY, endY) - radius..maxOf(startY, endY) + radius) {
            val intervals = mutableListOf<BrushRowInterval>()
            addCircleRowInterval(intervals, startX, startY, globalY, brushRadius)
            addCircleRowInterval(intervals, endX, endY, globalY, brushRadius)
            addSegmentBodyRowInterval(intervals, startX, startY, endX, endY, globalY, brushRadius)
            putMergedBrushRowIntervals(session, grouped, cellCache, globalY, intervals)
        }
        return grouped.values.map { it.build() }
    }

    private fun addCircleRowInterval(
        intervals: MutableList<BrushRowInterval>,
        centerX: Int,
        centerY: Int,
        rowY: Int,
        radius: Double
    ) {
        val deltaY = rowY - centerY
        val remaining = radius * radius - deltaY * deltaY
        if (remaining < 0.0) return
        val span = sqrt(remaining)
        intervals += BrushRowInterval(
            startX = ceil(centerX - span - ROW_INTERVAL_EPSILON).toInt(),
            endX = floor(centerX + span + ROW_INTERVAL_EPSILON).toInt()
        )
    }

    private fun addSegmentBodyRowInterval(
        intervals: MutableList<BrushRowInterval>,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        rowY: Int,
        radius: Double
    ) {
        val segmentX = (endX - startX).toDouble()
        val segmentY = (endY - startY).toDouble()
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared <= 0.0) return

        val rowDeltaY = (rowY - startY).toDouble()
        var minX = Double.NEGATIVE_INFINITY
        var maxX = Double.POSITIVE_INFINITY

        fun intersectLinearRange(coefficient: Double, constant: Double, rangeStart: Double, rangeEnd: Double): Boolean {
            if (abs(coefficient) <= ROW_INTERVAL_EPSILON) {
                return constant + ROW_INTERVAL_EPSILON >= rangeStart && constant - ROW_INTERVAL_EPSILON <= rangeEnd
            }
            val first = (rangeStart - constant) / coefficient
            val second = (rangeEnd - constant) / coefficient
            minX = max(minX, minOf(first, second))
            maxX = minOf(maxX, maxOf(first, second))
            return minX <= maxX + ROW_INTERVAL_EPSILON
        }

        val projectionConstant = -startX * segmentX + rowDeltaY * segmentY
        if (!intersectLinearRange(segmentX, projectionConstant, 0.0, lengthSquared)) return

        val length = sqrt(lengthSquared)
        val perpendicularConstant = -startX * segmentY - rowDeltaY * segmentX
        if (!intersectLinearRange(segmentY, perpendicularConstant, -radius * length, radius * length)) return

        intervals += BrushRowInterval(
            startX = ceil(minX - ROW_INTERVAL_EPSILON).toInt(),
            endX = floor(maxX + ROW_INTERVAL_EPSILON).toInt()
        )
    }

    private fun putMergedBrushRowIntervals(
        session: PaintSession,
        grouped: MutableMap<Int, BrushMapPixelBuilder>,
        cellCache: MutableMap<Long, PaintCanvasCell?>,
        globalY: Int,
        intervals: MutableList<BrushRowInterval>
    ) {
        if (intervals.isEmpty()) return
        intervals.sortBy { it.startX }
        var currentStart = intervals.first().startX
        var currentEnd = intervals.first().endX
        for (index in 1 until intervals.size) {
            val interval = intervals[index]
            if (interval.startX <= currentEnd + 1) {
                currentEnd = max(currentEnd, interval.endX)
                continue
            }
            putBrushRow(session, grouped, cellCache, globalY, currentStart, currentEnd)
            currentStart = interval.startX
            currentEnd = interval.endX
        }
        putBrushRow(session, grouped, cellCache, globalY, currentStart, currentEnd)
    }

    private fun putBrushRow(
        session: PaintSession,
        grouped: MutableMap<Int, BrushMapPixelBuilder>,
        cellCache: MutableMap<Long, PaintCanvasCell?>,
        globalY: Int,
        startX: Int,
        endX: Int
    ) {
        if (endX < startX) return
        val cellY = floorDiv(globalY, MAP_HEIGHT)
        val localY = floorMod(globalY, MAP_HEIGHT)
        var globalX = startX
        while (globalX <= endX) {
            val cellX = floorDiv(globalX, MAP_WIDTH)
            val cellEndX = minOf(endX, (cellX + 1) * MAP_WIDTH - 1)
            val cellKey = packGridPoint(cellX, cellY)
            val cell = if (cellCache.containsKey(cellKey)) {
                cellCache[cellKey]
            } else {
                session.canvasCells[PaintGridPoint(cellX, cellY)].also { cellCache[cellKey] = it }
            }
            if (cell != null) {
                val builder = grouped.getOrPut(cell.mapId) { BrushMapPixelBuilder(cell.mapId) }
                val rowOffset = localY * MAP_WIDTH
                for (localX in floorMod(globalX, MAP_WIDTH)..floorMod(cellEndX, MAP_WIDTH)) {
                    builder.put(rowOffset + localX)
                }
            }
            globalX = cellEndX + 1
        }
    }

    private fun putBrushPixel(
        session: PaintSession,
        grouped: MutableMap<Int, BrushMapPixelBuilder>,
        cellCache: MutableMap<Long, PaintCanvasCell?>,
        pixelGlobalX: Int,
        pixelGlobalY: Int
    ) {
        val cellX = floorDiv(pixelGlobalX, MAP_WIDTH)
        val cellY = floorDiv(pixelGlobalY, MAP_HEIGHT)
        val cellKey = packGridPoint(cellX, cellY)
        val cell = if (cellCache.containsKey(cellKey)) {
            cellCache[cellKey]
        } else {
            session.canvasCells[PaintGridPoint(cellX, cellY)].also { cellCache[cellKey] = it }
        } ?: return
        val localIndex = floorMod(pixelGlobalY, MAP_HEIGHT) * MAP_WIDTH + floorMod(pixelGlobalX, MAP_WIDTH)
        grouped.getOrPut(cell.mapId) { BrushMapPixelBuilder(cell.mapId) }.put(localIndex)
    }

    private fun rememberStroke(session: PaintSession, hit: PaintSurfacePixel, color: Byte) {
        session.lastStrokeGlobalX = hit.globalX
        session.lastStrokeGlobalY = hit.globalY
        session.lastStrokeColor = color
        session.lastStrokeAtMillis = System.currentTimeMillis()
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

    private fun brushStampOffsets(size: Int): IntArray {
        val normalizedSize = size.coerceIn(1, 50)
        brushStampCache[normalizedSize]?.let { return it }
        val radius = max(0, normalizedSize - 1)
        val offsets = mutableListOf<Int>()
        for (offsetY in -radius..radius) {
            for (offsetX in -radius..radius) {
                if (offsetX * offsetX + offsetY * offsetY > radius * radius) continue
                offsets += offsetX
                offsets += offsetY
            }
        }
        return offsets.toIntArray().also { brushStampCache[normalizedSize] = it }
    }

    private fun resolvePixelByGlobal(session: PaintSession, globalX: Int, globalY: Int): PaintSurfacePixel? {
        val cellX = floorDiv(globalX, MAP_WIDTH)
        val cellY = floorDiv(globalY, MAP_HEIGHT)
        val point = PaintGridPoint(cellX, cellY)
        if (!session.canvasCells.containsKey(point)) return null
        val localX = floorMod(globalX, MAP_WIDTH)
        val localY = floorMod(globalY, MAP_HEIGHT)
        return PaintSurfacePixel(point, localX, localY, globalX, globalY)
    }

    private fun resolveHitPixel(
        player: Player,
        session: PaintSession,
        allowMissingCell: Boolean,
        clampToCanvasBounds: Boolean = false
    ): PaintSurfacePixel? {
        val origin = player.eyeLocation.toVector()
        val direction = player.eyeLocation.direction.clone().normalize()
        val planePoint = resolveRenderPlaneCenter(session.anchorLocation, session.frameDirection).toVector()
        val planeNormal = Vector(session.frameDirection.normalX, 0.0, session.frameDirection.normalZ)
        val facingDot = direction.dot(planeNormal.clone().multiply(-1.0))
        if (facingDot <= MIN_CANVAS_FACING_DOT) return null

        val denominator = direction.dot(planeNormal)
        if (abs(denominator) <= PLANE_EPSILON) return null
        val t = planePoint.clone().subtract(origin).dot(planeNormal) / denominator
        if (t <= 0.0) return null

        val hit = origin.clone().add(direction.multiply(t))
        val relative = hit.clone().subtract(planePoint)
        val canvasBounds = if (clampToCanvasBounds) resolveCanvasPlaneBounds(session) else null
        val half = FRAME_RENDER_SIZE / 2.0
        val rawHorizontal = relative.x * session.frameDirection.rightAxisX + relative.z * session.frameDirection.rightAxisZ
        val rawVertical = relative.y
        val horizontal = canvasBounds?.let { rawHorizontal.coerceIn(it.minHorizontal, it.maxHorizontal) } ?: rawHorizontal
        val vertical = canvasBounds?.let { rawVertical.coerceIn(it.minVertical, it.maxVertical) } ?: rawVertical

        var cellX = floor(horizontal + session.anchorPoint.x.toDouble() + 0.5).toInt()
        var cellY = session.anchorPoint.y - floor(vertical + 0.5).toInt()
        if (canvasBounds != null) {
            cellX = cellX.coerceIn(canvasBounds.minCellX, canvasBounds.maxCellX)
            cellY = cellY.coerceIn(canvasBounds.minCellY, canvasBounds.maxCellY)
        }

        var point = PaintGridPoint(cellX, cellY)
        if (canvasBounds != null && point !in session.canvasCells) {
            point = resolveNearestCanvasCellPoint(session, horizontal, vertical) ?: return null
            cellX = point.x
            cellY = point.y
        }
        if (!allowMissingCell && point !in session.canvasCells) return null

        val localHorizontal = (horizontal - (cellX - session.anchorPoint.x).toDouble()).let { value ->
            if (canvasBounds == null) value else value.coerceIn(-half, half)
        }
        val localVertical = (vertical - (session.anchorPoint.y - cellY).toDouble()).let { value ->
            if (canvasBounds == null) value else value.coerceIn(-half, half)
        }
        if (canvasBounds == null && (localHorizontal < -half || localHorizontal > half || localVertical < -half || localVertical > half)) {
            return null
        }

        val pixelX = floor(((localHorizontal + half) / FRAME_RENDER_SIZE) * MAP_WIDTH).toInt().coerceIn(0, MAP_WIDTH - 1)
        val pixelY = floor(((half - localVertical) / FRAME_RENDER_SIZE) * MAP_HEIGHT).toInt().coerceIn(0, MAP_HEIGHT - 1)
        return PaintSurfacePixel(
            cellPoint = point,
            localX = pixelX,
            localY = pixelY,
            globalX = cellX * MAP_WIDTH + pixelX,
            globalY = cellY * MAP_HEIGHT + pixelY
        )
    }

    private fun resolveCanvasPlaneBounds(session: PaintSession): CanvasPlaneBounds? {
        val bounds = PaintCanvasBounds.from(session.canvasCells.keys) ?: return null
        val half = FRAME_RENDER_SIZE / 2.0
        val minHorizontal = bounds.minX.toDouble() - session.anchorPoint.x.toDouble() - half
        val maxHorizontal = bounds.maxX.toDouble() - session.anchorPoint.x.toDouble() + half
        val minVertical = session.anchorPoint.y.toDouble() - bounds.maxY.toDouble() - half
        val maxVertical = session.anchorPoint.y.toDouble() - bounds.minY.toDouble() + half
        return CanvasPlaneBounds(
            minCellX = bounds.minX,
            maxCellX = bounds.maxX,
            minCellY = bounds.minY,
            maxCellY = bounds.maxY,
            minHorizontal = minHorizontal,
            maxHorizontal = maxHorizontal,
            minVertical = minVertical,
            maxVertical = maxVertical
        )
    }

    private fun resolveNearestCanvasCellPoint(
        session: PaintSession,
        horizontal: Double,
        vertical: Double
    ): PaintGridPoint? {
        val half = FRAME_RENDER_SIZE / 2.0
        return session.canvasCells.keys.minByOrNull { point ->
            val centerHorizontal = point.x.toDouble() - session.anchorPoint.x.toDouble()
            val centerVertical = session.anchorPoint.y.toDouble() - point.y.toDouble()
            val nearestHorizontal = horizontal.coerceIn(centerHorizontal - half, centerHorizontal + half)
            val nearestVertical = vertical.coerceIn(centerVertical - half, centerVertical + half)
            val deltaHorizontal = horizontal - nearestHorizontal
            val deltaVertical = vertical - nearestVertical
            deltaHorizontal * deltaHorizontal + deltaVertical * deltaVertical
        }
    }

    private fun resolveRenderPlaneCenter(anchorLocation: Location, direction: PaintFrameDirection): Location {
        val anchorX = floor(anchorLocation.x) + 0.5
        val anchorY = floor(anchorLocation.y) + 0.5
        val anchorZ = floor(anchorLocation.z) + 0.5
        return Location(
            anchorLocation.world,
            anchorX - direction.normalX * FRAME_HANGING_CENTER_OFFSET,
            anchorY,
            anchorZ - direction.normalZ * FRAME_HANGING_CENTER_OFFSET
        )
    }

    private fun resolveFillArea(session: PaintSession, hit: PaintSurfacePixel, ignoreShade: Boolean): FillArea? {
        val (cache, componentId) = resolveFillComponent(session, hit, ignoreShade) ?: return null
        return resolveFillArea(cache, componentId)
    }

    private fun resolveFillComponent(
        session: PaintSession,
        hit: PaintSurfacePixel,
        ignoreShade: Boolean
    ): Pair<FillComponentCache, Int>? {
        val cache = resolveFillComponentCache(session, ignoreShade) ?: return null
        val startIndex = cache.indexOf(hit.globalX, hit.globalY) ?: return null
        if (startIndex !in cache.labels.indices) return null
        val componentId = cache.labels[startIndex]
        if (componentId <= 0) return null
        return cache to componentId
    }

    private fun resolveFillArea(cache: FillComponentCache, componentId: Int): FillArea? {
        if (componentId !in cache.componentSizes.indices) return null
        val componentSize = cache.componentSizes[componentId].takeIf { it > 0 } ?: return null
        if (componentId !in cache.componentStarts.indices) return null
        return FillArea(cache, componentId, cache.componentStarts[componentId], componentSize)
    }

    private fun resolveFillComponentCache(session: PaintSession, ignoreShade: Boolean): FillComponentCache? {
        fillComponentCaches[session.playerId]?.let { cache ->
            if (cache.revision == session.canvasRevision && cache.ignoreShade == ignoreShade) {
                return cache
            }
        }
        return buildFillComponentCache(session, ignoreShade)?.also { cache ->
            fillComponentCaches[session.playerId] = cache
        }
    }

    private fun buildFillComponentCache(session: PaintSession, ignoreShade: Boolean): FillComponentCache? {
        val bounds = PaintCanvasBounds.from(session.canvasCells.keys) ?: return null
        val minGlobalX = bounds.minX * MAP_WIDTH
        val minGlobalY = bounds.minY * MAP_HEIGHT
        val width = bounds.width * MAP_WIDTH
        val height = bounds.height * MAP_HEIGHT
        val totalPixels = width * height
        val labels = IntArray(totalPixels) { FILL_INVALID_LABEL }
        val colors = ByteArray(totalPixels)

        session.canvasCells.forEach { (point, cell) ->
            val snapshot = MapDataExtractor.extract(cell.mapId) ?: return@forEach
            val offsetX = point.x * MAP_WIDTH - minGlobalX
            val offsetY = point.y * MAP_HEIGHT - minGlobalY
            for (localY in 0 until MAP_HEIGHT) {
                val targetRowStart = (offsetY + localY) * width + offsetX
                val sourceRowStart = localY * MAP_WIDTH
                snapshot.colors.copyInto(
                    destination = colors,
                    destinationOffset = targetRowStart,
                    startIndex = sourceRowStart,
                    endIndex = sourceRowStart + MAP_WIDTH
                )
                for (localX in 0 until MAP_WIDTH) {
                    labels[targetRowStart + localX] = FILL_UNLABELED
                }
            }
        }

        val componentSizes = IntArray(totalPixels + 1)
        val componentStarts = IntArray(totalPixels + 2)
        val componentIndices = IntArray(totalPixels)
        val queue = IntArray(totalPixels)
        var componentCursor = 0
        var componentId = 0
        for (startIndex in 0 until totalPixels) {
            if (labels[startIndex] != FILL_UNLABELED) continue
            componentId += 1
            componentStarts[componentId] = componentCursor
            val startColor = colors[startIndex]
            var head = 0
            var tail = 0
            queue[tail++] = startIndex
            labels[startIndex] = componentId

            while (head < tail) {
                val currentIndex = queue[head++]
                val x = currentIndex % width
                tail = enqueueFillNeighbor(currentIndex - 1, x > 0, labels, colors, startColor, ignoreShade, componentId, queue, tail)
                tail = enqueueFillNeighbor(currentIndex + 1, x + 1 < width, labels, colors, startColor, ignoreShade, componentId, queue, tail)
                tail = enqueueFillNeighbor(currentIndex - width, currentIndex >= width, labels, colors, startColor, ignoreShade, componentId, queue, tail)
                tail = enqueueFillNeighbor(currentIndex + width, currentIndex + width < totalPixels, labels, colors, startColor, ignoreShade, componentId, queue, tail)
            }

            componentSizes[componentId] = tail
            queue.copyInto(componentIndices, destinationOffset = componentCursor, startIndex = 0, endIndex = tail)
            componentCursor += tail
        }

        return FillComponentCache(
            revision = session.canvasRevision,
            ignoreShade = ignoreShade,
            minGlobalX = minGlobalX,
            minGlobalY = minGlobalY,
            width = width,
            height = height,
            labels = labels,
            colors = colors,
            componentSizes = componentSizes.copyOf(componentId + 1),
            componentStarts = componentStarts.copyOf(componentId + 1),
            componentIndices = componentIndices.copyOf(componentCursor)
        )
    }

    private fun enqueueFillNeighbor(
        index: Int,
        isValidNeighbor: Boolean,
        labels: IntArray,
        colors: ByteArray,
        startColor: Byte,
        ignoreShade: Boolean,
        componentId: Int,
        queue: IntArray,
        tail: Int
    ): Int {
        if (!isValidNeighbor || labels[index] != FILL_UNLABELED) return tail
        if (!matchesFillColor(startColor, colors[index], ignoreShade)) return tail
        labels[index] = componentId
        queue[tail] = index
        return tail + 1
    }

    private fun matchesFillColor(expected: Byte, candidate: Byte, ignoreShade: Boolean): Boolean {
        if (!ignoreShade) {
            return expected == candidate
        }
        if (expected == MapColorMatcher.TRANSPARENT_COLOR_ID || candidate == MapColorMatcher.TRANSPARENT_COLOR_ID) {
            return expected == candidate
        }
        return packedColorFamily(expected) == packedColorFamily(candidate)
    }

    private fun packedColorFamily(color: Byte): Int = (color.toInt() and 0xFF) / 4

    private fun resolveMixedPaletteColors(
        palette: PaintPaletteEntry,
        baseShade: PaintShade,
        mixedShades: Set<PaintShade>
    ): ByteArray {
        if (palette.isTransparent) return byteArrayOf(MapColorMatcher.TRANSPARENT_COLOR_ID)
        val available = mixedShades.ifEmpty { setOf(baseShade) }
        val colors = ByteArray(available.size)
        var index = 0
        available.forEach { shade ->
            colors[index++] = palette.packed(shade)
        }
        return colors
    }

    private fun randomColor(colors: ByteArray): Byte {
        return if (colors.size == 1) colors[0] else colors[Random.nextInt(colors.size)]
    }

    private fun resolveShapePixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintShapeSettings
    ): List<PaintSurfacePixel> {
        if (settings.shapeType == PaintShapeType.LINE) {
            return emptyList()
        }
        val radius = max(1, settings.normalizedSize())
        val searchRadius = shapeSearchRadius(settings, radius)
        val results = linkedMapOf<Int, PaintSurfacePixel>()
        for (offsetY in -searchRadius..searchRadius) {
            for (offsetX in -searchRadius..searchRadius) {
                if (!shapeContains(settings, radius, offsetX, offsetY)) continue
                resolvePixelByGlobal(session, hit.globalX + offsetX, hit.globalY + offsetY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
        }
        return results.values.toList()
    }

    private fun shapeSearchRadius(settings: PaintShapeSettings, radius: Int): Int {
        if (settings.shapeType != PaintShapeType.STAR || settings.filled) {
            return radius
        }
        return radius + ceil(max(1.0, radius * STAR_OUTLINE_WIDTH_RATIO)).toInt()
    }

    private fun resolveLineShapePixels(
        session: PaintSession,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        settings: PaintShapeSettings
    ): List<PaintSurfacePixel> {
        val results = linkedMapOf<Int, PaintSurfacePixel>()
        val radius = max(0, settings.normalizedSize() - 1)
        if (radius == 0) {
            traceLine(startX, startY, endX, endY).forEach { (globalX, globalY) ->
                resolvePixelByGlobal(session, globalX, globalY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
            return results.values.toList()
        }

        val radiusSquared = radius.toDouble() * radius.toDouble()
        val innerRadius = if (radius <= 1) 0.5 else radius - 1.0
        val innerRadiusSquared = innerRadius * innerRadius
        for (globalY in minOf(startY, endY) - radius..maxOf(startY, endY) + radius) {
            for (globalX in minOf(startX, endX) - radius..maxOf(startX, endX) + radius) {
                val distanceSquared = squaredDistanceToSegment(globalX, globalY, startX, startY, endX, endY)
                val contains = if (settings.filled) {
                    distanceSquared <= radiusSquared
                } else {
                    distanceSquared <= radiusSquared && distanceSquared >= innerRadiusSquared
                }
                if (!contains) continue
                resolvePixelByGlobal(session, globalX, globalY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
        }
        return results.values.toList()
    }

    private fun squaredDistanceToSegment(
        pointX: Int,
        pointY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int
    ): Double {
        val segmentX = (endX - startX).toDouble()
        val segmentY = (endY - startY).toDouble()
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared == 0.0) {
            val dx = pointX - startX
            val dy = pointY - startY
            return (dx * dx + dy * dy).toDouble()
        }
        val projection = (((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared)
            .coerceIn(0.0, 1.0)
        val closestX = startX + projection * segmentX
        val closestY = startY + projection * segmentY
        val dx = pointX - closestX
        val dy = pointY - closestY
        return dx * dx + dy * dy
    }

    private fun shapeContains(
        settings: PaintShapeSettings,
        radius: Int,
        offsetX: Int,
        offsetY: Int
    ): Boolean {
        return when (settings.shapeType) {
            PaintShapeType.SQUARE -> {
                if (settings.filled) {
                    abs(offsetX) <= radius && abs(offsetY) <= radius
                } else {
                    abs(offsetX) == radius || abs(offsetY) == radius
                }
            }

            PaintShapeType.CIRCLE -> {
                val distance = sqrt((offsetX * offsetX + offsetY * offsetY).toDouble())
                if (settings.filled) {
                    distance <= radius
                } else {
                    distance in (radius - 0.9)..(radius + 0.9)
                }
            }

            PaintShapeType.LINE -> false

            PaintShapeType.TRIANGLE -> {
                val normalizedY = offsetY + radius
                if (normalizedY < 0 || normalizedY > radius * 2) {
                    false
                } else {
                    val width = normalizedY / 2.0
                    if (settings.filled) {
                        abs(offsetX) <= width
                    } else {
                        abs(abs(offsetX) - width) <= 0.8 || normalizedY == radius * 2
                    }
                }
            }

            PaintShapeType.STAR -> starContains(settings, radius, offsetX, offsetY)
        }
    }

    private fun starContains(
        settings: PaintShapeSettings,
        radius: Int,
        offsetX: Int,
        offsetY: Int
    ): Boolean {
        val vertices = regularStarVertices(radius.toDouble())
        val x = offsetX.toDouble()
        val y = offsetY.toDouble()
        if (settings.filled) {
            return pointInPolygon(x, y, vertices)
        }
        val outlineWidth = max(1.0, radius * STAR_OUTLINE_WIDTH_RATIO)
        return vertices.indices.any { index ->
            val start = vertices[index]
            val end = vertices[(index + 1) % vertices.size]
            distanceToSegment(x, y, start.first, start.second, end.first, end.second) <= outlineWidth
        }
    }

    private fun regularStarVertices(radius: Double): List<Pair<Double, Double>> {
        val innerRadius = radius * STAR_INNER_RADIUS_RATIO
        return List(STAR_VERTEX_COUNT) { index ->
            val angle = -Math.PI / 2.0 + index * Math.PI / STAR_POINTS
            val vertexRadius = if (index % 2 == 0) radius else innerRadius
            cos(angle) * vertexRadius to sin(angle) * vertexRadius
        }
    }

    private fun pointInPolygon(x: Double, y: Double, vertices: List<Pair<Double, Double>>): Boolean {
        var inside = false
        var previousIndex = vertices.lastIndex
        vertices.indices.forEach { index ->
            val current = vertices[index]
            val previous = vertices[previousIndex]
            val crossesY = (current.second > y) != (previous.second > y)
            if (crossesY) {
                val intersectionX = (previous.first - current.first) *
                    (y - current.second) /
                    (previous.second - current.second) +
                    current.first
                if (x < intersectionX) {
                    inside = !inside
                }
            }
            previousIndex = index
        }
        return inside
    }

    private fun distanceToSegment(
        pointX: Double,
        pointY: Double,
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double
    ): Double {
        val segmentX = endX - startX
        val segmentY = endY - startY
        val lengthSquared = segmentX * segmentX + segmentY * segmentY
        if (lengthSquared <= 0.0) {
            return sqrt((pointX - startX) * (pointX - startX) + (pointY - startY) * (pointY - startY))
        }
        val projection = (((pointX - startX) * segmentX + (pointY - startY) * segmentY) / lengthSquared).coerceIn(0.0, 1.0)
        val closestX = startX + projection * segmentX
        val closestY = startY + projection * segmentY
        return sqrt((pointX - closestX) * (pointX - closestX) + (pointY - closestY) * (pointY - closestY))
    }

    private fun updateResizePreview(player: Player, session: PaintSession) {
        val target = resolveResizeTarget(player, session)
        if (target == null) {
            removeResizePreview(player, session)
            return
        }

        val canAdd = target.state == ResizeTargetState.ADD
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

    private fun resolveResizeTarget(player: Player, session: PaintSession): ResizeTarget? {
        val hit = resolveHitPixel(player, session, allowMissingCell = true) ?: return null
        val point = hit.cellPoint
        if (!isWithinResizePreviewRange(session, point)) {
            return null
        }

        val location = resolveCellLocation(session.anchorLocation, session.anchorPoint, point, session.frameDirection)

        if (point in session.canvasCells) {
            return ResizeTarget(point, location, ResizeTargetState.REMOVE_OWN)
        }

        if (!isEmptyFrameSpace(location)) {
            return ResizeTarget(point, location, ResizeTargetState.INVALID)
        }

        if (!isAdjacentToCanvas(session, point)) {
            return ResizeTarget(point, location, ResizeTargetState.INVALID)
        }

        val bounds = PaintCanvasBounds.from(session.canvasCells.keys + point) ?: return ResizeTarget(point, location, ResizeTargetState.INVALID)
        if (bounds.width > 4 || bounds.height > 4) {
            return ResizeTarget(point, location, ResizeTargetState.INVALID)
        }

        return ResizeTarget(point, location, ResizeTargetState.ADD)
    }

    private fun handleResizeActivation(player: Player, session: PaintSession) {
        val target = resolveResizeTarget(player, session)
        if (target == null) {
            menuController.openEaselMenu(player, session)
            return
        }

        when (target.state) {
            ResizeTargetState.ADD -> {
                if (!addCanvasCell(player, session, target.point, target.location)) {
                    menuController.openEaselMenu(player, session)
                    return
                }
                clearStrokeState(session)
                clearHistory(session)
                session.resizeMode = false
                removeResizePreview(player, session)
                menuController.openEaselMenu(player, session)
            }

            ResizeTargetState.REMOVE_OWN -> {
                if (session.canvasCells.size <= 1) {
                    menuController.openEaselMenu(player, session)
                    return
                }
                removeCanvasCell(session, target.point)
                clearStrokeState(session)
                clearHistory(session)
                session.resizeMode = false
                removeResizePreview(player, session)
                menuController.openEaselMenu(player, session)
            }

            ResizeTargetState.INVALID -> menuController.openEaselMenu(player, session)
        }
    }

    private fun addCanvasCell(player: Player, session: PaintSession, point: PaintGridPoint, location: Location): Boolean {
        val snapshot = MapDataExtractor.create(player.world) ?: return false
        val frame = createFrame(player, snapshot.mapId, location, session.frameDirection)
        if (!frame.spawn(PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch))) {
            return false
        }
        session.canvasCells[point] = PaintCanvasCell(point, snapshot.mapId, frame, location)
        session.viewers.forEach(frame::addViewer)
        markCanvasTopologyChanged(session)
        sendFullMapDataToViewers(session, snapshot)
        scheduleDelayedMapDataRefresh(session.playerId, snapshot.mapId, session.viewers.toSet())
        return true
    }

    private fun removeCanvasCell(session: PaintSession, point: PaintGridPoint) {
        session.canvasCells.remove(point)?.let { cell ->
            cell.frame.remove()
            markCanvasTopologyChanged(session)
        }
    }

    private fun isAdjacentToCanvas(session: PaintSession, point: PaintGridPoint): Boolean {
        return adjacentPoints(point).any { it in session.canvasCells }
    }

    private fun isWithinResizePreviewRange(session: PaintSession, point: PaintGridPoint): Boolean {
        val bounds = PaintCanvasBounds.from(session.canvasCells.keys) ?: return false
        return point.x in (bounds.minX - MAX_RESIZE_PREVIEW_DISTANCE_CELLS)..(bounds.maxX + MAX_RESIZE_PREVIEW_DISTANCE_CELLS) &&
            point.y in (bounds.minY - MAX_RESIZE_PREVIEW_DISTANCE_CELLS)..(bounds.maxY + MAX_RESIZE_PREVIEW_DISTANCE_CELLS)
    }

    private fun adjacentPoints(point: PaintGridPoint): List<PaintGridPoint> = listOf(
        PaintGridPoint(point.x + 1, point.y),
        PaintGridPoint(point.x - 1, point.y),
        PaintGridPoint(point.x, point.y + 1),
        PaintGridPoint(point.x, point.y - 1)
    )

    private fun isEmptyFrameSpace(location: Location): Boolean {
        if (!location.block.type.isAir) return false
        return sessions.values
            .flatMap { it.canvasCells.values }
            .none { other ->
                other.location.world == location.world &&
                    other.location.distanceSquared(location) < LOCATION_EPSILON
            }
    }

    private fun createResizePreview(
        player: Player,
        session: PaintSession,
        location: Location,
        canAdd: Boolean
    ): PaintResizePreview {
        val frame = WrapperEntity(EntityTypes.GLOW_ITEM_FRAME)
        frame.addViewer(player.uniqueId)
        val meta = frame.entityMeta as ItemFrameMeta
        meta.orientation = session.frameDirection.orientation
        val transparentMap = resolveResizePreviewMap(player, session)
        transparentMap?.let { snapshot ->
            meta.item = createMapItem(snapshot.mapId)
        }
        meta.isGlowing = true
        frame.spawn(PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch))
        transparentMap?.let { snapshot ->
            sendFullMapData(player, snapshot)
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

    private fun removeResizePreview(player: Player, session: PaintSession) {
        val preview = session.resizePreview ?: return
        sendResizeTeamRemove(player, preview.teamName)
        preview.frame.remove()
        session.resizePreview = null
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

    private fun clearCanvas(player: Player, session: PaintSession) {
        restorePreviewIfNeeded(player, session)
        var changed = false
        session.canvasCells.values.forEach { cell ->
            MapDataExtractor.fill(cell.mapId, BACKGROUND_COLOR_ID)?.let { snapshot ->
                changed = true
                sendFullMapDataToViewers(session, snapshot)
            }
        }
        if (changed) {
            markCanvasChanged(session)
        }
        clearStrokeState(session)
        clearHistory(session)
    }

    private fun scheduleDelayedMapDataRefresh(playerId: UUID, mapId: Int, viewerIds: Set<UUID>) {
        hooker.tickScheduler.runLater(1L) {
            val session = sessions[playerId] ?: return@runLater
            if (session.canvasCells.values.none { it.mapId == mapId }) return@runLater
            val snapshot = MapDataExtractor.extract(mapId) ?: return@runLater
            viewerIds.forEach { viewerId ->
                Bukkit.getPlayer(viewerId)?.let { viewer ->
                    sendFullMapData(viewer, snapshot)
                }
            }
        }
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

    private fun sendMapPatchData(player: Player, patch: MapDataExtractor.Patch) {
        val packet = WrapperPlayServerMapData(
            patch.mapId,
            patch.scale,
            false,
            patch.locked,
            null,
            patch.width,
            patch.height,
            patch.startX,
            patch.startY,
            patch.colors
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun sendFullMapDataToViewers(session: PaintSession, snapshot: MapDataExtractor.Snapshot) {
        session.viewers.forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                sendFullMapData(viewer, snapshot)
            }
        }
    }

    private fun sendMapPatchDataToViewers(session: PaintSession, patch: MapDataExtractor.Patch) {
        session.viewers.forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                sendMapPatchData(viewer, patch)
            }
        }
    }

    private fun resolveTool(item: BukkitItemStack?): PaintToolDefinition? = toolInventoryService.resolve(item)

    private fun isLineShapeTool(tool: PaintToolDefinition, session: PaintSession): Boolean {
        return tool.mode == PaintToolMode.SHAPE && session.toolSettings.shape.shapeType == PaintShapeType.LINE
    }

    private fun resolveDirection(player: Player): PaintFrameDirection {
        val normalizedYaw = ((player.location.yaw % 360f) + 360f) % 360f
        return when {
            normalizedYaw >= 45f && normalizedYaw < 135f -> PaintFrameDirection.WEST
            normalizedYaw >= 135f && normalizedYaw < 225f -> PaintFrameDirection.NORTH
            normalizedYaw >= 225f && normalizedYaw < 315f -> PaintFrameDirection.EAST
            else -> PaintFrameDirection.SOUTH
        }
    }

    private fun isWorkTool(item: BukkitItemStack?): Boolean = toolInventoryService.isWorkTool(item)

    private fun isDropClick(click: ClickType): Boolean {
        return click == ClickType.DROP || click == ClickType.CONTROL_DROP
    }

    private fun floorDiv(value: Int, divisor: Int): Int = Math.floorDiv(value, divisor)

    private fun floorMod(value: Int, divisor: Int): Int = Math.floorMod(value, divisor)

    private fun packGridPoint(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xFFFFFFFFL)

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
        private const val DIRECT_USE_DEBOUNCE_MILLIS = 65L
        private const val DIRECT_USE_SUPPRESS_AFTER_DROP_MILLIS = 250L
        private const val DROP_ACTION_DEBOUNCE_MILLIS = 65L
        private const val PALETTE_ROTATION_DEBOUNCE_MILLIS = 100L
        private const val STROKE_CONTINUE_WINDOW_MILLIS = 250L
        private const val PLANE_EPSILON = 1.0E-6
        private const val ROW_INTERVAL_EPSILON = 1.0E-9
        private const val MIN_CANVAS_FACING_DOT = -0.1
        private const val PREVIEW_ALPHA = 0.42
        private const val LARGE_BRUSH_PREVIEW_SUPPRESS_SIZE = 35
        private const val LARGE_BRUSH_PREVIEW_SUPPRESS_MILLIS = 140L
        private const val FILL_COOLDOWN_MILLIS = 100L
        private const val LOCATION_EPSILON = 0.01
        private const val MAX_RESIZE_PREVIEW_DISTANCE_CELLS = 3
        private const val HISTORY_PIXEL_ESTIMATE_BYTES = 40L
        private const val HISTORY_MAP_GROUP_BASE_BYTES = 96L
        private const val HISTORY_ENTRY_BASE_BYTES = 64L
        private const val HISTORY_LINE_ANCHOR_ESTIMATE_BYTES = 32L
        private const val MAX_HISTORY_BYTES = 32L * 1024L * 1024L
        private const val GLOBAL_CANVAS_HASH_BASE = 100_000
        private const val FILL_INVALID_LABEL = 0
        private const val FILL_UNLABELED = -1
        private const val STAR_POINTS = 5
        private const val STAR_VERTEX_COUNT = STAR_POINTS * 2
        private const val STAR_OUTLINE_WIDTH_RATIO = 0.12
        private val RESIZE_PREVIEW_CAN_ADD_COLOR = ChatFormatting.GREEN
        private val RESIZE_PREVIEW_CANNOT_ADD_COLOR = ChatFormatting.RED
        private val STAR_INNER_RADIUS_RATIO = sin(Math.PI / 10.0) / sin(3.0 * Math.PI / 10.0)
        private val BACKGROUND_COLOR_ID: Byte = MapDataExtractor.DEFAULT_CANVAS_COLOR_ID
    }
}
