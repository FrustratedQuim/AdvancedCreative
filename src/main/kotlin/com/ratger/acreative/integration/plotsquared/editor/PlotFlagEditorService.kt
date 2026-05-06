package com.ratger.acreative.integration.plotsquared.editor

import com.plotsquared.core.player.PlotPlayer
import com.plotsquared.core.plot.Plot
import com.plotsquared.core.plot.PlotArea
import com.plotsquared.core.plot.PlotId
import com.plotsquared.core.plot.PlotTitle
import com.plotsquared.core.plot.flag.PlotFlag
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.edit.restrictions.ItemRestrictionSupport
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.util.ItemBuilder
import java.util.UUID

class PlotFlagEditorService(
    private val hooker: FunctionHooker
) {
    private data class PlotReference(
        val area: PlotArea,
        val id: PlotId
    ) {
        fun resolve(): Plot? = area.getPlotAbs(id) ?: area.getPlot(id)
    }

    private enum class PlotFlagFilter {
        ALL,
        ADDED,
        MISSING
    }

    private sealed interface PlotEditorView {
        data class Main(val page: Int, val filter: PlotFlagFilter) : PlotEditorView
        data class Collection(val entryKey: String, val page: Int, val returnPage: Int, val returnFilter: PlotFlagFilter) : PlotEditorView
        data class BlockPicker(val entryKey: String, val page: Int, val returnCollectionPage: Int, val returnMainPage: Int, val returnFilter: PlotFlagFilter) : PlotEditorView
    }

    private data class ResolvedPlotFlagDefinition(
        val definition: PlotFlagDefinition,
        val flagClass: Class<out PlotFlag<*, *>>,
        val requiredRoleKey: String
    )

    private data class PlotEditorSession(
        val playerId: UUID,
        val plotReference: PlotReference,
        var view: PlotEditorView,
        var renderedMainEntries: List<String> = emptyList(),
        var renderedMainSlots: Map<String, Int> = emptyMap(),
        var renderedMainTotalPages: Int = 1,
        val pendingBlockSelections: MutableMap<String, MutableList<String>> = mutableMapOf()
    )

    private data class ApplyRequest(
        val timeoutTaskId: Int,
        val usageMessageKey: MessageKey,
        val suggestions: (String) -> List<String>,
        val onApply: (Player, String) -> Unit,
        val onReopen: (Player) -> Unit
    )

    private val parser = MiniMessageParser()
    private val promptService = ApplyPromptService(hooker.messageManager)
    private val signInputService = SignInputService(hooker.plugin)
    private val buttonFactory: MenuButtonFactory = hooker.menuService.buttonFactory()
    private val sessions = mutableMapOf<UUID, PlotEditorSession>()
    private val applyRequests = mutableMapOf<UUID, ApplyRequest>()
    private val menuTransitions = mutableSetOf<UUID>()
    private val mutationCooldowns = mutableSetOf<String>()
    private val emptyButton = buttonFactory.itemAsIsButton(ItemStack(Material.AIR)) { }
    private val resolvedFlagsByKey: Map<String, ResolvedPlotFlagDefinition>
    private val enabledFlagsInOrder: List<ResolvedPlotFlagDefinition>
    private val blockOptions: List<ItemRestrictionSupport.BlockOption>
    private val plotEditAdminAccessPermission: String = hooker.configManager.config.getString(CONFIG_ADMIN_PERMISSION)
        ?.trim()
        .takeUnless { it.isNullOrBlank() }
        ?: DEFAULT_ADMIN_PERMISSION
    private val enabled: Boolean = hooker.plugin.server.pluginManager.getPlugin(PLOT_SQUARED_PLUGIN_NAME)?.isEnabled == true

    init {
        val roleRequirements = resolveRoleRequirements(hooker.configManager.config.getConfigurationSection(CONFIG_ROLE_FLAGS))
        val loadedDefinitions = mutableListOf<ResolvedPlotFlagDefinition>()
        val loadedByKey = mutableMapOf<String, ResolvedPlotFlagDefinition>()
        enabledFlagKeys().forEach { groupKey ->
            PlotFlagCatalog.definitions
                .filter { it.groupKey.equals(groupKey, ignoreCase = true) }
                .forEach { definition ->
                    val flagClass = definition.resolveFlagClass()
                    val requiredRoleKey = roleRequirements[groupKey] ?: hooker.permissionManager.defaultRoleKey()
                    if (flagClass == null) {
                        hooker.plugin.logger.warning("Plot flag editor skipped '$groupKey': unable to resolve ${definition.flagClassCandidates.joinToString()}")
                        return@forEach
                    }
                    if (hooker.permissionManager.getRole(requiredRoleKey) == null) {
                        hooker.plugin.logger.warning("Plot flag editor skipped '$groupKey': unknown role '$requiredRoleKey'")
                        return@forEach
                    }
                    val resolved = ResolvedPlotFlagDefinition(definition, flagClass, requiredRoleKey)
                    loadedDefinitions += resolved
                    loadedByKey[definition.key] = resolved
                }
        }
        enabledFlagsInOrder = loadedDefinitions
        resolvedFlagsByKey = loadedByKey
        blockOptions = ItemRestrictionSupport.blockOptions()

        hooker.menuService.registerApplyTarget(object : ApplyCommandTarget {
            override fun isWaiting(player: Player): Boolean = applyRequests.containsKey(player.uniqueId)

            override fun handle(player: Player, args: Array<out String>): Boolean {
                val request = applyRequests[player.uniqueId] ?: return false
                if (args.isEmpty()) {
                    hooker.messageManager.sendChat(player, request.usageMessageKey)
                    return true
                }
                if (args[0].equals("cancel", ignoreCase = true)) {
                    clearApplyRequest(player, request)
                    request.onReopen(player)
                    return true
                }
                clearApplyRequest(player, request)
                request.onApply(player, args.joinToString(" "))
                return true
            }

            override fun tabComplete(player: Player, args: Array<out String>): List<String> {
                val request = applyRequests[player.uniqueId] ?: return emptyList()
                if (args.size == 1) {
                    val prefix = args[0]
                    val variants = mutableListOf<String>()
                    if ("cancel".startsWith(prefix, ignoreCase = true)) {
                        variants += "cancel"
                    }
                    variants += request.suggestions(prefix)
                    return variants.distinct()
                }
                return emptyList()
            }

            override fun cancel(player: Player) {
                val request = applyRequests.remove(player.uniqueId) ?: return
                hooker.tickScheduler.cancel(request.timeoutTaskId)
                promptService.clearPrompt(player)
            }
        })
    }

    fun handleCommandAlias(player: Player, rawMessage: String): Boolean {
        if (!enabled) return false
        val tokens = rawMessage.trim().removePrefix("/").split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (tokens.size < 2) return false
        if (tokens[0].lowercase() !in ROOT_ALIASES) return false
        if (!tokens[1].equals("edit", ignoreCase = true) && !tokens[1].equals("e", ignoreCase = true)) return false

        openEditor(player)
        return true
    }

    fun handleRuntimeReset(player: Player) {
        cancelApplySilently(player)
        sessions.remove(player.uniqueId)
    }

    private fun openEditor(player: Player) {
        cancelApplySilently(player)
        sessions.remove(player.uniqueId)

        val plotPlayer = PlotPlayer.from(player)
        val plot = plotPlayer.currentPlot
        if (plot == null) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_EDIT_NOT_ON_PLOT)
            return
        }
        if (!canAccessPlot(player, plot)) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_EDIT_NOT_OWNER)
            return
        }
        val plotArea = plot.area
        if (plotArea == null) {
            hooker.messageManager.sendChat(player, MessageKey.PLOT_EDIT_NOT_ON_PLOT)
            return
        }

        val session = PlotEditorSession(
            playerId = player.uniqueId,
            plotReference = PlotReference(plotArea, plot.id),
            view = PlotEditorView.Main(page = 0, filter = PlotFlagFilter.ALL)
        )
        sessions[player.uniqueId] = session
        openMainMenu(player, session, 0, PlotFlagFilter.ALL)
    }

    private fun openMainMenu(player: Player, session: PlotEditorSession, requestedPage: Int, filter: PlotFlagFilter) {
        val plot = resolvePlotForView(player, session) ?: return
        val explicitGroups = enabledFlagsInOrder
            .map { it.definition.groupKey }
            .distinct()
            .filterTo(mutableSetOf()) { groupKey -> hasExplicitFlag(plot, groupKey) }
        val filteredEntries = enabledFlagsInOrder.filter { entry ->
            when (filter) {
                PlotFlagFilter.ALL -> true
                PlotFlagFilter.ADDED -> entry.definition.groupKey in explicitGroups
                PlotFlagFilter.MISSING -> entry.definition.groupKey !in explicitGroups
            }
        }
        val totalPages = maxOf(1, (filteredEntries.size + MAIN_WORK_SLOTS.size - 1) / MAIN_WORK_SLOTS.size)
        val page = requestedPage.coerceIn(0, totalPages - 1)
        val pageEntries = filteredEntries.drop(page * MAIN_WORK_SLOTS.size).take(MAIN_WORK_SLOTS.size)
        session.view = PlotEditorView.Main(page, filter)
        session.renderedMainEntries = pageEntries.map { it.definition.key }
        session.renderedMainSlots = pageEntries.mapIndexed { index, entry ->
            entry.definition.key to MAIN_WORK_SLOTS[index]
        }.toMap()
        session.renderedMainTotalPages = totalPages

        val menu = MenuUiSupport.buildMenu(
            plugin = hooker.plugin,
            parser = parser,
            title = "<!i>▍ Настройка флагов [${page + 1}/$totalPages]",
            rows = MenuRows.SIX,
            menuTopRange = 0 until 54,
            interactiveTopSlots = MAIN_INTERACTIVE_SLOTS,
            allowPlayerInventoryClicks = false,
            onClose = { closeEvent ->
                if (closeEvent.player.uniqueId in menuTransitions) return@buildMenu
                if (sessions[closeEvent.player.uniqueId] === session) {
                    cancelApplySilently(closeEvent.player)
                    sessions.remove(closeEvent.player.uniqueId)
                }
            }
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        MAIN_BLACK_SLOTS.forEach { menu.setButton(it, black) }
        MAIN_GRAY_SLOTS.forEach { menu.setButton(it, gray) }
        MAIN_WORK_SLOTS.forEach { menu.setButton(it, emptyButton) }

        if (page > 0) {
            menu.setButton(48, buttonFactory.backButton {
                openMainMenu(player, session, page - 1, filter)
            })
        }
        if (page + 1 < totalPages) {
            menu.setButton(50, buttonFactory.forwardButton {
                openMainMenu(player, session, page + 1, filter)
            })
        }

        menu.setButton(49, buildFilterButton(filter) { newFilter ->
            openMainMenu(player, session, 0, newFilter)
        })

        pageEntries.forEachIndexed { index, entry ->
            val slot = MAIN_WORK_SLOTS[index]
            menu.setButton(slot, buildMainFlagButton(player, session, plot, entry, slot))
        }

        markTransition(player.uniqueId)
        menu.open(player)
    }

    private fun openMainMenuSnapshot(player: Player, session: PlotEditorSession) {
        val mainView = session.view as? PlotEditorView.Main ?: run {
            openCurrentView(player, session)
            return
        }
        val plot = resolvePlotForView(player, session) ?: return
        val pageEntries = session.renderedMainEntries.mapNotNull { resolvedFlagsByKey[it] }
        if (pageEntries.isEmpty()) {
            openMainMenu(player, session, mainView.page, mainView.filter)
            return
        }

        val menu = MenuUiSupport.buildMenu(
            plugin = hooker.plugin,
            parser = parser,
            title = "<!i>▍ Настройка флагов [${mainView.page + 1}/${session.renderedMainTotalPages}]",
            rows = MenuRows.SIX,
            menuTopRange = 0 until 54,
            interactiveTopSlots = MAIN_INTERACTIVE_SLOTS,
            allowPlayerInventoryClicks = false,
            onClose = { closeEvent ->
                if (closeEvent.player.uniqueId in menuTransitions) return@buildMenu
                if (sessions[closeEvent.player.uniqueId] === session) {
                    cancelApplySilently(closeEvent.player)
                    sessions.remove(closeEvent.player.uniqueId)
                }
            }
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        MAIN_BLACK_SLOTS.forEach { menu.setButton(it, black) }
        MAIN_GRAY_SLOTS.forEach { menu.setButton(it, gray) }
        MAIN_WORK_SLOTS.forEach { menu.setButton(it, emptyButton) }

        if (mainView.page > 0) {
            menu.setButton(48, buttonFactory.backButton {
                openMainMenu(player, session, mainView.page - 1, mainView.filter)
            })
        }
        if (mainView.page + 1 < session.renderedMainTotalPages) {
            menu.setButton(50, buttonFactory.forwardButton {
                openMainMenu(player, session, mainView.page + 1, mainView.filter)
            })
        }
        menu.setButton(49, buildFilterButton(mainView.filter) { newFilter ->
            openMainMenu(player, session, 0, newFilter)
        })

        pageEntries.forEachIndexed { index, entry ->
            val slot = MAIN_WORK_SLOTS.getOrNull(index) ?: return@forEachIndexed
            menu.setButton(slot, buildMainFlagButton(player, session, plot, entry, slot))
        }

        markTransition(player.uniqueId)
        menu.open(player)
    }

    private fun openCollectionMenu(
        player: Player,
        session: PlotEditorSession,
        entry: ResolvedPlotFlagDefinition,
        requestedPage: Int,
        returnMainPage: Int,
        returnFilter: PlotFlagFilter
    ) {
        val plot = resolvePlotForView(player, session) ?: return
        val values = currentCollectionValues(plot, entry)
        val totalPages = maxOf(1, (values.size + COLLECTION_WORK_SLOTS.size - 1) / COLLECTION_WORK_SLOTS.size)
        val page = requestedPage.coerceIn(0, totalPages - 1)
        val pageEntries = values.drop(page * COLLECTION_WORK_SLOTS.size).take(COLLECTION_WORK_SLOTS.size)
        session.view = PlotEditorView.Collection(entry.definition.key, page, returnMainPage, returnFilter)

        val addByCommandSlot = if (entry.definition.groupKey == "blocked-cmds") 39 else 38
        val addByMenuSlot = if (entry.definition.groupKey == "blocked-cmds") null else 40
        val clearSlot = if (entry.definition.groupKey == "blocked-cmds") 41 else 42
        val interactiveSlots = mutableSetOf(18, 26, addByCommandSlot, clearSlot).apply {
            addByMenuSlot?.let(::add)
            addAll(COLLECTION_WORK_SLOTS)
        }
        val menu = MenuUiSupport.buildMenu(
            plugin = hooker.plugin,
            parser = parser,
            title = collectionMenuTitle(entry.definition.title, page, totalPages),
            rows = MenuRows.FIVE,
            menuTopRange = 0 until 45,
            interactiveTopSlots = interactiveSlots,
            allowPlayerInventoryClicks = false,
            onClose = { closeEvent ->
                if (closeEvent.player.uniqueId in menuTransitions) return@buildMenu
                if (sessions[closeEvent.player.uniqueId] === session) {
                    cancelApplySilently(closeEvent.player)
                    sessions.remove(closeEvent.player.uniqueId)
                }
            }
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        COLLECTION_BLACK_SLOTS.forEach { menu.setButton(it, black) }
        COLLECTION_GRAY_SLOTS.forEach { menu.setButton(it, gray) }
        COLLECTION_WORK_SLOTS.forEach { menu.setButton(it, emptyButton) }

        menu.setButton(18, buttonFactory.backButton {
            if (page > 0) {
                openCollectionMenu(player, session, entry, page - 1, returnMainPage, returnFilter)
            } else {
                openMainMenu(player, session, returnMainPage, returnFilter)
            }
        })
        if (page + 1 < totalPages) {
            menu.setButton(26, buttonFactory.forwardButton {
                openCollectionMenu(player, session, entry, page + 1, returnMainPage, returnFilter)
            })
        }

        menu.setButton(addByCommandSlot, buttonFactory.actionButton(
            material = Material.LIME_DYE,
            name = collectionAddByCommandTitle(entry),
            lore = emptyList()
        ) {
            beginCollectionApply(player, session, entry, page, returnMainPage, returnFilter)
        })

        if (addByMenuSlot != null && entry.definition.collectionAddMode == PlotFlagCollectionAddMode.COMMAND_AND_MENU) {
            menu.setButton(addByMenuSlot, buttonFactory.actionButton(
                material = Material.MAGENTA_DYE,
                name = "<!i><#FF00FF>₪ Добавить блок <#FF66FF>[Меню]",
                lore = emptyList()
            ) {
                openBlockPickerMenu(player, session, entry, 0, page, returnMainPage, returnFilter)
            })
        } else if (addByMenuSlot != null) {
            menu.setButton(addByMenuSlot, gray)
        }

        menu.setButton(clearSlot, buttonFactory.actionButton(
            material = Material.RED_DYE,
            name = "<!i><#FF1500>⚠ Удалить всё",
            lore = emptyList()
        ) {
            mutatePlotFlag(
                player = player,
                session = session,
                groupKey = entry.definition.groupKey
            ) { livePlot ->
                livePlot.removeFlag(entry.flagClass)
            }
            openCollectionMenu(player, session, entry, 0, returnMainPage, returnFilter)
        })

        pageEntries.forEachIndexed { index, value ->
            val slot = COLLECTION_WORK_SLOTS[index]
            menu.setButton(slot, buildCollectionEntryButton(entry, value) removeAction@{
                val livePlot = resolvePlotForView(player, session) ?: return@removeAction
                val currentValues = currentCollectionValues(livePlot, entry)
                val nextValues = currentValues.filterNot { it.equals(value, ignoreCase = true) }
                mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
                    if (nextValues.isEmpty()) {
                        livePlot.removeFlag(entry.flagClass)
                    } else {
                        livePlot.setFlag(entry.flagClass, nextValues.joinToString(","))
                    }
                }
                val targetPage = page.coerceAtMost(maxOf(0, (nextValues.size - 1) / COLLECTION_WORK_SLOTS.size))
                openCollectionMenu(player, session, entry, targetPage, returnMainPage, returnFilter)
            })
        }

        markTransition(player.uniqueId)
        menu.open(player)
    }

    private fun openBlockPickerMenu(
        player: Player,
        session: PlotEditorSession,
        entry: ResolvedPlotFlagDefinition,
        requestedPage: Int,
        returnCollectionPage: Int,
        returnMainPage: Int,
        returnFilter: PlotFlagFilter
    ) {
        val plot = resolvePlotForView(player, session) ?: return
        val selected = pendingBlockSelection(session, plot, entry).toSet()
        val totalPages = maxOf(1, (blockOptions.size + BLOCK_PICKER_WORK_SLOTS.size - 1) / BLOCK_PICKER_WORK_SLOTS.size)
        val page = requestedPage.coerceIn(0, totalPages - 1)
        val pageEntries = blockOptions.drop(page * BLOCK_PICKER_WORK_SLOTS.size).take(BLOCK_PICKER_WORK_SLOTS.size)
        session.view = PlotEditorView.BlockPicker(entry.definition.key, page, returnCollectionPage, returnMainPage, returnFilter)

        val interactiveSlots = mutableSetOf(18, 26).apply {
            if (totalPages > 1) {
                add(40)
            }
            addAll(BLOCK_PICKER_WORK_SLOTS)
        }
        val menu = MenuUiSupport.buildMenu(
            plugin = hooker.plugin,
            parser = parser,
            title = collectionMenuTitle("${entry.definition.title} → Блоки", page, totalPages),
            rows = MenuRows.FIVE,
            menuTopRange = 0 until 45,
            interactiveTopSlots = interactiveSlots,
            allowPlayerInventoryClicks = false,
            onClose = { closeEvent ->
                if (closeEvent.player.uniqueId in menuTransitions) return@buildMenu
                finalizePendingBlockSelection(closeEvent.player, session, entry)
                if (sessions[closeEvent.player.uniqueId] === session) {
                    cancelApplySilently(closeEvent.player)
                    sessions.remove(closeEvent.player.uniqueId)
                }
            }
        )

        val black = buttonFactory.blackFillerButton()
        val gray = buttonFactory.grayFillerButton()
        BLOCK_PICKER_BLACK_SLOTS.forEach { menu.setButton(it, black) }
        BLOCK_PICKER_GRAY_SLOTS.forEach { menu.setButton(it, gray) }

        menu.setButton(18, buttonFactory.backButton {
            if (page > 0) {
                openBlockPickerMenu(player, session, entry, page - 1, returnCollectionPage, returnMainPage, returnFilter)
            } else {
                finalizePendingBlockSelection(player, session, entry)
                openCollectionMenu(player, session, entry, returnCollectionPage, returnMainPage, returnFilter)
            }
        })

        if (page + 1 < totalPages) {
            menu.setButton(26, buttonFactory.forwardButton {
                openBlockPickerMenu(player, session, entry, page + 1, returnCollectionPage, returnMainPage, returnFilter)
            })
        }

        if (totalPages > 1) {
            menu.setButton(40, buttonFactory.actionButton(
                material = Material.BOOK,
                name = "<!i><#C7A300>№ <#FFD700>Страница: <#FFF3E0>${page + 1}",
                lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")
            ) {
                requestSignInput(
                    player = player,
                    templateLines = arrayOf("", "↑ Страница ↑", "", ""),
                    reopen = { openBlockPickerMenu(player, session, entry, page, returnCollectionPage, returnMainPage, returnFilter) }
                ) { input ->
                    val targetPage = (input?.toIntOrNull()?.coerceIn(1, totalPages) ?: (page + 1)) - 1
                    openBlockPickerMenu(player, session, entry, targetPage, returnCollectionPage, returnMainPage, returnFilter)
                }
            })
        } else {
            menu.setButton(40, gray)
        }

        pageEntries.forEachIndexed { index, option ->
            val slot = BLOCK_PICKER_WORK_SLOTS[index]
            val selectedNow = option.key.asString() in selected || option.key.key in selected
            menu.setButton(slot, buttonFactory.restrictionBlockTypeEntryButton(
                displayName = option.displayName,
                modelId = option.modelId,
                selected = selectedNow
            ) { clickEvent ->
                if (!canEditFlag(player, entry.requiredRoleKey)) return@restrictionBlockTypeEntryButton
                val currentValues = pendingBlockSelection(session, plot, entry).toMutableList()
                val value = option.key.key
                if (currentValues.none { it.equals(value, ignoreCase = true) }) {
                    currentValues += value
                    session.pendingBlockSelections[entry.definition.key] = currentValues
                    clickEvent.menu.setButton(slot, buttonFactory.restrictionBlockTypeEntryButton(
                        displayName = option.displayName,
                        modelId = option.modelId,
                        selected = true
                    ) { })
                }
            })
        }

        markTransition(player.uniqueId)
        menu.open(player)
    }

    private fun buildMainFlagButton(
        player: Player,
        session: PlotEditorSession,
        plot: Plot,
        entry: ResolvedPlotFlagDefinition,
        slot: Int
    ): Button {
        val editable = canEditFlag(player, entry.requiredRoleKey)

        return when (entry.definition.kind) {
            PlotFlagEntryKind.PRESET -> {
                val explicitText = explicitFlagValueText(plot, entry)
                val selectedIndex = entry.definition.presetOptions.indexOfFirst {
                    presetValueMatches(entry, explicitText, it.value)
                }.takeIf { it >= 0 } ?: 0
                val selected = entry.definition.presetOptions[selectedIndex]
                val active = !selected.value.equals("none", ignoreCase = true)
                val lore = buildList {
                    if (editable) {
                        add(changeHintLine())
                    } else {
                        add(roleAccessLine(entry))
                    }
                    add("")
                    addAll(buildPresetOptionLines(entry, selectedIndex))
                    add("")
                    addAll(buildDescriptionSection(entry.definition.description))
                }
                buttonFactory.actionButton(
                    material = Material.STRUCTURE_VOID,
                    name = if (active) {
                        "<!i><#C7A300>\u25CE <#FFD700>${entry.definition.title}"
                    } else {
                        "<!i><#C7A300>\u2B58 <#FFD700>${entry.definition.title}"
                    },
                    lore = lore,
                    itemModifier = {
                        applyModel(entry.definition.itemModel)
                        if (active) {
                            glint(true)
                        }
                        this
                    }
                ) { clickEvent ->
                    if (!editable) return@actionButton
                    val newIndex = when {
                        clickEvent.isLeft || clickEvent.isShiftLeft -> (selectedIndex + 1) % entry.definition.presetOptions.size
                        clickEvent.isRight || clickEvent.isShiftRight -> (selectedIndex - 1 + entry.definition.presetOptions.size) % entry.definition.presetOptions.size
                        else -> return@actionButton
                    }
                    val nextOption = entry.definition.presetOptions[newIndex]
                    val changed = mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
                        if (nextOption.value.equals("none", ignoreCase = true)) {
                            livePlot.removeFlag(entry.flagClass)
                        } else {
                            livePlot.setFlag(entry.flagClass, nextOption.value)
                        }
                    }
                    if (changed) {
                        val livePlot = resolvePlotForView(player, session) ?: return@actionButton
                        clickEvent.menu.setButton(slot, buildMainFlagButton(player, session, livePlot, entry, slot))
                    }
                }
            }

            PlotFlagEntryKind.TEXT,
            PlotFlagEntryKind.TITLE_PART -> {
                val preview = resolveTextPreview(plot, entry)
                val active = !preview.isNullOrBlank()
                val lore = buildList {
                    preview?.let { add(textPreviewLine(it)) }
                    if (!editable) {
                        add(roleAccessLine(entry, if (active) "\u27A5" else "\u258D"))
                        add("")
                    } else {
                        if (active) {
                            add("")
                        }
                        addAll(buildTextApplyHints())
                        add("")
                        addAll(buildTextApplyCommands("<\u0442\u0435\u043A\u0441\u0442>"))
                        add("")
                    }
                    addAll(buildDescriptionSection(entry.definition.description))
                }
                buttonFactory.actionButton(
                    material = Material.STRUCTURE_VOID,
                    name = when {
                        active -> "<!i><#C7A300>\u25CE <#FFD700>${entry.definition.title}"
                        entry.definition.kind == PlotFlagEntryKind.TITLE_PART -> "<!i><#C7A300>\u2B58 <#FFD700>${entry.definition.title}: <#FF1500>\u0421\u0442\u0430\u043D\u0434\u0430\u0440\u0442\u043D\u044B\u0439"
                        else -> "<!i><#C7A300>\u2B58 <#FFD700>${entry.definition.title}: <#FF1500>\u041D\u0435 \u0437\u0430\u0434\u0430\u043D\u043E"
                    },
                    lore = lore,
                    itemModifier = {
                        applyModel(entry.definition.itemModel)
                        if (active) {
                            glint(true)
                        }
                        this
                    }
                ) { clickEvent ->
                    if (!editable) return@actionButton
                    when {
                        clickEvent.isLeft || clickEvent.isShiftLeft -> beginTextApply(player, session, entry)
                        clickEvent.isRight || clickEvent.isShiftRight -> {
                            val changed = mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
                                removeTextFlag(livePlot, entry)
                            }
                            if (changed) {
                                reopenAfterMutation(player, session)
                            }
                        }
                    }
                }
            }

            PlotFlagEntryKind.NUMBER -> {
                val value = explicitFlagValueText(plot, entry)
                val active = value?.toIntOrNull()?.let { it >= 0 } == true
                val lore = buildList {
                    if (editable) {
                        add(changeHintLine())
                    } else {
                        add(roleAccessLine(entry))
                    }
                    add("")
                    addAll(buildDescriptionSection(entry.definition.description))
                }
                buttonFactory.actionButton(
                    material = Material.STRUCTURE_VOID,
                    name = if (active) {
                        "<!i><#C7A300>\u25CE <#FFD700>${entry.definition.title}: <#00FF40>$value"
                    } else {
                        "<!i><#C7A300>\u2B58 <#FFD700>${entry.definition.title}: <#FF1500>\u041D\u0435 \u0437\u0430\u0434\u0430\u043D\u043E"
                    },
                    lore = lore,
                    itemModifier = {
                        applyModel(entry.definition.itemModel)
                        if (active) {
                            glint(true)
                        }
                        this
                    }
                ) {
                    if (!editable) return@actionButton
                    when {
                        it.isLeft || it.isShiftLeft -> beginNumberInput(player, session, entry)
                        it.isRight || it.isShiftRight -> {
                            val changed = mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
                                livePlot.removeFlag(entry.flagClass)
                            }
                            if (changed) {
                                reopenAfterMutation(player, session)
                            }
                        }
                    }
                }
            }

            PlotFlagEntryKind.TIMED_NUMBER -> {
                val value = explicitFlagValueText(plot, entry)
                val active = !value.isNullOrBlank()
                val lore = buildList {
                    if (editable) {
                        add(changeHintLine())
                        add("")
                        addAll(buildTextApplyCommands("<\u0438\u043D\u0442\u0435\u0440\u0432\u0430\u043B> <\u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435>"))
                    } else {
                        add(roleAccessLine(entry))
                    }
                    add("")
                    addAll(buildDescriptionSection(entry.definition.description))
                }
                buttonFactory.actionButton(
                    material = Material.STRUCTURE_VOID,
                    name = if (active) {
                        "<!i><#C7A300>\u25CE <#FFD700>${entry.definition.title}: <#00FF40>$value"
                    } else {
                        "<!i><#C7A300>\u2B58 <#FFD700>${entry.definition.title}: <#FF1500>\u041D\u0435 \u0437\u0430\u0434\u0430\u043D\u043E"
                    },
                    lore = lore,
                    itemModifier = {
                        applyModel(entry.definition.itemModel)
                        if (active) {
                            glint(true)
                        }
                        this
                    }
                ) {
                    if (!editable) return@actionButton
                    when {
                        it.isLeft || it.isShiftLeft -> beginTimedApply(player, session, entry)
                        it.isRight || it.isShiftRight -> {
                            val changed = mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
                                livePlot.removeFlag(entry.flagClass)
                            }
                            if (changed) {
                                reopenAfterMutation(player, session)
                            }
                        }
                    }
                }
            }

            PlotFlagEntryKind.COLLECTION -> {
                val values = currentCollectionValues(plot, entry)
                val active = values.isNotEmpty()
                val lore = buildList {
                    if (editable) {
                        add(changeHintLine())
                    } else {
                        add(roleAccessLine(entry))
                    }
                    add("")
                    if (active) {
                        addAll(buildCollectionValuesSection(entry, values))
                        add("")
                    }
                    addAll(buildDescriptionSection(entry.definition.description))
                }
                buttonFactory.actionButton(
                    material = Material.STRUCTURE_VOID,
                    name = if (active) {
                        "<!i><#C7A300>\u25CE <#FFD700>${entry.definition.title}: <#00FF40>${values.size}"
                    } else {
                        "<!i><#C7A300>\u2B58 <#FFD700>${entry.definition.title}"
                    },
                    lore = lore,
                    itemModifier = {
                        applyModel(entry.definition.itemModel)
                        if (active) {
                            glint(true)
                        }
                        this
                    }
                ) {
                    if (!editable) return@actionButton
                    val mainView = session.view as? PlotEditorView.Main ?: PlotEditorView.Main(0, PlotFlagFilter.ALL)
                    openCollectionMenu(player, session, entry, 0, mainView.page, mainView.filter)
                }
            }
        }
    }
    private fun buildCollectionEntryButton(
        entry: ResolvedPlotFlagDefinition,
        value: String,
        action: () -> Unit
    ): Button {
        val isBlockCollection = entry.definition.collectionAddMode == PlotFlagCollectionAddMode.COMMAND_AND_MENU
        return buttonFactory.actionButton(
            material = Material.STRUCTURE_VOID,
            name = "<!i><#C7A300>◎ <#FFD700>${displayCollectionValue(entry, value)}",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить"),
            itemModifier = {
                if (isBlockCollection) {
                    applyModel(collectionItemModelId(value))
                } else {
                    applyModel(entry.definition.itemModel)
                }
                glint(true)
                this
            },
            action = { action() }
        )
    }

    private fun buildFilterButton(current: PlotFlagFilter, onChange: (PlotFlagFilter) -> Unit): Button {
        val options = listOf(
            MenuButtonFactory.ListButtonOption(PlotFlagFilter.ALL, "Все"),
            MenuButtonFactory.ListButtonOption(PlotFlagFilter.ADDED, "Добавленные"),
            MenuButtonFactory.ListButtonOption(PlotFlagFilter.MISSING, "Отсутствующие")
        )
        val selectedIndex = options.indexOfFirst { it.value == current }.coerceAtLeast(0)
        return buttonFactory.listButton(
            material = Material.CLOCK,
            options = options,
            selectedIndex = selectedIndex,
            titleBuilder = { _, _ -> "<!i><#C7A300>⚡ <#FFD700>Фильтр" },
            beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", ""),
            afterOptionsLore = listOf("")
        ) { _, newIndex ->
            onChange(options[newIndex].value)
        }
    }
    private fun beginTextApply(player: Player, session: PlotEditorSession, entry: ResolvedPlotFlagDefinition) {
        beginApplyRequest(
            player = player,
            promptKind = EditorApplyKind.LORE_TEXT,
            usageMessageKey = MessageKey.EDIT_APPLY_USAGE_TEXT,
            suggestions = { emptyList() },
            onReopen = { reopenCurrentSession(it.uniqueId) },
            onApply = { applyPlayer, rawInput ->
                val changed = mutatePlotFlag(applyPlayer, session, entry.definition.groupKey) { livePlot ->
                    applyTextFlag(livePlot, entry, rawInput)
                }
                if (changed) {
                    reopenAfterMutation(applyPlayer, session)
                } else {
                    reopenCurrentSession(applyPlayer.uniqueId)
                }
            }
        )
    }

    private fun beginTimedApply(player: Player, session: PlotEditorSession, entry: ResolvedPlotFlagDefinition) {
        beginApplyRequest(
            player = player,
            promptKind = EditorApplyKind.AMOUNT,
            usageMessageKey = MessageKey.EDIT_APPLY_USAGE_AMOUNT,
            suggestions = { emptyList() },
            onReopen = { reopenCurrentSession(it.uniqueId) },
            onApply = { applyPlayer, rawInput ->
                val changed = mutatePlotFlag(applyPlayer, session, entry.definition.groupKey) { livePlot ->
                    livePlot.setFlag(entry.flagClass, rawInput.trim())
                }
                if (!changed) {
                    hooker.messageManager.sendChat(applyPlayer, MessageKey.EDIT_APPLY_INVALID_VALUE)
                }
                if (changed) {
                    reopenAfterMutation(applyPlayer, session)
                } else {
                    reopenCurrentSession(applyPlayer.uniqueId)
                }
            }
        )
    }

    private fun beginCollectionApply(
        player: Player,
        session: PlotEditorSession,
        entry: ResolvedPlotFlagDefinition,
        collectionPage: Int,
        returnMainPage: Int,
        returnFilter: PlotFlagFilter
    ) {
        val usageKey = if (entry.definition.groupKey == "blocked-cmds") {
            MessageKey.EDIT_APPLY_USAGE_COMMAND
        } else {
            MessageKey.EDIT_APPLY_USAGE_ID
        }
        beginApplyRequest(
            player = player,
            promptKind = if (entry.definition.groupKey == "blocked-cmds") EditorApplyKind.COMMAND else EditorApplyKind.ITEM_ID,
            usageMessageKey = usageKey,
            suggestions = { prefix ->
                if (entry.definition.groupKey == "blocked-cmds") {
                    emptyList()
                } else {
                    blockOptions.asSequence()
                        .map { it.key.key }
                        .filter { it.startsWith(prefix.lowercase()) }
                        .take(20)
                        .toList()
                }
            },
            onReopen = {
                openCollectionMenu(it, session, entry, collectionPage, returnMainPage, returnFilter)
            },
            onApply = { applyPlayer, rawInput ->
                val changed = mutatePlotFlag(applyPlayer, session, entry.definition.groupKey) { livePlot ->
                    val currentValues = currentCollectionValues(livePlot, entry)
                    val additions = normalizeCollectionAdditions(entry, rawInput)
                    if (additions.isEmpty()) {
                        false
                    } else {
                        livePlot.setFlag(entry.flagClass, (currentValues + additions).distinct().joinToString(","))
                    }
                }
                if (!changed) {
                    hooker.messageManager.sendChat(applyPlayer, MessageKey.EDIT_APPLY_INVALID_VALUE)
                }
                openCollectionMenu(applyPlayer, session, entry, collectionPage, returnMainPage, returnFilter)
            }
        )
    }

    private fun beginNumberInput(player: Player, session: PlotEditorSession, entry: ResolvedPlotFlagDefinition) {
        requestSignInput(
            player = player,
            templateLines = arrayOf("", "↑ Значение ↑", "", ""),
            reopen = { reopenCurrentSession(player.uniqueId) }
        ) { input ->
            var changed = false
            val parsed = input?.toIntOrNull()
            if (parsed != null) {
                changed = mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
                    if (parsed < 0) {
                        livePlot.removeFlag(entry.flagClass)
                    } else {
                        livePlot.setFlag(entry.flagClass, parsed.coerceIn(0, 2048).toString())
                    }
                }
            }
            if (changed) {
                reopenAfterMutation(player, session)
            } else {
                reopenCurrentSession(player.uniqueId)
            }
        }
    }

    private fun beginApplyRequest(
        player: Player,
        promptKind: EditorApplyKind,
        usageMessageKey: MessageKey,
        suggestions: (String) -> List<String>,
        onReopen: (Player) -> Unit,
        onApply: (Player, String) -> Unit
    ) {
        cancelApplySilently(player)
        val timeoutTaskId = hooker.tickScheduler.runLater(APPLY_TIMEOUT_TICKS) {
            val online = Bukkit.getPlayer(player.uniqueId) ?: return@runLater
            val request = applyRequests.remove(online.uniqueId) ?: return@runLater
            promptService.clearPrompt(online)
            request.onReopen(online)
        }
        applyRequests[player.uniqueId] = ApplyRequest(
            timeoutTaskId = timeoutTaskId,
            usageMessageKey = usageMessageKey,
            suggestions = suggestions,
            onApply = onApply,
            onReopen = onReopen
        )
        promptService.showPrompt(player, promptKind, APPLY_TIMEOUT_SECONDS)
        markTransition(player.uniqueId)
        player.closeInventory()
    }

    private fun clearApplyRequest(player: Player, request: ApplyRequest) {
        applyRequests.remove(player.uniqueId)
        hooker.tickScheduler.cancel(request.timeoutTaskId)
        promptService.clearPrompt(player)
    }

    private fun cancelApplySilently(player: Player) {
        val request = applyRequests.remove(player.uniqueId) ?: return
        hooker.tickScheduler.cancel(request.timeoutTaskId)
        promptService.clearPrompt(player)
    }

    private fun requestSignInput(
        player: Player,
        templateLines: Array<String>,
        reopen: () -> Unit,
        onSubmit: (String?) -> Unit
    ) {
        markTransition(player.uniqueId)
        player.closeInventory()
        signInputService.open(
            player = player,
            templateLines = templateLines,
            onSubmit = { submitPlayer, input ->
                hooker.tickScheduler.runNow {
                    if (!submitPlayer.isOnline) return@runNow
                    onSubmit(input)
                }
            },
            onLeave = { leavePlayer ->
                hooker.tickScheduler.runNow {
                    if (!leavePlayer.isOnline) return@runNow
                    reopen()
                }
            }
        )
    }

    private fun applyTextFlag(plot: Plot, entry: ResolvedPlotFlagDefinition, rawInput: String): Boolean {
        val normalized = normalizeTextInput(rawInput)
        if (entry.definition.kind == PlotFlagEntryKind.TITLE_PART) {
            val currentTitle = resolvedFlagValue(plot, entry.flagClass) as? PlotTitle
            val currentMain = currentTitle?.title().orEmpty()
            val currentSub = currentTitle?.subtitle().orEmpty()
            val nextMain = if (entry.definition.titlePart == PlotTitlePart.TITLE) normalized else currentMain
            val nextSub = if (entry.definition.titlePart == PlotTitlePart.SUBTITLE) normalized else currentSub
            return if (nextMain.isEmpty() && nextSub.isEmpty()) {
                plot.removeFlag(entry.flagClass)
            } else {
                plot.setFlag(entry.flagClass, "\"${escapeQuotedFlagText(nextMain)}\" \"${escapeQuotedFlagText(nextSub)}\"")
            }
        }
        return if (normalized.isEmpty()) {
            false
        } else {
            plot.setFlag(entry.flagClass, normalized)
        }
    }

    private fun removeTextFlag(plot: Plot, entry: ResolvedPlotFlagDefinition): Boolean {
        if (entry.definition.kind != PlotFlagEntryKind.TITLE_PART) {
            return plot.removeFlag(entry.flagClass)
        }
        val currentTitle = resolvedFlagValue(plot, entry.flagClass) as? PlotTitle
        val currentMain = currentTitle?.title().orEmpty()
        val currentSub = currentTitle?.subtitle().orEmpty()
        val nextMain = if (entry.definition.titlePart == PlotTitlePart.TITLE) "" else currentMain
        val nextSub = if (entry.definition.titlePart == PlotTitlePart.SUBTITLE) "" else currentSub
        return if (nextMain.isEmpty() && nextSub.isEmpty()) {
            plot.removeFlag(entry.flagClass)
        } else {
            plot.setFlag(entry.flagClass, "\"${escapeQuotedFlagText(nextMain)}\" \"${escapeQuotedFlagText(nextSub)}\"")
        }
    }

    private fun resolveTextPreview(plot: Plot, entry: ResolvedPlotFlagDefinition): String? {
        if (entry.definition.kind == PlotFlagEntryKind.TITLE_PART) {
            val title = resolvedFlagValue(plot, entry.flagClass) as? PlotTitle ?: return null
            return when (entry.definition.titlePart) {
                PlotTitlePart.TITLE -> title.title()?.takeIf { it.isNotBlank() }
                PlotTitlePart.SUBTITLE -> title.subtitle()?.takeIf { it.isNotBlank() }
                null -> null
            }
        }
        return explicitFlagValueText(plot, entry)?.takeIf { it.isNotBlank() }
    }

    private fun normalizeCollectionAdditions(entry: ResolvedPlotFlagDefinition, rawInput: String): List<String> {
        return rawInput.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { token -> normalizeCollectionValue(entry, token) }
    }

    private fun currentCollectionValues(plot: Plot, entry: ResolvedPlotFlagDefinition): List<String> {
        val raw = explicitFlagValueText(plot, entry) ?: return emptyList()
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { normalizeCollectionValue(entry, it) }
    }

    private fun pendingBlockSelection(
        session: PlotEditorSession,
        plot: Plot,
        entry: ResolvedPlotFlagDefinition
    ): List<String> {
        return session.pendingBlockSelections[entry.definition.key]?.toList()
            ?: currentCollectionValues(plot, entry)
    }

    private fun finalizePendingBlockSelection(
        player: Player,
        session: PlotEditorSession,
        entry: ResolvedPlotFlagDefinition
    ) {
        val pendingValues = session.pendingBlockSelections.remove(entry.definition.key) ?: return
        if (!canEditFlag(player, entry.requiredRoleKey)) {
            return
        }
        mutatePlotFlag(player, session, entry.definition.groupKey) { livePlot ->
            val currentValues = currentCollectionValues(livePlot, entry)
            if (currentValues == pendingValues) {
                return@mutatePlotFlag false
            }
            if (pendingValues.isEmpty()) {
                livePlot.removeFlag(entry.flagClass)
            } else {
                livePlot.setFlag(entry.flagClass, pendingValues.joinToString(","))
            }
        }
    }

    private fun normalizeCollectionValue(entry: ResolvedPlotFlagDefinition, token: String): String {
        return if (entry.definition.collectionAddMode == PlotFlagCollectionAddMode.COMMAND_AND_MENU) {
            token.substringAfter(':').lowercase()
        } else if (entry.definition.groupKey == "blocked-cmds") {
            token.removePrefix("/")
        } else {
            token.lowercase()
        }
    }

    private fun displayCollectionValue(entry: ResolvedPlotFlagDefinition, value: String): String {
        return if (entry.definition.collectionAddMode == PlotFlagCollectionAddMode.COMMAND_AND_MENU) {
            blockOptions.firstOrNull { it.key.key.equals(value, ignoreCase = true) }?.displayName ?: value
        } else {
            value
        }
    }

    private fun collectionItemModelId(value: String): String {
        return if (':' in value) value else "minecraft:${value}"
    }

    private fun explicitFlagValueText(plot: Plot, entry: ResolvedPlotFlagDefinition): String? {
        val explicit = plot.flags.firstOrNull { entry.flagClass.isInstance(it) } ?: return null
        return explicit.toString().trim().takeUnless { it.isEmpty() }
    }

    private fun hasExplicitFlag(plot: Plot, groupKey: String): Boolean {
        return enabledFlagsInOrder
            .filter { it.definition.groupKey.equals(groupKey, ignoreCase = true) }
            .any { entry -> plot.flags.any { entry.flagClass.isInstance(it) } }
    }

    private fun canAccessPlot(player: Player, plot: Plot): Boolean {
        return hasAdminAccess(player) || plot.isOwner(player.uniqueId)
    }

    private fun canEditFlag(player: Player, roleKey: String): Boolean {
        return hasAdminAccess(player) || hooker.permissionManager.hasAtLeastRole(player, roleKey)
    }

    private fun hasAdminAccess(player: Player): Boolean {
        return plotEditAdminAccessPermission.isNotBlank() && player.hasPermission(plotEditAdminAccessPermission)
    }

    private fun resolvePlotForView(player: Player, session: PlotEditorSession): Plot? {
        val plot = session.plotReference.resolve() ?: run {
            closeSession(player)
            return null
        }
        if (!plot.hasOwner() || !canAccessPlot(player, plot)) {
            closeSession(player)
            return null
        }
        return plot
    }

    private fun resolvePlotForMutation(player: Player, session: PlotEditorSession): Plot? {
        return resolvePlotForView(player, session)
    }

    private fun mutatePlotFlag(
        player: Player,
        session: PlotEditorSession,
        groupKey: String,
        action: (Plot) -> Boolean
    ): Boolean {
        val plot = resolvePlotForMutation(player, session) ?: return false
        val cooldownKey = cooldownKey(player.uniqueId, session.plotReference, groupKey)
        if (!acquireMutationCooldown(cooldownKey)) {
            return false
        }
        return action(plot)
    }

    private fun acquireMutationCooldown(key: String): Boolean {
        if (!mutationCooldowns.add(key)) {
            return false
        }
        hooker.tickScheduler.runLater(FLAG_MUTATION_COOLDOWN_TICKS) {
            mutationCooldowns.remove(key)
        }
        return true
    }

    private fun cooldownKey(playerId: UUID, reference: PlotReference, groupKey: String): String {
        return "$playerId:${System.identityHashCode(reference.area)}:${reference.id}:$groupKey"
    }

    private fun closeSession(player: Player) {
        sessions.remove(player.uniqueId)
        cancelApplySilently(player)
        markTransition(player.uniqueId)
        player.closeInventory()
    }

    private fun reopenCurrentSession(playerId: UUID) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val session = sessions[playerId] ?: return
        openCurrentView(player, session)
    }

    private fun openCurrentView(player: Player, session: PlotEditorSession) {
        when (val view = session.view) {
            is PlotEditorView.Main -> openMainMenu(player, session, view.page, view.filter)
            is PlotEditorView.Collection -> {
                val entry = resolvedFlagsByKey[view.entryKey] ?: return
                openCollectionMenu(player, session, entry, view.page, view.returnPage, view.returnFilter)
            }
            is PlotEditorView.BlockPicker -> {
                val entry = resolvedFlagsByKey[view.entryKey] ?: return
                openBlockPickerMenu(player, session, entry, view.page, view.returnCollectionPage, view.returnMainPage, view.returnFilter)
            }
        }
    }

    private fun reopenAfterMutation(player: Player, session: PlotEditorSession) {
        val mainView = session.view as? PlotEditorView.Main
        if (mainView != null && mainView.filter == PlotFlagFilter.MISSING) {
            openMainMenuSnapshot(player, session)
            return
        }
        openCurrentView(player, session)
    }

    private fun enabledFlagKeys(): List<String> {
        val configured = hooker.configManager.config.getStringList(CONFIG_ENABLED_FLAGS)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        return configured.ifEmpty { PlotFlagCatalog.definitions.map { it.groupKey }.distinct() }
    }

    private fun resolveRoleRequirements(section: ConfigurationSection?): Map<String, String> {
        if (section == null) {
            return emptyMap()
        }
        val requirements = mutableMapOf<String, String>()
        hooker.permissionManager.orderedRoles().forEach { role ->
            section.getStringList(role.key).forEach { flagKey ->
                requirements.putIfAbsent(flagKey.trim().lowercase(), role.key)
            }
        }
        return requirements
    }

    private fun changeHintLine(): String = "<!i><#FFD700>\u041D\u0430\u0436\u043C\u0438\u0442\u0435, <#FFE68A>\u0447\u0442\u043E\u0431\u044B \u0438\u0437\u043C\u0435\u043D\u0438\u0442\u044C"

    private fun roleAccessLine(entry: ResolvedPlotFlagDefinition, marker: String = "\u258D"): String {
        val role = hooker.permissionManager.getRole(entry.requiredRoleKey)
        val roleText = role?.prefix?.takeUnless(String::isBlank)
            ?: role?.let { "<#FFF3E0>${it.display}" }
            ?: "<#FFF3E0>\u043A\u043E\u043D\u0444\u0438\u0433\u0430"
        return "<!i><#C7A300>$marker <#FFE68A>\u0414\u043E\u0441\u0442\u0443\u043F\u043D\u043E \u0441 $roleText"
    }

    private fun textPreviewLine(value: String): String = "<!i><#C7A300>\u258D ${previewLine(value)}"

    private fun buildTextApplyHints(): List<String> = listOf(
        "<!i><#FFD700>\u041B\u041A\u041C, <#FFE68A>\u0447\u0442\u043E\u0431\u044B \u0437\u0430\u0434\u0430\u0442\u044C",
        "<!i><#FFD700>\u041F\u041A\u041C, <#FFE68A>\u0447\u0442\u043E\u0431\u044B \u0441\u0431\u0440\u043E\u0441\u0438\u0442\u044C"
    )

    private fun buildTextApplyCommands(inputLabel: String): List<String> = listOf(
        "<!i><#FFD700>\u041F\u043E\u0441\u043B\u0435 \u043D\u0430\u0436\u0430\u0442\u0438\u044F:",
        "<!i><#C7A300> \u25CF <#FFF3E0>/apply $inputLabel <#C7A300>- <#FFE68A>\u0437\u0430\u0434\u0430\u0442\u044C",
        "<!i><#C7A300> \u25CF <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>\u043E\u0442\u043C\u0435\u043D\u0438\u0442\u044C"
    )

    private fun buildPresetOptionLines(
        entry: ResolvedPlotFlagDefinition,
        selectedIndex: Int
    ): List<String> = entry.definition.presetOptions.mapIndexed { index, option ->
        if (index == selectedIndex) {
            "<!i>  <#00FF40>\u00BB ${option.label}"
        } else {
            "<!i><b> </b><#C7A300>\u00BB ${option.label}"
        }
    }

    private fun buildCollectionValuesSection(
        entry: ResolvedPlotFlagDefinition,
        values: List<String>
    ): List<String> = buildList {
        add("<!i><#FFD700>${collectionValuesHeading(entry)}")
        values.take(6).forEach { value ->
            add("<!i><#C7A300> ● <#FFE68A>${displayCollectionValue(entry, value)}")
        }
        if (values.size > 6) {
            add("<!i><#C7A300> ● <#FFE68A>ещё ${values.size - 6}")
        }
    }

    private fun collectionValuesHeading(entry: ResolvedPlotFlagDefinition): String = when (entry.definition.groupKey) {
        "use" -> "Разрешено использовать:"
        "place" -> "Разрешено ставить:"
        "break" -> "Разрешено ломать:"
        "blocked-cmds" -> "Заблокированные:"
        else -> "Текущие значения:"
    }

    private fun collectionMenuTitle(title: String, page: Int, totalPages: Int): String {
        return if (totalPages > 1) {
            "<!i>▍ $title [${page + 1}/$totalPages]"
        } else {
            "<!i>▍ $title"
        }
    }

    private fun collectionAddByCommandTitle(entry: ResolvedPlotFlagDefinition): String {
        return if (entry.definition.groupKey == "blocked-cmds") {
            "<!i><#00FF40>₪ Добавить команду"
        } else {
            "<!i><#00FF40>₪ Добавить блок <#7BFF00>[Команда]"
        }
    }

    private fun buildDescriptionSection(description: String): List<String> = buildList {
        add("<!i><#FFD700>\u041D\u0430\u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435:")
        wrapDescriptionLines(description).forEachIndexed { index, line ->
            if (index == 0) {
                add("<!i><#C7A300> \u25CF <#FFE68A>$line ")
            } else {
                add("<!i><#FFE68A>  <b> </b>$line ")
            }
        }
        add("")
    }

    private fun wrapDescriptionLines(text: String, maxLineLength: Int = 34): List<String> {
        val words = WHITESPACE_REGEX.split(text.trim()).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return emptyList()
        }

        val lines = mutableListOf<String>()
        val current = StringBuilder()
        words.forEach { word ->
            val separatorLength = if (current.isEmpty()) 0 else 1
            if (current.length + separatorLength + word.length > maxLineLength && current.isNotEmpty()) {
                lines += current.toString()
                current.setLength(0)
            }
            if (current.isNotEmpty()) {
                current.append(' ')
            }
            current.append(word)
        }
        if (current.isNotEmpty()) {
            lines += current.toString()
        }
        return lines
    }
    private fun presetValueMatches(entry: ResolvedPlotFlagDefinition, explicitText: String?, optionValue: String): Boolean {
        if (optionValue.equals("none", ignoreCase = true)) {
            return explicitText == null
        }
        val normalizedExplicit = when (entry.definition.groupKey) {
            "music" -> explicitText?.removePrefix("minecraft:")
            else -> explicitText
        }?.lowercase()
        val normalizedOption = when (entry.definition.groupKey) {
            "music" -> optionValue.removePrefix("minecraft:")
            else -> optionValue
        }.lowercase()
        return normalizedExplicit == normalizedOption
    }

    private fun previewLine(value: String): String {
        return if (STYLE_TAG_REGEX.containsMatchIn(value)) value else "<#FFF3E0>$value"
    }

    private fun normalizeTextInput(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed == "\"\"" || trimmed == "''") {
            return ""
        }
        if (trimmed.isEmpty()) {
            return ""
        }
        return if (STYLE_TAG_REGEX.containsMatchIn(trimmed)) {
            trimmed
        } else {
            "<#FFF3E0>$trimmed"
        }
    }

    private fun escapeQuotedFlagText(text: String): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvedFlagValue(plot: Plot, flagClass: Class<out PlotFlag<*, *>>): Any? {
        return plot.getFlag(flagClass as Class<out PlotFlag<Any?, *>>)
    }

    private fun ItemBuilder.applyModel(modelKey: String) {
        edit { item ->
            val meta: ItemMeta = item.itemMeta ?: return@edit
            NamespacedKey.fromString(modelKey)?.let { resolvedKey ->
                meta.itemModel = resolvedKey
            }
            item.itemMeta = meta
        }
    }

    private fun markTransition(playerId: UUID) {
        menuTransitions += playerId
        hooker.tickScheduler.runLater(1L) {
            menuTransitions.remove(playerId)
        }
    }

    companion object {
        private const val PLOT_SQUARED_PLUGIN_NAME = "PlotSquared"
        private const val CONFIG_ENABLED_FLAGS = "plotsquared.edit.enabled-flags"
        private const val CONFIG_ROLE_FLAGS = "plotsquared.edit.role-flags"
        private const val CONFIG_ADMIN_PERMISSION = "plotsquared.edit.plotEditAdminAccess"
        private const val DEFAULT_ADMIN_PERMISSION = "advancedcreative.acreative"
        private const val APPLY_TIMEOUT_SECONDS = 30
        private const val APPLY_TIMEOUT_TICKS = APPLY_TIMEOUT_SECONDS * 20L
        private const val FLAG_MUTATION_COOLDOWN_TICKS = 2L
        private val STYLE_TAG_REGEX = Regex("<[^>]+>")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val ROOT_ALIASES = setOf("plot", "plots", "p", "plotsquared", "p2", "ps", "2", "plotme")
        private val MAIN_WORK_SLOTS = (9..44).toList()
        private val MAIN_BLACK_SLOTS = setOf(0, 8, 45, 53)
        private val MAIN_GRAY_SLOTS = (1..7).toSet() + (46..52).toSet()
        private val MAIN_INTERACTIVE_SLOTS = MAIN_WORK_SLOTS.toSet() + setOf(48, 49, 50)
        private val COLLECTION_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
        private val COLLECTION_GRAY_SLOTS = setOf(
            1, 2, 3, 4, 5, 6, 7,
            10, 16,
            19, 25,
            28, 34,
            37, 38, 39, 40, 41, 42, 43
        )
        private val COLLECTION_WORK_SLOTS = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33)
        private val BLOCK_PICKER_BLACK_SLOTS = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44)
        private val BLOCK_PICKER_GRAY_SLOTS = setOf(
            1, 2, 3, 4, 5, 6, 7,
            37, 38, 39, 40, 41, 42, 43
        )
        private val BLOCK_PICKER_WORK_SLOTS = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        )
    }
}
