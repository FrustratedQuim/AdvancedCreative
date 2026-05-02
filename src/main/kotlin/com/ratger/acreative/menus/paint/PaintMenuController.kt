package com.ratger.acreative.menus.paint

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.edit.experimental.ComponentsService
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.paint.model.PaintBinaryBrushSettings
import com.ratger.acreative.paint.model.PaintMenuKind
import com.ratger.acreative.paint.model.PaintSession
import com.ratger.acreative.paint.model.PaintShade
import com.ratger.acreative.paint.model.PaintShapeType
import com.ratger.acreative.paint.model.PaintToolMode
import com.ratger.acreative.paint.palette.PaintPalette
import com.ratger.acreative.paint.palette.PaintPaletteEntry
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.menu.event.CloseEvent
import java.util.UUID
import kotlin.math.max

class PaintMenuController(
    private val hooker: FunctionHooker,
    private val parser: MiniMessageParser,
    private val callbacks: PaintMenuCallbacks
) {
    private val buttonFactory = MenuButtonFactory(parser, ComponentsService(), hooker.tickScheduler)
    private val signInputService = SignInputService(hooker.plugin)
    private val menuTransitions = mutableSetOf<UUID>()

    fun handleInventoryClose(player: Player, session: PaintSession) {
        if (!session.isMenuOpen) return
        if (player.uniqueId in menuTransitions) return
        finalizeMenuClose(player, session, session.openMenuKind)
    }

    fun openSettingsForCurrentTool(player: Player, session: PaintSession) {
        val tool = callbacks.resolveTool(player.inventory.itemInMainHand) ?: return
        when (tool.mode) {
            PaintToolMode.BASIC_COLOR_BRUSH -> openBasicBrushMenu(player, session, requireNotNull(tool.fixedPaletteKey))
            PaintToolMode.CUSTOM_BRUSH -> openCustomBrushMenu(player, session)
            PaintToolMode.ERASER -> openBinaryBrushMenu(player, session, PaintMenuKind.ERASER, "Настройка кисти", session.toolSettings.eraser)
            PaintToolMode.SHEARS -> openBinaryBrushMenu(player, session, PaintMenuKind.ERASER, "Настройка кисти", session.toolSettings.shears, shears = true)
            PaintToolMode.FILL -> openFillMenu(player, session)
            PaintToolMode.SHAPE -> openShapeMenu(player, session)
            PaintToolMode.EASEL -> openEaselMenu(player, session)
        }
    }

    private fun openBasicBrushMenu(player: Player, session: PaintSession, paletteKey: String) {
        session.activeBasicBrushPaletteKey = paletteKey
        val settings = session.toolSettings.basicBrush(paletteKey)
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, setOf(11, 12, 14, 15))
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
        fun setShadeMixButton(target: Menu) {
            target.setButton(15, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix()) { event, interaction ->
                handleShadeMixInteraction(
                    interaction = interaction,
                    focusIndex = { settings.shadeMixFocusIndex },
                    setFocusIndex = { settings.shadeMixFocusIndex = it },
                    shadeMix = settings.shadeMix,
                    fallbackShade = settings.shade
                )
                callbacks.refreshTools(player, session)
                setShadeMixButton(event.menu)
            })
        }
        fun setShadeButton(target: Menu) {
            target.setButton(14, shadeButton(settings.shade) { event, newShade ->
                settings.applyShadeSelection(newShade)
                callbacks.refreshTools(player, session)
                setShadeButton(event.menu)
                setShadeMixButton(event.menu)
            })
        }
        setShadeButton(menu)
        setShadeMixButton(menu)
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
        val menu = basePaintMenu(session, "▍ $title", MenuRows.THREE, 27, setOf(12, 14))
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
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, setOf(10, 11, 12, 14, 15))
        fillThreeRowBase(menu)
        menu.setButton(10, fillPercentButton(settings.normalizedFillPercent()) {
            requestNumericInput(player, PaintMenuKind.FILL, "↑ % заполнения ↑") { input ->
                input?.toIntOrNull()?.let { settings.fillPercent = it.coerceIn(1, 100) }
            }
        })
        menu.setButton(11, colorPickerButton("🌧", settings.paletteKey) {
            openColorPickerMenu(player, session, PaintMenuKind.FILL) { selected ->
                settings.paletteKey = selected
                reopenMenu(player, session, PaintMenuKind.FILL)
            }
        })
        fun setShadeMixButton(target: Menu) {
            target.setButton(14, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix()) { event, interaction ->
                handleShadeMixInteraction(
                    interaction = interaction,
                    focusIndex = { settings.shadeMixFocusIndex },
                    setFocusIndex = { settings.shadeMixFocusIndex = it },
                    shadeMix = settings.shadeMix,
                    fallbackShade = settings.baseShade
                )
                callbacks.refreshTools(player, session)
                setShadeMixButton(event.menu)
            })
        }
        fun setShadeButton(target: Menu) {
            target.setButton(12, shadeButton(settings.baseShade) { event, newShade ->
                settings.applyShadeSelection(newShade)
                callbacks.refreshTools(player, session)
                setShadeButton(event.menu)
                setShadeMixButton(event.menu)
            })
        }
        fun setIgnoreShadeButton(target: Menu) {
            target.setButton(15, ignoreShadeButton(settings.ignoreShade) { event ->
                settings.ignoreShade = !settings.ignoreShade
                callbacks.refreshTools(player, session)
                setIgnoreShadeButton(event.menu)
            })
        }
        setShadeButton(menu)
        setShadeMixButton(menu)
        setIgnoreShadeButton(menu)
        markMenuOpen(player, session, PaintMenuKind.FILL, menu)
    }

    private fun openCustomBrushMenu(player: Player, session: PaintSession) {
        val settings = session.toolSettings.customBrush
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, setOf(10, 11, 13, 15, 16))
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
        fun setShadeMixButton(target: Menu) {
            target.setButton(16, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix()) { event, interaction ->
                handleShadeMixInteraction(
                    interaction = interaction,
                    focusIndex = { settings.shadeMixFocusIndex },
                    setFocusIndex = { settings.shadeMixFocusIndex = it },
                    shadeMix = settings.shadeMix,
                    fallbackShade = settings.shade
                )
                callbacks.refreshTools(player, session)
                setShadeMixButton(event.menu)
            })
        }
        fun setShadeButton(target: Menu) {
            target.setButton(15, shadeButton(settings.shade) { event, newShade ->
                settings.applyShadeSelection(newShade)
                callbacks.refreshTools(player, session)
                setShadeButton(event.menu)
                setShadeMixButton(event.menu)
            })
        }
        setShadeButton(menu)
        setShadeMixButton(menu)
        markMenuOpen(player, session, PaintMenuKind.CUSTOM_BRUSH, menu)
    }

    private fun openShapeMenu(player: Player, session: PaintSession) {
        val settings = session.toolSettings.shape
        val menu = basePaintMenu(session, "▍ Настройка кисти", MenuRows.THREE, 27, setOf(10, 11, 12, 13, 14, 15, 16))
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
        fun setFillToggleButton(target: Menu) {
            target.setButton(13, shapeFillToggleButton(settings.filled) { event ->
                settings.filled = !settings.filled
                callbacks.refreshTools(player, session)
                setFillToggleButton(event.menu)
            })
        }
        fun setShapeTypeButton(target: Menu) {
            target.setButton(14, shapeTypeButton(settings.shapeType) { event, shape ->
                settings.shapeType = shape
                callbacks.refreshTools(player, session)
                setShapeTypeButton(event.menu)
            })
        }
        fun setShadeMixButton(target: Menu) {
            target.setButton(16, shadeMixButton(settings.shadeMixFocusIndex, settings.normalizedShadeMix()) { event, interaction ->
                handleShadeMixInteraction(
                    interaction = interaction,
                    focusIndex = { settings.shadeMixFocusIndex },
                    setFocusIndex = { settings.shadeMixFocusIndex = it },
                    shadeMix = settings.shadeMix,
                    fallbackShade = settings.shade
                )
                callbacks.refreshTools(player, session)
                setShadeMixButton(event.menu)
            })
        }
        fun setShadeButton(target: Menu) {
            target.setButton(15, shadeButton(settings.shade) { event, newShade ->
                settings.applyShadeSelection(newShade)
                callbacks.refreshTools(player, session)
                setShadeButton(event.menu)
                setShadeMixButton(event.menu)
            })
        }
        setFillToggleButton(menu)
        setShapeTypeButton(menu)
        setShadeButton(menu)
        setShadeMixButton(menu)
        markMenuOpen(player, session, PaintMenuKind.SHAPE, menu)
    }

    fun openEaselMenu(player: Player, session: PaintSession) {
        session.resizeMode = false
        callbacks.removeResizePreview(player, session)
        val menu = basePaintMenu(session, "▍ Параметры мальберта", MenuRows.THREE, 27, setOf(11, 13, 15))
        fillThreeRowBase(menu)
        menu.setButton(11, buttonFactory.actionButton(
            material = Material.WATER_BUCKET,
            name = "<!i><#C7A300>🌧 <#FFD700>Очистить мальберт",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")
        ) {
            callbacks.clearCanvas(player, session)
            closeCurrentPaintMenu(player, session)
        })
        menu.setButton(13, buttonFactory.actionButton(
            material = Material.ITEM_FRAME,
            name = "<!i><#C7A300>⭐ <#FFD700>Изменить размер",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")
        ) {
            callbacks.beginResizeMode(player, session)
            closeCurrentPaintMenu(player, session)
        })
        menu.setButton(15, zoomButton(session) { newZoom ->
            session.selectedZoom = newZoom
            openEaselMenu(player, session)
        })
        markMenuOpen(player, session, PaintMenuKind.EASEL, menu)
    }

    private fun handleShadeMixInteraction(
        interaction: MenuButtonFactory.FocusedToggleListInteraction,
        focusIndex: () -> Int,
        setFocusIndex: (Int) -> Unit,
        shadeMix: MutableSet<PaintShade>,
        fallbackShade: PaintShade
    ) {
        when (interaction) {
            MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                setFocusIndex((focusIndex() + 1) % PaintShade.ordered.size)
            }
            MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                val focused = PaintShade.ordered[focusIndex().coerceIn(0, PaintShade.ordered.lastIndex)]
                if (focused in shadeMix) {
                    shadeMix.remove(focused)
                } else {
                    shadeMix += focused
                }
                if (shadeMix.isEmpty()) {
                    shadeMix += fallbackShade
                }
            }
            MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                shadeMix.clear()
                shadeMix += fallbackShade
            }
        }
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
        val menu = basePaintMenu(session, "▍ Выбор цвета [${safePage + 1}/$totalPages]", MenuRows.FIVE, 45, COLOR_MENU_SLOTS.toMutableSet().apply {
            add(18)
            add(26)
        })
        fillColorPickerBase(menu)

        PaintPalette.entries.drop(safePage * pageSize).take(pageSize).let { slice ->
            COLOR_MENU_SLOTS.zip(slice).forEach { (slot, entry) ->
                menu.setButton(slot, colorChoiceButton(entry, slot, isPaletteSelected(session, returnTo, entry.key)) {
                    onSelect(entry.key)
                })
            }
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
        callbacks.refreshTools(player, session)
        when (menuKind) {
            PaintMenuKind.BASIC_BRUSH -> {
                val paletteKey = session.activeBasicBrushPaletteKey
                    ?: callbacks.resolveTool(player.inventory.itemInMainHand)?.fixedPaletteKey
                    ?: return
                openBasicBrushMenu(player, session, paletteKey)
            }
            PaintMenuKind.ERASER -> {
                val tool = callbacks.resolveTool(player.inventory.itemInMainHand)
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
                hooker.tickScheduler.runNow {
                    if (callbacks.isPainting(submitPlayer)) {
                        apply(input)
                        callbacks.session(submitPlayer.uniqueId)?.let { liveSession ->
                            callbacks.refreshTools(submitPlayer, liveSession)
                            reopenMenu(submitPlayer, liveSession, returnTo)
                        }
                    }
                }
            },
            onLeave = { leavePlayer ->
                hooker.tickScheduler.runNow {
                    callbacks.session(leavePlayer.uniqueId)?.let { liveSession ->
                        if (callbacks.isPainting(leavePlayer)) {
                            callbacks.refreshTools(leavePlayer, liveSession)
                            reopenMenu(leavePlayer, liveSession, returnTo)
                        }
                    }
                }
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
        if (!session.isMenuOpen) return
        if (event.player.uniqueId in menuTransitions) return
        if (callbacks.session(event.player.uniqueId) != session) return
        finalizeMenuClose(event.player, session, session.openMenuKind)
    }

    private fun finalizeMenuClose(player: Player, session: PaintSession, closedKind: PaintMenuKind?) {
        session.isMenuOpen = false
        session.openMenuKind = null
        session.activeBasicBrushPaletteKey = null
        session.activeColorMenuReturnTo = null
        if (closedKind == PaintMenuKind.EASEL) {
            callbacks.handleEaselMenuClose(player, session)
        }
    }

    private fun markMenuOpen(player: Player, session: PaintSession, kind: PaintMenuKind, menu: Menu) {
        session.isMenuOpen = true
        session.openMenuKind = kind
        if (kind != PaintMenuKind.BASIC_BRUSH) {
            session.activeBasicBrushPaletteKey = null
        }
        markMenuTransition(player.uniqueId)
        menu.open(player)
    }

    private fun closeCurrentPaintMenu(player: Player, session: PaintSession) {
        finalizeMenuClose(player, session, null)
        markMenuTransition(player.uniqueId)
        player.closeInventory()
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

    private fun zoomButton(session: PaintSession, action: (Int) -> Unit) = run {
        val maxSide = session.maxLogicalSide()
        val allowed = session.allowedZoomLevels()
        val selectedZoom = session.selectedZoom
        if (maxSide >= 3) {
            buttonFactory.actionButton(
                material = Material.SPYGLASS,
                name = "<!i><#C7A300>⭐ <#FFD700>Приближение",
                lore = listOf("<!i><#C7A300>▍ <#FFE68A>Недоступно для текущего размера"),
                itemModifier = {
                    if (selectedZoom > 1) {
                        glint(true)
                    }
                    this
                }
            )
        } else {
            val options = listOf(1, 2, 3, 4)
            val lore = buildList {
                add("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")
                add("")
                options.forEach { option ->
                    val label = if (option == 1) "Нет" else "В $option раза"
                    val enabled = option in allowed
                    val selected = selectedZoom == option
                    add(
                        when {
                            selected && enabled -> "<!i><#00FF40>  » $label"
                            selected && !enabled -> "<!i><gray>  » $label"
                            enabled -> "<!i><#C7A300><b> </b>» $label"
                            else -> "<!i><#FF1500><b> </b>» $label"
                        }
                    )
                }
                add("")
            }
            buttonFactory.actionButton(
                material = Material.SPYGLASS,
                name = "<!i><#C7A300>⭐ <#FFD700>Приближение",
                lore = lore,
                itemModifier = {
                    if (selectedZoom > 1 || selectedZoom != session.appliedZoom) {
                        glint(true)
                    }
                    this
                },
                action = { event ->
                    val currentIndex = allowed.indexOf(selectedZoom).takeIf { it >= 0 } ?: 0
                    val newIndex = when {
                        event.isLeft || event.isShiftLeft -> (currentIndex + 1) % allowed.size
                        event.isRight || event.isShiftRight -> (currentIndex - 1 + allowed.size) % allowed.size
                        else -> return@actionButton
                    }
                    action(allowed[newIndex])
                }
            )
        }
    }

    private fun shadeButton(current: PaintShade, action: (ClickEvent, PaintShade) -> Unit) = buttonFactory.listButton(
        material = Material.GLOW_INK_SAC,
        options = PaintShade.ordered.map { MenuButtonFactory.ListButtonOption(it, it.displayName) },
        selectedIndex = PaintShade.ordered.indexOf(current).coerceAtLeast(0),
        titleBuilder = { _, _ -> "<!i><#C7A300>☀ <#FFD700>Оттенок кисти" },
        beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
        afterOptionsLore = listOf("")
    ) { event, newIndex ->
        action(event, PaintShade.ordered[newIndex])
    }

    private fun shadeMixButton(
        focusIndex: Int,
        enabledShades: Set<PaintShade>,
        action: (ClickEvent, MenuButtonFactory.FocusedToggleListInteraction) -> Unit
    ) = buttonFactory.focusedToggleListButton(
        material = Material.MAGMA_CREAM,
        title = "<!i><#C7A300>🧪 <#FFD700>Смешение оттенков",
        options = PaintShade.ordered.map { shade ->
            MenuButtonFactory.FocusedToggleListOption(
                label = shade.displayName,
                enabled = shade in enabledShades
            )
        },
        focusedIndex = focusIndex.coerceIn(0, PaintShade.ordered.lastIndex),
        beforeOptionsLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
            "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
            ""
        ),
        afterOptionsLore = listOf(""),
        itemModifier = {
            if (enabledShades.size > 1) {
                glint(true)
            }
            this
        }
    ) { event, interaction ->
        action(event, interaction)
    }

    private fun ignoreShadeButton(enabled: Boolean, action: (ClickEvent) -> Unit) = buttonFactory.toggleButton(
        material = Material.FERMENTED_SPIDER_EYE,
        enabled = enabled,
        enabledName = "<!i><#C7A300>🔥 <#FFD700>Игнорировать оттенок: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>🔥 <#FFD700>Игнорировать оттенок: <#FF1500>Выкл",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = action
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

    private fun shapeFillToggleButton(enabled: Boolean, action: (ClickEvent) -> Unit) = buttonFactory.toggleButton(
        material = Material.POWDER_SNOW_BUCKET,
        enabled = enabled,
        enabledName = "<!i><#C7A300>◎ <#FFD700>Заполнение: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Заполнение: <#FF1500>Выкл",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = action
    )

    private fun shapeTypeButton(current: PaintShapeType, action: (ClickEvent, PaintShapeType) -> Unit) = buttonFactory.listButton(
        material = Material.ECHO_SHARD,
        options = PaintShapeType.entries.map { MenuButtonFactory.ListButtonOption(it, it.displayName) },
        selectedIndex = PaintShapeType.entries.indexOf(current).coerceAtLeast(0),
        titleBuilder = { _, _ -> "<!i><#C7A300>₪ <#FFD700>Форма кисти" },
        beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
        afterOptionsLore = listOf("")
    ) { event, newIndex ->
        action(event, PaintShapeType.entries[newIndex])
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
            7, 16 -> "↓ $name"
            in 20..25 -> "← $name"
            19, 28 -> "↓ $name"
            else -> name
        }
    }

    private companion object {
        val THREE_ROW_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26)
        val FIVE_ROW_COLOR_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
        val COLOR_MENU_SLOTS = listOf(1, 2, 3, 4, 5, 6, 7, 16, 25, 24, 23, 22, 21, 20, 19, 28, 37, 38, 39, 40, 41, 42, 43)
    }
}
