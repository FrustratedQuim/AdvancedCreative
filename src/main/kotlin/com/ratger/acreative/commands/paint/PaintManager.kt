package com.ratger.acreative.commands.paint

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
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.edit.experimental.ComponentsService
import com.ratger.acreative.menus.edit.map.MapItemSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.utils.PlayerInventoryTransferSupport
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
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.ShulkerBox
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.util.Vector
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.CloseEvent
import java.awt.Color
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
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
        val colorsByIndex: Map<Int, Byte>
    )

    private val sessions = mutableMapOf<UUID, PaintSession>()
    private val toolKey = NamespacedKey(hooker.plugin, "paint_tool_id")
    private val parser = MiniMessageParser()
    private val buttonFactory = MenuButtonFactory(parser, ComponentsService(), hooker.tickScheduler)
    private val signInputService = SignInputService(hooker.plugin)
    private val menuTransitions = mutableSetOf<UUID>()
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
            openEaselMenu(player, session)
            return true
        }
        if (stackDrop) {
            undoLastAction(player, session)
            return true
        }
        if (!isWorkTool(player.inventory.itemInMainHand)) {
            return true
        } else {
            openSettingsForCurrentTool(player, session)
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
        if (player.uniqueId in menuTransitions) return
        session.isMenuOpen = false
        session.openMenuKind = null
        session.activeColorMenuReturnTo = null
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
        hooker.tickScheduler.cancel(session.viewerTaskId)
        hooker.tickScheduler.cancel(session.previewTaskId)
        restorePreviewIfNeeded(player, session)
        removeResizePreview(player, session)
        session.canvasCells.values.forEach { it.frame.remove() }
        clearPaintInventory(player)
        session.inventorySnapshot.restore(player)
        giveResult(player, session)
        hooker.playerStateManager.deactivateState(player, PlayerStateType.PAINTING)
    }

    fun releaseAll() {
        sessions.keys.toList().forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let(::stopPainting) ?: run {
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
        preparePaintInventory(player, session)

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
            clearStrokeState(session)
            session.paletteRotation = (session.paletteRotation + 1) % 4
            refreshToolInventory(player, session)
            return true
        }

        val tool = resolveTool(player.inventory.itemInMainHand) ?: return true
        val hit = resolveHitPixel(player, session, allowMissingCell = false) ?: return true
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
                    settings = session.toolSettings.basicBrush,
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

            PaintToolMode.EASEL -> openEaselMenu(player, session)
        }
    }

    private fun applyBrushLikeTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBrushSettings,
        paletteKey: String
    ) {
        val path = resolveBrushStrokePath(session, hit, settings, paletteKey)
        if (path.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        path.forEach { pixel ->
            val oldColor = resolveActualColor(session, pixel.cellPoint, pixel.localX, pixel.localY) ?: return@forEach
            if (Random.nextInt(100) >= settings.normalizedFillPercent()) return@forEach
            val newColor = resolveMixedPaletteColor(PaintPalette.entry(paletteKey), settings.shade, settings.normalizedShadeMix())
            if (oldColor == newColor) return@forEach
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(pixel.localX, pixel.localY, oldColor, newColor)
            )
        }

        rememberStroke(session, hit, PaintPalette.entry(paletteKey).packed(settings.shade))
        applyHistoryChanges(session, changes)
    }

    private fun applyBinaryBrushTool(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBinaryBrushSettings,
        color: Byte
    ) {
        val path = resolveBrushStrokePath(session, hit, settings.normalizedSize())
        if (path.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        path.forEach { pixel ->
            val oldColor = resolveActualColor(session, pixel.cellPoint, pixel.localX, pixel.localY) ?: return@forEach
            if (Random.nextInt(100) >= settings.normalizedFillPercent()) return@forEach
            if (oldColor == color) return@forEach
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(pixel.localX, pixel.localY, oldColor, color)
            )
        }

        rememberStroke(session, hit, color)
        applyHistoryChanges(session, changes)
    }

    private fun applyFillTool(session: PaintSession, hit: PaintSurfacePixel) {
        if (session.currentTick < session.fillCooldownUntilTick) return
        session.fillCooldownUntilTick = session.currentTick + FILL_COOLDOWN_TICKS

        val settings = session.toolSettings.fill
        val area = resolveFloodFillArea(session, hit, settings.ignoreShade)
        if (area.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        val entry = PaintPalette.entry(settings.paletteKey)
        area.forEach { pixel ->
            val oldColor = resolveActualColor(session, pixel.cellPoint, pixel.localX, pixel.localY) ?: return@forEach
            if (Random.nextInt(100) >= settings.normalizedFillPercent()) return@forEach
            val newColor = resolveMixedPaletteColor(entry, settings.baseShade, settings.normalizedShadeMix())
            if (oldColor == newColor) return@forEach
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(pixel.localX, pixel.localY, oldColor, newColor)
            )
        }

        clearStrokeState(session)
        applyHistoryChanges(session, changes)
    }

    private fun applyShapeTool(session: PaintSession, hit: PaintSurfacePixel) {
        val settings = session.toolSettings.shape
        val entry = PaintPalette.entry(settings.paletteKey)
        val pixels = resolveShapePixels(session, hit, settings)
        if (pixels.isEmpty()) return

        val changes = mutableMapOf<Int, MutableList<PaintPixelChange>>()
        pixels.forEach { pixel ->
            val oldColor = resolveActualColor(session, pixel.cellPoint, pixel.localX, pixel.localY) ?: return@forEach
            if (Random.nextInt(100) >= settings.normalizedFillPercent()) return@forEach
            val newColor = resolveMixedPaletteColor(entry, settings.shade, settings.normalizedShadeMix())
            if (oldColor == newColor) return@forEach
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            changes.getOrPut(mapId) { mutableListOf() }.add(
                PaintPixelChange(pixel.localX, pixel.localY, oldColor, newColor)
            )
        }

        clearStrokeState(session)
        applyHistoryChanges(session, changes)
    }

    private fun applyHistoryChanges(session: PaintSession, changesByMapId: Map<Int, List<PaintPixelChange>>) {
        if (changesByMapId.isEmpty()) return
        session.previewPaused = false
        clearPreviewSuppression(session)
        Bukkit.getPlayer(session.playerId)?.let { restorePreviewIfNeeded(it, session) }

        val deduplicated = changesByMapId.mapValues { (_, changes) ->
            changes
                .associateBy { it.y * MAP_WIDTH + it.x }
                .values
                .toList()
        }.filterValues { it.isNotEmpty() }
        if (deduplicated.isEmpty()) return

        val snapshots = mutableListOf<MapDataExtractor.Snapshot>()
        deduplicated.forEach { (mapId, changes) ->
            val snapshot = MapDataExtractor.setPixels(
                mapId,
                changes.map { Triple(it.x, it.y, it.newColor) }
            ) ?: return@forEach
            snapshots += snapshot
        }
        if (snapshots.isEmpty()) return

        val historyEntry = PaintHistoryEntry(
            changesByMapId = deduplicated,
            estimatedBytes = estimateHistoryEntryBytes(deduplicated)
        )
        session.history += historyEntry
        session.historyBytes += historyEntry.estimatedBytes
        while (session.historyBytes > MAX_HISTORY_BYTES && session.history.isNotEmpty()) {
            val removed = session.history.removeFirst()
            session.historyBytes = (session.historyBytes - removed.estimatedBytes).coerceAtLeast(0L)
        }

        snapshots.forEach { snapshot ->
            sendFullMapDataToViewers(session, snapshot)
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
        entry.changesByMapId.forEach { (mapId, changes) ->
            val snapshot = MapDataExtractor.setPixels(
                mapId,
                changes.map { Triple(it.x, it.y, it.oldColor) }
            ) ?: return@forEach
            sendFullMapDataToViewers(session, snapshot)
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

    private fun preparePaintInventory(player: Player, session: PaintSession) {
        clearPaintInventory(player)
        player.inventory.storageContents = rotatedLayout(PaintToolCatalog.buildLayout(toolKey, parser, session), session.paletteRotation)
        player.inventory.heldItemSlot = 0
    }

    private fun refreshToolInventory(player: Player, session: PaintSession) {
        val heldSlot = player.inventory.heldItemSlot
        player.inventory.storageContents = rotatedLayout(PaintToolCatalog.buildLayout(toolKey, parser, session), session.paletteRotation)
        player.inventory.heldItemSlot = heldSlot.coerceIn(0, 8)
    }

    private fun rotatedLayout(contents: Array<BukkitItemStack?>, rotation: Int): Array<BukkitItemStack?> {
        val result = contents.copyOf()
        repeat(rotation.coerceIn(0, 3)) {
            val hotbar = result.sliceArray(0..8)
            val second = result.sliceArray(9..17)
            val third = result.sliceArray(18..26)
            val fourth = result.sliceArray(27..35)
            fourth.copyInto(result, 0)
            hotbar.copyInto(result, 9)
            second.copyInto(result, 18)
            third.copyInto(result, 27)
        }
        return result
    }

    private fun clearPaintInventory(player: Player) {
        player.inventory.storageContents = arrayOfNulls(36)
        player.inventory.armorContents = arrayOf(null, null, null, null)
        player.inventory.extraContents = arrayOfNulls(player.inventory.extraContents.size.coerceAtLeast(1))
    }

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
            restorePreviewIfNeeded(player, session)
            return
        }
        if (tool.mode == PaintToolMode.EASEL) {
            clearPreviewSuppression(session)
            clearStrokeState(session)
            restorePreviewIfNeeded(player, session)
            return
        }

        val hit = resolveHitPixel(player, session, allowMissingCell = false) ?: run {
            clearPreviewSuppression(session)
            clearStrokeState(session)
            restorePreviewIfNeeded(player, session)
            return
        }

        val overlays = resolvePreviewOverlays(session, tool, hit)
        val fingerprint = overlays.joinToString("|") { overlay ->
            "${overlay.mapId}:${overlay.colorsByIndex.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value.toInt() and 0xFF}" }}"
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
            val snapshot = MapDataExtractor.extract(overlay.mapId) ?: return@forEach
            val colors = snapshot.colors.copyOf()
            overlay.colorsByIndex.forEach { (index, color) ->
                if (index in colors.indices) {
                    colors[index] = color
                }
            }
            sendPreviewMapData(player, snapshot, colors)
        }
        session.previewMapIds = overlays.mapTo(mutableSetOf()) { it.mapId }
        session.previewFingerprint = fingerprint
    }

    private fun resolvePreviewOverlays(
        session: PaintSession,
        tool: PaintToolDefinition,
        hit: PaintSurfacePixel
    ): List<PreviewMapOverlay> {
        val (pixels, previewColor) = when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> {
                val palette = PaintPalette.entry(requireNotNull(tool.fixedPaletteKey))
                resolveBrushStrokePath(session, hit, session.toolSettings.basicBrush.normalizedSize()) to
                    palette.packed(session.toolSettings.basicBrush.shade)
            }

            PaintToolMode.CUSTOM_BRUSH ->
                resolveBrushStrokePath(session, hit, session.toolSettings.customBrush.normalizedSize()) to
                    PaintPalette.entry(session.toolSettings.customBrush.paletteKey).packed(session.toolSettings.customBrush.shade)

            PaintToolMode.ERASER ->
                resolveBrushStrokePath(session, hit, session.toolSettings.eraser.normalizedSize()) to BACKGROUND_COLOR_ID

            PaintToolMode.SHEARS ->
                resolveBrushStrokePath(session, hit, session.toolSettings.shears.normalizedSize()) to MapColorMatcher.TRANSPARENT_COLOR_ID

            PaintToolMode.FILL ->
                resolveFloodFillArea(session, hit, session.toolSettings.fill.ignoreShade) to
                    PaintPalette.entry(session.toolSettings.fill.paletteKey).packed(session.toolSettings.fill.baseShade)

            PaintToolMode.SHAPE ->
                resolveShapePixels(session, hit, session.toolSettings.shape) to
                    PaintPalette.entry(session.toolSettings.shape.paletteKey).packed(session.toolSettings.shape.shade)

            PaintToolMode.EASEL -> return emptyList()
        }
        if (pixels.isEmpty()) return emptyList()

        val grouped = mutableMapOf<Int, MutableMap<Int, Byte>>()
        pixels.forEach { pixel ->
            val mapId = session.canvasCells[pixel.cellPoint]?.mapId ?: return@forEach
            val oldColor = resolveActualColor(session, pixel.cellPoint, pixel.localX, pixel.localY) ?: return@forEach
            val blended = blendPreviewColor(oldColor, previewColor)
            grouped.getOrPut(mapId) { mutableMapOf() }[pixel.localY * MAP_WIDTH + pixel.localX] = blended
        }

        return grouped.map { (mapId, colorsByIndex) -> PreviewMapOverlay(mapId, colorsByIndex) }
    }

    private fun restorePreviewIfNeeded(player: Player, session: PaintSession) {
        if (session.previewMapIds.isEmpty()) return
        session.previewMapIds.forEach { mapId ->
            MapDataExtractor.extract(mapId)?.let { snapshot ->
                sendFullMapData(player, snapshot)
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
    }

    private fun clearPreviewSuppression(session: PaintSession) {
        session.previewSuppressionKey = null
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
        val currentSlot = player.inventory.heldItemSlot
        val currentItem = player.inventory.itemInMainHand
        val itemForSync = if (isWorkTool(currentItem)) {
            currentItem.clone()
        } else {
            rotatedLayout(PaintToolCatalog.buildLayout(toolKey, parser, session), session.paletteRotation)
                .getOrNull(currentSlot)
                ?.clone()
                ?: BukkitItemStack(Material.AIR)
        }
        player.inventory.setItem(currentSlot, itemForSync)
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

    private fun estimateHistoryEntryBytes(changesByMapId: Map<Int, List<PaintPixelChange>>): Long {
        return HISTORY_ENTRY_BASE_BYTES + changesByMapId.entries.sumOf { (_, changes) ->
            HISTORY_MAP_GROUP_BASE_BYTES + changes.size * HISTORY_PIXEL_ESTIMATE_BYTES
        }
    }

    private fun resolveActualColor(session: PaintSession, point: PaintGridPoint, x: Int, y: Int): Byte? {
        val mapId = session.canvasCells[point]?.mapId ?: return null
        return MapDataExtractor.colorAt(mapId, x, y)
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
        if (brushColorId == MapColorMatcher.TRANSPARENT_COLOR_ID) {
            return brushColorId
        }
        val base = runCatching { MapDataExtractor.resolvePaletteColor(baseColorId) }.getOrNull() ?: return brushColorId
        val brush = runCatching { MapDataExtractor.resolvePaletteColor(brushColorId) }.getOrNull() ?: return brushColorId
        if (base.rgb == brush.rgb) return baseColorId

        val alpha = PREVIEW_ALPHA
        val red = (base.red * (1.0 - alpha) + brush.red * alpha).toInt().coerceIn(0, 255)
        val green = (base.green * (1.0 - alpha) + brush.green * alpha).toInt().coerceIn(0, 255)
        val blue = (base.blue * (1.0 - alpha) + brush.blue * alpha).toInt().coerceIn(0, 255)
        return MapColorMatcher.match(Color(red, green, blue))
    }

    private fun resolveBrushStrokePath(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintBrushSettings,
        paletteKey: String
    ): List<PaintSurfacePixel> {
        val color = PaintPalette.entry(paletteKey).packed(settings.shade)
        val points = resolveBrushStrokePath(session, hit, settings.normalizedSize())
        if (points.isEmpty()) return emptyList()
        session.lastStrokeColor = color
        return points
    }

    private fun resolveBrushStrokePath(
        session: PaintSession,
        hit: PaintSurfacePixel,
        size: Int
    ): List<PaintSurfacePixel> {
        val now = System.currentTimeMillis()
        val previousX = session.lastStrokeGlobalX
        val previousY = session.lastStrokeGlobalY
        val canContinueStroke =
            previousX != null &&
                previousY != null &&
                now - session.lastStrokeAtMillis <= STROKE_CONTINUE_WINDOW_MILLIS

        val centers = if (!canContinueStroke) {
            listOf(hit.globalX to hit.globalY)
        } else {
            traceLine(previousX, previousY, hit.globalX, hit.globalY)
        }

        val points = linkedMapOf<Int, PaintSurfacePixel>()
        centers.forEach { (globalX, globalY) ->
            resolveBrushPixelsAround(session, globalX, globalY, size).forEach { pixel ->
                points.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
            }
        }
        return points.values.toList()
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

    private fun resolveBrushPixelsAround(
        session: PaintSession,
        centerGlobalX: Int,
        centerGlobalY: Int,
        size: Int
    ): List<PaintSurfacePixel> {
        val radius = max(0, size - 1)
        val points = mutableListOf<PaintSurfacePixel>()
        for (offsetY in -radius..radius) {
            for (offsetX in -radius..radius) {
                if (offsetX * offsetX + offsetY * offsetY > radius * radius) continue
                resolvePixelByGlobal(session, centerGlobalX + offsetX, centerGlobalY + offsetY)?.let(points::add)
            }
        }
        return points
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
        allowMissingCell: Boolean
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
        val horizontal = relative.x * session.frameDirection.rightAxisX + relative.z * session.frameDirection.rightAxisZ
        val vertical = relative.y

        val cellX = floor(horizontal + session.anchorPoint.x + 0.5).toInt()
        val cellY = session.anchorPoint.y - floor(vertical + 0.5).toInt()
        val point = PaintGridPoint(cellX, cellY)
        if (!allowMissingCell && point !in session.canvasCells) return null

        val localHorizontal = horizontal - (cellX - session.anchorPoint.x)
        val localVertical = vertical - (session.anchorPoint.y - cellY)
        val half = FRAME_RENDER_SIZE / 2.0
        if (localHorizontal < -half || localHorizontal > half || localVertical < -half || localVertical > half) {
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

    private fun resolveFloodFillArea(
        session: PaintSession,
        hit: PaintSurfacePixel,
        ignoreShade: Boolean
    ): List<PaintSurfacePixel> {
        val startColor = resolveActualColor(session, hit.cellPoint, hit.localX, hit.localY) ?: return emptyList()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val visited = hashSetOf<Pair<Int, Int>>()
        val result = mutableListOf<PaintSurfacePixel>()

        queue += hit.globalX to hit.globalY
        while (queue.isNotEmpty()) {
            val (globalX, globalY) = queue.removeFirst()
            if (!visited.add(globalX to globalY)) continue
            val pixel = resolvePixelByGlobal(session, globalX, globalY) ?: continue
            val currentColor = resolveActualColor(session, pixel.cellPoint, pixel.localX, pixel.localY) ?: continue
            if (!matchesFillColor(startColor, currentColor, ignoreShade)) continue
            result += pixel
            queue += (globalX + 1) to globalY
            queue += (globalX - 1) to globalY
            queue += globalX to (globalY + 1)
            queue += globalX to (globalY - 1)
        }

        return result
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

    private fun resolveMixedPaletteColor(
        palette: PaintPaletteEntry,
        baseShade: PaintShade,
        mixedShades: Set<PaintShade>
    ): Byte {
        if (palette.isTransparent) return MapColorMatcher.TRANSPARENT_COLOR_ID
        val available = mixedShades.ifEmpty { setOf(baseShade) }.toList()
        return palette.packed(available.random())
    }

    private fun resolveShapePixels(
        session: PaintSession,
        hit: PaintSurfacePixel,
        settings: PaintShapeSettings
    ): List<PaintSurfacePixel> {
        val radius = max(1, settings.normalizedSize())
        val results = linkedMapOf<Int, PaintSurfacePixel>()
        for (offsetY in -radius..radius) {
            for (offsetX in -radius..radius) {
                if (!shapeContains(settings, radius, offsetX, offsetY)) continue
                resolvePixelByGlobal(session, hit.globalX + offsetX, hit.globalY + offsetY)?.let { pixel ->
                    results.putIfAbsent(pixel.globalY * GLOBAL_CANVAS_HASH_BASE + pixel.globalX, pixel)
                }
            }
        }
        return results.values.toList()
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

            PaintShapeType.LINE -> abs(offsetY) <= (if (settings.filled) 1 else 0) && abs(offsetX) <= radius

            PaintShapeType.TRIANGLE -> {
                val normalizedY = offsetY + radius
                if (normalizedY < 0 || normalizedY > radius * 2) {
                    false
                } else {
                    val width = radius - normalizedY / 2.0
                    if (settings.filled) {
                        abs(offsetX) <= width
                    } else {
                        abs(abs(offsetX) - width) <= 0.8 || normalizedY == radius * 2
                    }
                }
            }

            PaintShapeType.STAR -> {
                val angle = atan2(offsetY.toDouble(), offsetX.toDouble())
                val distance = sqrt((offsetX * offsetX + offsetY * offsetY).toDouble())
                val spikes = cos(angle * 5.0).coerceAtLeast(0.0)
                val outlineRadius = radius * (0.45 + spikes * 0.55)
                if (settings.filled) {
                    distance <= outlineRadius
                } else {
                    distance in (outlineRadius - 1.0)..(outlineRadius + 1.0)
                }
            }
        }
    }

    private fun updateResizePreview(player: Player, session: PaintSession) {
        val target = resolveResizeTarget(player, session)
        if (target == null) {
            removeResizePreview(player, session)
            return
        }

        val glowingGreen = target.state == ResizeTargetState.ADD
        val preview = session.resizePreview ?: createResizePreview(player, session, target.location, glowingGreen).also {
            session.resizePreview = it
        }

        preview.targetPoint = target.point
        if (preview.glowingGreen != glowingGreen) {
            sendResizeTeamRemove(player, preview.teamName)
            sendResizeTeamAdd(player, preview.teamName, preview.frame.uuid.toString(), glowingGreen)
            preview.glowingGreen = glowingGreen
        }
        preview.frame.teleport(PacketLocation(target.location.x, target.location.y, target.location.z, target.location.yaw, target.location.pitch))
    }

    private fun resolveResizeTarget(player: Player, session: PaintSession): ResizeTarget? {
        val hit = resolveHitPixel(player, session, allowMissingCell = true) ?: return null
        val location = resolveCellLocation(session.anchorLocation, session.anchorPoint, hit.cellPoint, session.frameDirection)
        val point = hit.cellPoint

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
            openEaselMenu(player, session)
            return
        }

        when (target.state) {
            ResizeTargetState.ADD -> {
                if (!addCanvasCell(player, session, target.point, target.location)) {
                    openEaselMenu(player, session)
                    return
                }
                clearStrokeState(session)
                clearHistory(session)
                session.resizeMode = false
                removeResizePreview(player, session)
            }

            ResizeTargetState.REMOVE_OWN -> {
                if (session.canvasCells.size <= 1) {
                    openEaselMenu(player, session)
                    return
                }
                removeCanvasCell(session, target.point)
                clearStrokeState(session)
                clearHistory(session)
                session.resizeMode = false
                removeResizePreview(player, session)
            }

            ResizeTargetState.INVALID -> openEaselMenu(player, session)
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
        sendFullMapDataToViewers(session, snapshot)
        scheduleDelayedMapDataRefresh(session.playerId, snapshot.mapId, session.viewers.toSet())
        return true
    }

    private fun removeCanvasCell(session: PaintSession, point: PaintGridPoint) {
        session.canvasCells.remove(point)?.frame?.remove()
    }

    private fun isAdjacentToCanvas(session: PaintSession, point: PaintGridPoint): Boolean {
        return adjacentPoints(point).any { it in session.canvasCells }
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
        glowingGreen: Boolean
    ): PaintResizePreview {
        val frame = WrapperEntity(EntityTypes.ITEM_FRAME)
        frame.addViewer(player.uniqueId)
        val meta = frame.entityMeta as ItemFrameMeta
        meta.orientation = session.frameDirection.orientation
        meta.isInvisible = true
        meta.isGlowing = true
        frame.spawn(PacketLocation(location.x, location.y, location.z, location.yaw, location.pitch))

        val preview = PaintResizePreview(
            frame = frame,
            teamName = "paint_${player.uniqueId.toString().take(8)}",
            glowingGreen = glowingGreen
        )
        sendResizeTeamAdd(player, preview.teamName, frame.uuid.toString(), glowingGreen)
        return preview
    }

    private fun removeResizePreview(player: Player, session: PaintSession) {
        val preview = session.resizePreview ?: return
        sendResizeTeamRemove(player, preview.teamName)
        preview.frame.remove()
        session.resizePreview = null
    }

    private fun sendResizeTeamAdd(player: Player, teamName: String, entry: String, green: Boolean) {
        val scoreboard = Scoreboard()
        val team = PlayerTeam(scoreboard, teamName)
        team.color = if (green) ChatFormatting.GREEN else ChatFormatting.RED
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

    private fun openSettingsForCurrentTool(player: Player, session: PaintSession) {
        val tool = resolveTool(player.inventory.itemInMainHand) ?: return
        when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> openBasicBrushMenu(player, session)
            PaintToolMode.CUSTOM_BRUSH -> openCustomBrushMenu(player, session)
            PaintToolMode.ERASER -> openBinaryBrushMenu(player, session, PaintMenuKind.ERASER, "Настройка кисти", session.toolSettings.eraser)
            PaintToolMode.SHEARS -> openBinaryBrushMenu(player, session, PaintMenuKind.ERASER, "Настройка кисти", session.toolSettings.shears, shears = true)
            PaintToolMode.FILL -> openFillMenu(player, session)
            PaintToolMode.SHAPE -> openShapeMenu(player, session)
            PaintToolMode.EASEL -> openEaselMenu(player, session)
        }
    }

    private fun openBasicBrushMenu(player: Player, session: PaintSession) {
        val settings = session.toolSettings.basicBrush
        val interactive = setOf(11, 12, 14, 15)
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, interactive)
        fillThreeRowBase(menu)
        menu.setButton(11, sizeButton(settings.normalizedSize()) {
            requestNumericInput(player, PaintMenuKind.BASIC_BRUSH, "↑ Размер ↑") { input ->
                input?.toIntOrNull()?.let { settings.size = it.coerceIn(0, 50).coerceAtLeast(1) }
            }
        })
        menu.setButton(12, fillPercentButton(settings.normalizedFillPercent()) {
            requestNumericInput(player, PaintMenuKind.BASIC_BRUSH, "↑ % заполнения ↑") { input ->
                input?.toIntOrNull()?.let { settings.fillPercent = it.coerceIn(1, 100) }
            }
        })
        menu.setButton(14, shadeButton(settings.shade) { newShade ->
            settings.applyShadeSelection(newShade)
            reopenMenu(player, session, PaintMenuKind.BASIC_BRUSH)
        })
        menu.setButton(15, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix(), settings.shade) { interaction ->
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    settings.shadeMixFocusIndex = (settings.shadeMixFocusIndex + 1) % PaintShade.ordered.size
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    val focusedShade = PaintShade.ordered[settings.shadeMixFocusIndex]
                    if (focusedShade in settings.shadeMix) {
                        settings.shadeMix.remove(focusedShade)
                    } else {
                        settings.shadeMix += focusedShade
                    }
                    if (settings.shadeMix.isEmpty()) {
                        settings.shadeMix += settings.shade
                    }
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    settings.shadeMix.clear()
                    settings.shadeMix += settings.shade
                    settings.shadeMixFocusIndex = PaintShade.ordered.indexOf(settings.shade).coerceAtLeast(0)
                }
            }
            reopenMenu(player, session, PaintMenuKind.BASIC_BRUSH)
        })
        markMenuOpen(player, session, PaintMenuKind.BASIC_BRUSH, menu)
    }

    private fun openBinaryBrushMenu(
        player: Player,
        session: PaintSession,
        kind: PaintMenuKind,
        title: String,
        settings: PaintBinaryBrushSettings,
        shears: Boolean = false
    ) {
        val interactive = setOf(12, 14)
        val menu = basePaintMenu(session, "▍ $title", MenuRows.THREE, 27, interactive)
        fillThreeRowBase(menu)
        menu.setButton(12, sizeButton(settings.normalizedSize()) {
            requestNumericInput(player, kind, "↑ Размер ↑") { input ->
                input?.toIntOrNull()?.let { settings.size = it.coerceIn(0, 50).coerceAtLeast(1) }
            }
        })
        menu.setButton(14, fillPercentButton(settings.normalizedFillPercent()) {
            requestNumericInput(player, kind, "↑ % заполнения ↑") { input ->
                input?.toIntOrNull()?.let { settings.fillPercent = it.coerceIn(1, 100) }
            }
        })
        markMenuOpen(player, session, if (shears) PaintMenuKind.ERASER else kind, menu)
    }

    private fun openFillMenu(player: Player, session: PaintSession) {
        val settings = session.toolSettings.fill
        val interactive = setOf(11, 12, 14, 15)
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, interactive)
        fillThreeRowBase(menu)
        menu.setButton(11, fillPercentButton(settings.normalizedFillPercent()) {
            requestNumericInput(player, PaintMenuKind.FILL, "↑ % заполнения ↑") { input ->
                input?.toIntOrNull()?.let { settings.fillPercent = it.coerceIn(1, 100) }
            }
        })
        menu.setButton(12, colorPickerButton("🌧", settings.paletteKey) {
            openColorPickerMenu(player, session, PaintMenuKind.FILL) { selected ->
                settings.paletteKey = selected
                reopenMenu(player, session, PaintMenuKind.FILL)
            }
        })
        menu.setButton(14, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix(), settings.baseShade) { interaction ->
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    settings.shadeMixFocusIndex = (settings.shadeMixFocusIndex + 1) % PaintShade.ordered.size
                    val focused = PaintShade.ordered[settings.shadeMixFocusIndex]
                    settings.baseShade = focused
                    settings.shadeMix += focused
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    val focused = PaintShade.ordered[settings.shadeMixFocusIndex]
                    if (focused in settings.shadeMix) {
                        if (settings.shadeMix.size > 1) {
                            settings.shadeMix.remove(focused)
                            if (settings.baseShade == focused) {
                                settings.baseShade = settings.shadeMix.first()
                                settings.shadeMixFocusIndex = PaintShade.ordered.indexOf(settings.baseShade).coerceAtLeast(0)
                            }
                        }
                    } else {
                        settings.shadeMix += focused
                    }
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    settings.shadeMix.clear()
                    settings.shadeMix += settings.baseShade
                    settings.shadeMixFocusIndex = PaintShade.ordered.indexOf(settings.baseShade).coerceAtLeast(0)
                }
            }
            reopenMenu(player, session, PaintMenuKind.FILL)
        })
        menu.setButton(15, ignoreShadeButton(settings.ignoreShade) {
            settings.ignoreShade = !settings.ignoreShade
            reopenMenu(player, session, PaintMenuKind.FILL)
        })
        markMenuOpen(player, session, PaintMenuKind.FILL, menu)
    }

    private fun openCustomBrushMenu(player: Player, session: PaintSession) {
        val settings = session.toolSettings.customBrush
        val interactive = setOf(10, 11, 13, 15, 16)
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, interactive)
        fillThreeRowBase(menu)
        menu.setButton(10, sizeButton(settings.normalizedSize()) {
            requestNumericInput(player, PaintMenuKind.CUSTOM_BRUSH, "↑ Размер ↑") { input ->
                input?.toIntOrNull()?.let { settings.size = it.coerceIn(0, 50).coerceAtLeast(1) }
            }
        })
        menu.setButton(11, fillPercentButton(settings.normalizedFillPercent()) {
            requestNumericInput(player, PaintMenuKind.CUSTOM_BRUSH, "↑ % заполнения ↑") { input ->
                input?.toIntOrNull()?.let { settings.fillPercent = it.coerceIn(1, 100) }
            }
        })
        menu.setButton(13, colorPickerButton("✎", settings.paletteKey) {
            openColorPickerMenu(player, session, PaintMenuKind.CUSTOM_BRUSH) { selected ->
                settings.paletteKey = selected
                reopenMenu(player, session, PaintMenuKind.CUSTOM_BRUSH)
            }
        })
        menu.setButton(15, shadeButton(settings.shade) { newShade ->
            settings.applyShadeSelection(newShade)
            reopenMenu(player, session, PaintMenuKind.CUSTOM_BRUSH)
        })
        menu.setButton(16, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix(), settings.shade) { interaction ->
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    settings.shadeMixFocusIndex = (settings.shadeMixFocusIndex + 1) % PaintShade.ordered.size
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    val focused = PaintShade.ordered[settings.shadeMixFocusIndex]
                    if (focused in settings.shadeMix) {
                        settings.shadeMix.remove(focused)
                    } else {
                        settings.shadeMix += focused
                    }
                    if (settings.shadeMix.isEmpty()) {
                        settings.shadeMix += settings.shade
                    }
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    settings.shadeMix.clear()
                    settings.shadeMix += settings.shade
                    settings.shadeMixFocusIndex = PaintShade.ordered.indexOf(settings.shade).coerceAtLeast(0)
                }
            }
            reopenMenu(player, session, PaintMenuKind.CUSTOM_BRUSH)
        })
        markMenuOpen(player, session, PaintMenuKind.CUSTOM_BRUSH, menu)
    }

    private fun openShapeMenu(player: Player, session: PaintSession) {
        val settings = session.toolSettings.shape
        val interactive = setOf(10, 11, 12, 13, 14, 15, 16)
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, interactive)
        fillThreeRowBase(menu)
        menu.setButton(10, sizeButton(settings.normalizedSize()) {
            requestNumericInput(player, PaintMenuKind.SHAPE, "↑ Размер ↑") { input ->
                input?.toIntOrNull()?.let { settings.size = it.coerceIn(0, 50).coerceAtLeast(1) }
            }
        })
        menu.setButton(11, fillPercentButton(settings.normalizedFillPercent()) {
            requestNumericInput(player, PaintMenuKind.SHAPE, "↑ % заполнения ↑") { input ->
                input?.toIntOrNull()?.let { settings.fillPercent = it.coerceIn(1, 100) }
            }
        })
        menu.setButton(12, colorPickerButton("⭐", settings.paletteKey) {
            openColorPickerMenu(player, session, PaintMenuKind.SHAPE) { selected ->
                settings.paletteKey = selected
                reopenMenu(player, session, PaintMenuKind.SHAPE)
            }
        })
        menu.setButton(13, shapeFillToggleButton(settings.filled) {
            settings.filled = !settings.filled
            reopenMenu(player, session, PaintMenuKind.SHAPE)
        })
        menu.setButton(14, shapeTypeButton(settings.shapeType) { shape ->
            settings.shapeType = shape
            reopenMenu(player, session, PaintMenuKind.SHAPE)
        })
        menu.setButton(15, shadeButton(settings.shade) { newShade ->
            settings.applyShadeSelection(newShade)
            reopenMenu(player, session, PaintMenuKind.SHAPE)
        })
        menu.setButton(16, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix(), settings.shade) { interaction ->
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    settings.shadeMixFocusIndex = (settings.shadeMixFocusIndex + 1) % PaintShade.ordered.size
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    val focused = PaintShade.ordered[settings.shadeMixFocusIndex]
                    if (focused in settings.shadeMix) {
                        settings.shadeMix.remove(focused)
                    } else {
                        settings.shadeMix += focused
                    }
                    if (settings.shadeMix.isEmpty()) {
                        settings.shadeMix += settings.shade
                    }
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    settings.shadeMix.clear()
                    settings.shadeMix += settings.shade
                    settings.shadeMixFocusIndex = PaintShade.ordered.indexOf(settings.shade).coerceAtLeast(0)
                }
            }
            reopenMenu(player, session, PaintMenuKind.SHAPE)
        })
        markMenuOpen(player, session, PaintMenuKind.SHAPE, menu)
    }

    private fun openEaselMenu(player: Player, session: PaintSession) {
        session.resizeMode = false
        removeResizePreview(player, session)
        val interactive = setOf(12, 14)
        val menu = basePaintMenu(session, "▍ Параметры мальберта", MenuRows.THREE, 27, interactive)
        fillThreeRowBase(menu)
        menu.setButton(12, buttonFactory.actionButton(
            material = Material.WATER_BUCKET,
            name = "<!i><#C7A300>🌧 <#FFD700>Очистить мальберт",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")
        ) {
            clearCanvas(player, session)
            markMenuTransition(player.uniqueId)
            player.closeInventory()
        })
        menu.setButton(14, buttonFactory.actionButton(
            material = Material.ITEM_FRAME,
            name = "<!i><#C7A300>⭐ <#FFD700>Изменить размер",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")
        ) {
            session.resizeMode = true
            markMenuTransition(player.uniqueId)
            player.closeInventory()
        })
        markMenuOpen(player, session, PaintMenuKind.EASEL, menu)
    }

    private fun clearCanvas(player: Player, session: PaintSession) {
        restorePreviewIfNeeded(player, session)
        session.canvasCells.values.forEach { cell ->
            MapDataExtractor.fill(cell.mapId, BACKGROUND_COLOR_ID)?.let { snapshot ->
                sendFullMapDataToViewers(session, snapshot)
            }
        }
        clearStrokeState(session)
        clearHistory(session)
    }

    private fun openColorPickerMenu(
        player: Player,
        session: PaintSession,
        returnTo: PaintMenuKind,
        onSelect: (String) -> Unit
    ) {
        openColorPickerPage(player, session, returnTo, 0, onSelect)
    }

    private fun openColorPickerPage(
        player: Player,
        session: PaintSession,
        returnTo: PaintMenuKind,
        page: Int,
        onSelect: (String) -> Unit
    ) {
        val pageSize = COLOR_MENU_SLOTS.size
        val totalPages = max(1, (PaintPalette.entries.size + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(0, totalPages - 1)
        val title = "▍ Выбор цвета [${safePage + 1}/$totalPages]"
        val interactive = COLOR_MENU_SLOTS.toMutableSet().apply {
            add(18)
            add(26)
        }
        val menu = basePaintMenu(session, title, MenuRows.FIVE, 45, interactive)
        fillColorPickerBase(menu)

        val slice = PaintPalette.entries.drop(safePage * pageSize).take(pageSize)
        COLOR_MENU_SLOTS.zip(slice).forEach { (slot, entry) ->
            val selected = isPaletteSelected(session, returnTo, entry.key)
            menu.setButton(slot, colorChoiceButton(entry, slot, selected) {
                onSelect(entry.key)
            })
        }

        if (safePage > 0) {
            menu.setButton(18, buttonFactory.backButton { openColorPickerPage(player, session, returnTo, safePage - 1, onSelect) })
        }
        if (safePage + 1 < totalPages) {
            menu.setButton(26, buttonFactory.forwardButton { openColorPickerPage(player, session, returnTo, safePage + 1, onSelect) })
        }

        session.activeColorMenuReturnTo = returnTo
        markMenuOpen(player, session, PaintMenuKind.COLOR_PICKER, menu)
    }

    private fun isPaletteSelected(session: PaintSession, returnTo: PaintMenuKind, key: String): Boolean {
        return when (returnTo) {
            PaintMenuKind.FILL -> session.toolSettings.fill.paletteKey == key
            PaintMenuKind.CUSTOM_BRUSH -> session.toolSettings.customBrush.paletteKey == key
            PaintMenuKind.SHAPE -> session.toolSettings.shape.paletteKey == key
            else -> false
        }
    }

    private fun reopenMenu(player: Player, session: PaintSession, menuKind: PaintMenuKind) {
        refreshToolInventory(player, session)
        when (menuKind) {
            PaintMenuKind.BASIC_BRUSH -> openBasicBrushMenu(player, session)
            PaintMenuKind.ERASER -> {
                val tool = resolveTool(player.inventory.itemInMainHand)
                if (tool?.mode == PaintToolMode.SHEARS) {
                    openBinaryBrushMenu(player, session, PaintMenuKind.ERASER, "Настройка кисти", session.toolSettings.shears, shears = true)
                } else {
                    openBinaryBrushMenu(player, session, PaintMenuKind.ERASER, "Настройка кисти", session.toolSettings.eraser)
                }
            }
            PaintMenuKind.FILL -> openFillMenu(player, session)
            PaintMenuKind.CUSTOM_BRUSH -> openCustomBrushMenu(player, session)
            PaintMenuKind.SHAPE -> openShapeMenu(player, session)
            PaintMenuKind.EASEL -> openEaselMenu(player, session)
            PaintMenuKind.COLOR_PICKER -> session.activeColorMenuReturnTo?.let { reopenMenu(player, session, it) }
        }
    }

    private fun requestNumericInput(
        player: Player,
        returnTo: PaintMenuKind,
        secondLine: String,
        apply: (String?) -> Unit
    ) {
        markMenuTransition(player.uniqueId)
        player.closeInventory()
        signInputService.open(
            player = player,
            templateLines = arrayOf("", secondLine, "", ""),
            onSubmit = { submitPlayer, input ->
                Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                    if (!isPainting(submitPlayer)) return@Runnable
                    apply(input)
                    sessions[submitPlayer.uniqueId]?.let { liveSession ->
                        refreshToolInventory(submitPlayer, liveSession)
                        reopenMenu(submitPlayer, liveSession, returnTo)
                    }
                })
            },
            onLeave = { leavePlayer ->
                Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                    sessions[leavePlayer.uniqueId]?.let { liveSession ->
                        if (!isPainting(leavePlayer)) return@Runnable
                        refreshToolInventory(leavePlayer, liveSession)
                        reopenMenu(leavePlayer, liveSession, returnTo)
                    }
                })
            }
        )
    }

    private fun basePaintMenu(
        session: PaintSession,
        title: String,
        rows: MenuRows,
        menuSize: Int,
        interactiveTopSlots: Set<Int>
    ): Menu {
        return MenuUiSupport.buildMenu(
            plugin = hooker.plugin,
            parser = parser,
            title = "<!i>$title",
            rows = rows,
            menuTopRange = 0 until menuSize,
            interactiveTopSlots = interactiveTopSlots,
            allowPlayerInventoryClicks = false,
            onClose = { event -> handlePaintMenuClose(session, event) }
        )
    }

    private fun handlePaintMenuClose(session: PaintSession, event: CloseEvent) {
        if (event.player.uniqueId in menuTransitions) return
        if (sessions[event.player.uniqueId] != session) return
        session.isMenuOpen = false
        session.openMenuKind = null
        session.activeColorMenuReturnTo = null
    }

    private fun markMenuOpen(player: Player, session: PaintSession, kind: PaintMenuKind, menu: Menu) {
        session.isMenuOpen = true
        session.openMenuKind = kind
        markMenuTransition(player.uniqueId)
        menu.open(player)
    }

    private fun markMenuTransition(playerId: UUID) {
        menuTransitions += playerId
        hooker.tickScheduler.runLater(1L) {
            menuTransitions.remove(playerId)
        }
    }

    private fun fillThreeRowBase(menu: Menu) {
        MenuUiSupport.fillByMask(
            menu = menu,
            menuSize = 27,
            primarySlots = THREE_ROW_BLACK_SLOTS,
            primaryButton = buttonFactory.blackFillerButton(),
            secondaryButton = buttonFactory.grayFillerButton()
        )
    }

    private fun fillColorPickerBase(menu: Menu) {
        MenuUiSupport.fillByMask(
            menu = menu,
            menuSize = 45,
            primarySlots = FIVE_ROW_COLOR_BLACK_SLOTS,
            primaryButton = buttonFactory.blackFillerButton(),
            secondaryButton = buttonFactory.grayFillerButton()
        )
    }

    private fun sizeButton(size: Int, action: () -> Unit) = buttonFactory.actionButton(
        material = Material.INK_SAC,
        name = "<!i><#C7A300>✎ <#FFD700>Размер кисти: <#FFF3E0>$size",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = { action() }
    )

    private fun fillPercentButton(fillPercent: Int, action: () -> Unit) = buttonFactory.actionButton(
        material = Material.FIRE_CHARGE,
        name = "<!i><#C7A300>₪ <#FFD700>Заполнение: <#FFF3E0>${fillPercent}%",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = { action() }
    )

    private fun shadeButton(current: PaintShade, action: (PaintShade) -> Unit) = buttonFactory.listButton(
        material = Material.GLOW_INK_SAC,
        options = PaintShade.ordered.map { MenuButtonFactory.ListButtonOption(it, it.displayName) },
        selectedIndex = PaintShade.ordered.indexOf(current).coerceAtLeast(0),
        titleBuilder = { _, _ -> "<!i><#C7A300>☀ <#FFD700>Оттенок кисти" },
        beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
        afterOptionsLore = listOf("")
    ) { _, newIndex ->
        action(PaintShade.ordered[newIndex])
    }

    private fun shadeMixButton(
        focusIndex: Int,
        enabledShades: Set<PaintShade>,
        baseShade: PaintShade,
        action: (MenuButtonFactory.FocusedToggleListInteraction) -> Unit
    ) = buttonFactory.textOrderedColorFocusButton(
        material = Material.MAGMA_CREAM,
        activeTitle = "<!i><#C7A300>🧪 <#FFD700>Смешение оттенков",
        inactiveTitle = "<!i><#C7A300>🧪 <#FFD700>Смешение оттенков",
        active = enabledShades.size > 1,
        options = PaintShade.ordered.mapIndexed { index, shade ->
            MenuButtonFactory.TextColorFocusOption(
                colorTag = if (shade == baseShade) "white" else "gold",
                label = shade.displayName,
                enabled = shade in enabledShades,
                focused = index == focusIndex.coerceIn(0, PaintShade.ordered.lastIndex),
                order = shade.mixNumber
            )
        }
    ) { _, interaction ->
        action(interaction)
    }

    private fun ignoreShadeButton(enabled: Boolean, action: () -> Unit) = buttonFactory.toggleButton(
        material = Material.FERMENTED_SPIDER_EYE,
        enabled = enabled,
        enabledName = "<!i><#C7A300>🔥 <#FFD700>Игнорировать оттенок: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>🔥 <#FFD700>Игнорировать оттенок: <#FF1500>Выкл",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = { action() }
    )

    private fun colorPickerButton(icon: String, paletteKey: String, action: () -> Unit) = buttonFactory.actionButton(
        material = Material.STRUCTURE_VOID,
        name = "<!i><#C7A300>$icon <#FFD700>Цвет: <${PaintPalette.entry(paletteKey).hexColor}>${PaintPalette.entry(paletteKey).displayName}",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            edit { item ->
                val meta = item.itemMeta ?: return@edit
                val entry = PaintPalette.entry(paletteKey)
                if (!entry.isTransparent) {
                    meta.itemModel = NamespacedKey.minecraft(entry.itemMaterial.key.key)
                }
                item.itemMeta = meta
            }
            this
        },
        action = { action() }
    )

    private fun shapeFillToggleButton(enabled: Boolean, action: () -> Unit) = buttonFactory.toggleButton(
        material = Material.POWDER_SNOW_BUCKET,
        enabled = enabled,
        enabledName = "<!i><#C7A300>◎ <#FFD700>Заполнение: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Заполнение: <#FF1500>Выкл",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = { action() }
    )

    private fun shapeTypeButton(current: PaintShapeType, action: (PaintShapeType) -> Unit) = buttonFactory.listButton(
        material = Material.ECHO_SHARD,
        options = PaintShapeType.entries.map { MenuButtonFactory.ListButtonOption(it, it.displayName) },
        selectedIndex = PaintShapeType.entries.indexOf(current).coerceAtLeast(0),
        titleBuilder = { _, _ -> "<!i><#C7A300>₪ <#FFD700>Форма кисти" },
        beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
        afterOptionsLore = listOf("")
    ) { _, newIndex ->
        action(PaintShapeType.entries[newIndex])
    }

    private fun colorChoiceButton(entry: PaintPaletteEntry, slot: Int, selected: Boolean, action: () -> Unit) = buttonFactory.actionButton(
        material = Material.STRUCTURE_VOID,
        name = "<!i><${entry.hexColor}>${decoratedColorName(entry.displayName, slot)}",
        lore = emptyList(),
        itemModifier = {
            edit { item ->
                val meta = item.itemMeta ?: return@edit
                if (!entry.isTransparent) {
                    meta.itemModel = NamespacedKey.minecraft(entry.itemMaterial.key.key)
                }
                item.itemMeta = meta
            }
            if (selected) {
                glint(true)
            }
            this
        },
        action = { action() }
    )

    private fun decoratedColorName(name: String, slot: Int): String {
        return when (slot) {
            in 1..6, in 37..43 -> "→ $name"
            7, 16 -> "$name ↓"
            in 20..25 -> "$name ←"
            19, 28 -> "↓ $name"
            else -> name
        }
    }

    private fun giveResult(player: Player, session: PaintSession) {
        val cells = session.cellsSortedTopLeft()
        if (cells.size <= 1) {
            val only = cells.firstOrNull() ?: return
            giveSingleMapItem(player, createArtworkMapItem(only.mapId, null, null, player.name, session.seriesCode))
            return
        }
        giveItem(player, createArtworkShulker(cells, player.name, session.seriesCode))
    }

    private fun createArtworkMapItem(
        mapId: Int,
        rowNumber: Int?,
        partNumber: Int?,
        author: String,
        seriesCode: String
    ): BukkitItemStack {
        val item = BukkitItemStack(Material.FILLED_MAP)
        MapItemSupport.resolveMapView(mapId)?.let { mapView ->
            MapItemSupport.setMapView(item, mapView)
        }
        item.editMeta { meta ->
            val name = if (rowNumber == null || partNumber == null) {
                "<!i><#FFD700>Рисунок"
            } else {
                "<!i><#FFD700>Строка $rowNumber <#C7A300>[<#FFF3E0>Часть $partNumber<#C7A300>]"
            }
            meta.displayName(parser.parse(name))
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            meta.lore(
                listOf(
                    "<!i><#FFD700>▍ <#FFE68A>Автор: <#FFF3E0>$author",
                    "<!i><#FFD700>▍ <#FFE68A>Серия: <#FFF3E0>$seriesCode"
                ).map(parser::parse)
            )
        }
        return item
    }

    private fun createArtworkShulker(
        cells: List<PaintCanvasCell>,
        author: String,
        seriesCode: String
    ): BukkitItemStack {
        val bounds = PaintCanvasBounds.from(cells.map { it.point }) ?: return BukkitItemStack(Material.WHITE_SHULKER_BOX)
        val shulker = BukkitItemStack(Material.WHITE_SHULKER_BOX)
        shulker.editMeta { rawMeta ->
            rawMeta.displayName(parser.parse("<!i><#EDC800>Содержимое рисунка"))
            val meta = rawMeta as? BlockStateMeta ?: return@editMeta
            val box = meta.blockState as? ShulkerBox ?: return@editMeta
            box.customName(parser.parse("<!i><#EDC800>Содержимое рисунка"))

            val contents = arrayOfNulls<BukkitItemStack>(27)
            cells.forEach { cell ->
                val rowNumber = cell.point.y - bounds.minY + 1
                val partNumber = cell.point.x - bounds.minX + 1
                val rowStart = SHULKER_ROW_STARTS.getOrElse(rowNumber - 1) { 0 }
                val slot = rowStart + (partNumber - 1)
                if (slot in contents.indices) {
                    contents[slot] = createArtworkMapItem(cell.mapId, rowNumber, partNumber, author, seriesCode)
                }
            }
            box.inventory.contents = contents
            meta.blockState = box
        }
        return shulker
    }

    private fun giveSingleMapItem(player: Player, item: BukkitItemStack) {
        val handItem = player.inventory.itemInMainHand
        if (handItem.type == Material.AIR || handItem.amount <= 0) {
            player.inventory.setItemInMainHand(item)
            return
        }
        giveItem(player, item)
    }

    private fun giveItem(player: Player, item: BukkitItemStack) {
        val remainingAmount = PlayerInventoryTransferSupport.storeInPreferredSlots(player.inventory, item)
        if (remainingAmount > 0) {
            val dropped = player.world.dropItem(
                player.location.clone().add(0.0, 1.0, 0.0),
                item.clone().apply { amount = remainingAmount }
            )
            hooker.utils.getPlayersWithHides().forEach { hider ->
                hooker.hideManager.hideDroppedItem(hider, dropped, player)
            }
        }
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

    private fun sendFullMapDataToViewers(session: PaintSession, snapshot: MapDataExtractor.Snapshot) {
        session.viewers.forEach { viewerId ->
            Bukkit.getPlayer(viewerId)?.let { viewer ->
                sendFullMapData(viewer, snapshot)
            }
        }
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

    private fun isWorkTool(item: BukkitItemStack?): Boolean = resolveTool(item) != null

    private fun isDropClick(click: ClickType): Boolean {
        return click == ClickType.DROP || click == ClickType.CONTROL_DROP
    }

    private fun floorDiv(value: Int, divisor: Int): Int = Math.floorDiv(value, divisor)

    private fun floorMod(value: Int, divisor: Int): Int = Math.floorMod(value, divisor)

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
        private const val STROKE_CONTINUE_WINDOW_MILLIS = 250L
        private const val PLANE_EPSILON = 1.0E-6
        private const val MIN_CANVAS_FACING_DOT = -0.1
        private const val PREVIEW_ALPHA = 0.42
        private const val FILL_COOLDOWN_TICKS = 5L
        private const val LOCATION_EPSILON = 0.01
        private const val HISTORY_PIXEL_ESTIMATE_BYTES = 40L
        private const val HISTORY_MAP_GROUP_BASE_BYTES = 96L
        private const val HISTORY_ENTRY_BASE_BYTES = 64L
        private const val MAX_HISTORY_BYTES = 32L * 1024L * 1024L
        private const val GLOBAL_CANVAS_HASH_BASE = 100_000
        private val BACKGROUND_COLOR_ID: Byte = PaintPalette.SNOW.packed(PaintShade.NORMAL)
        private val THREE_ROW_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26)
        private val FIVE_ROW_COLOR_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
        private val COLOR_MENU_SLOTS = listOf(1, 2, 3, 4, 5, 6, 7, 16, 25, 24, 23, 22, 21, 20, 19, 28, 37, 38, 39, 40, 41, 42, 43)
        private val SHULKER_ROW_STARTS = listOf(0, 9, 18, 5)
    }
}
