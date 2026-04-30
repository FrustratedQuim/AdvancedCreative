package com.ratger.acreative.paint.model

import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Location
import java.util.ArrayDeque
import java.util.UUID

data class PaintResizePreview(
    val frame: WrapperEntity,
    val teamName: String,
    val viewers: MutableSet<UUID> = mutableSetOf(),
    var targetPoint: PaintGridPoint? = null,
    var canAdd: Boolean = true
)

data class PaintSession(
    val playerId: UUID,
    val frameDirection: PaintFrameDirection,
    val anchorLocation: Location,
    val anchorPoint: PaintGridPoint,
    val initialSize: PaintCanvasSize,
    val inventorySnapshot: PaintInventorySnapshot,
    var viewerTaskId: Int,
    var previewTaskId: Int,
    val viewers: MutableSet<UUID>,
    val canvasCells: MutableMap<PaintGridPoint, PaintCanvasCell>,
    val toolSettings: PaintToolSettingsBundle = PaintToolSettingsBundle(),
    val history: ArrayDeque<PaintHistoryEntry> = ArrayDeque(),
    val seriesCode: String,
    var previewMapIds: MutableSet<Int> = mutableSetOf(),
    var previewFingerprint: String? = null,
    var historyBytes: Long = 0L,
    var lastInputKind: PaintInputKind? = null,
    var lastInputAtMillis: Long = 0L,
    var lastStrokeGlobalX: Int? = null,
    var lastStrokeGlobalY: Int? = null,
    var lastStrokeColor: Byte? = null,
    var lastStrokeAtMillis: Long = 0L,
    var lastPaletteRotationAtMillis: Long = 0L,
    var isMenuOpen: Boolean = false,
    var openMenuKind: PaintMenuKind? = null,
    var activeBasicBrushPaletteKey: String? = null,
    var activeColorMenuReturnTo: PaintMenuKind? = null,
    var resizeMode: Boolean = false,
    var resizePreview: PaintResizePreview? = null,
    var resizePreviewMapId: Int? = null,
    var shapeLineAnchor: PaintLineAnchor? = null,
    var currentTick: Long = 0L,
    var canvasRevision: Long = 0L,
    var canvasTopologyRevision: Long = 0L,
    var fillCooldownUntilMillis: Long = 0L,
    var brushPreviewSuppressedUntilMillis: Long = 0L,
    var previewPaused: Boolean = false,
    var previewSuppressionKey: String? = null,
    var paletteRotation: Int = 0
) {
    fun cellsSortedTopLeft(): List<PaintCanvasCell> {
        return canvasCells.values.sortedWith(compareBy<PaintCanvasCell> { it.point.y }.thenBy { it.point.x })
    }
}
