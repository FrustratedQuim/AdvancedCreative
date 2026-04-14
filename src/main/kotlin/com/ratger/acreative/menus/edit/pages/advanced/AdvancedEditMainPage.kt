package com.ratger.acreative.menus.edit.pages.advanced

import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.attributes.ItemAttributeMenuSupport
import com.ratger.acreative.itemedit.meta.MetaActionsApplier
import com.ratger.acreative.itemedit.meta.MaxStackSizeSupport
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.MenuButtonFactory
import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class AdvancedEditMainPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRoot: (Player, ItemEditSession) -> Unit,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val openSpecialParameters: (Player, ItemEditSession) -> Unit,
    private val openTextAppearance: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private data class HiddenInfoOption(val label: String, val key: String)

    companion object {
        private val hiddenInfoOptions = listOf(
            HiddenInfoOption("Скрыть зачарования", "enchantments"),
            HiddenInfoOption("Скрыть атрибуты", "attribute_modifiers"),
            HiddenInfoOption("Скрыть неразрушимость", "unbreakable"),
            HiddenInfoOption("Скрыть разное", "hide_additional_tooltip"),
            HiddenInfoOption("Скрыть цвет брони", "dyed_color"),
            HiddenInfoOption("Скрыть ограничения ломания", "can_break"),
            HiddenInfoOption("Скрыть ограничения установки", "can_place_on"),
            HiddenInfoOption("Скрыть отделку брони", "trim"),
            HiddenInfoOption("Скрыть музыку", "jukebox_playable"),
            HiddenInfoOption("Скрыть само отображение", "hide_tooltip")
        )
    }

    private fun updateEditablePreview(menu: Menu, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
    }

    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 54
        val menu = support.buildMenu(
            title = "<!i>▍ Продвинутый редактор [1/2]",
            menuSize = menuSize,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 26, 35, 29, 31, 32, 33, 38, 39, 40, 41, 42),
            session = session
        )

        support.fillBase(menu, menuSize, support.advancedBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        menu.setButton(26, buttonFactory.forwardButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(35, buttonFactory.forwardButton { support.transition(session) { openAdvancedPageTwo(player, session) } })

        val itemId = session.editableItem.type.key.key
        val amount = session.editableItem.amount
        val editableMeta = session.editableItem.itemMeta
        val modelKey = runCatching { editableMeta?.itemModel }.getOrNull()
        val modelId = runCatching { modelKey?.asString() }.getOrNull()
        val modelName = if (modelId == null) {
            "<!i><#C7A300>⭘ <#FFD700>Модель: <#FF1500>Обычная"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Модель: <#00FF40>$modelId"
        }
        val stackSize = if (editableMeta?.hasMaxStackSize() == true) editableMeta.maxStackSize else null
        val stackSizeName = if (stackSize == null) {
            "<!i><#C7A300>⭘ <#FFD700>Размер стака: <#FF1500>Обычный"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Размер стака: <#00FF40>$stackSize"
        }
        menu.setButton(29, buttonFactory.specialParameterButton(session.editableItem, player) {
            support.transition(session) {
                openSpecialParameters(player, session)
            }
        })
        menu.setButton(30, buttonFactory.actionButton(
            Material.NAME_TAG,
            "<!i><#C7A300>✎ <#FFD700>Изменить название и описание",
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openTextAppearance(player, session) } }
        ))
        menu.setButton(31, buttonFactory.actionButton(session.editableItem.type, "<!i><#C7A300>◎ <#FFD700>ID предмета: <#00FF40>$itemId", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        ), buttonFactory.hideEverythingExceptTooltip(), action = {
            support.transition(session) {
                requestApplyInput(player, session, EditorApplyKind.ITEM_ID) { reopenPlayer, reopenSession ->
                    open(reopenPlayer, reopenSession)
                }
            }
        }))
        menu.setButton(32, buttonFactory.actionButton(Material.STRUCTURE_VOID, modelName, listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Задаёт <#FFF3E0>внешний <#FFE68A>вид предмета. ",
            "<!i><#C7A300> ● <#FFE68A>Не влияет на его <#FFF3E0>поведение. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
            "<!i><#C7A300> ● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки ",
            ""
        ), itemModifier = {
            modelKey?.let { key ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.itemModel = key
                    item.itemMeta = meta
                }
                glint(true)
            }
            buttonFactory.hideEverythingExceptTooltip().invoke(this)
        }, action = { event ->
            if (event.isRight || event.isShiftRight) {
                val meta = session.editableItem.itemMeta
                if (meta != null) {
                    meta.itemModel = null
                    session.editableItem.itemMeta = meta
                    menu.setButton(32, buttonFactory.actionButton(
                        Material.STRUCTURE_VOID,
                        "<!i><#C7A300>⭘ <#FFD700>Модель: <#FF1500>Обычная",
                        listOf(
                            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                            "",
                            "<!i><#FFD700>Назначение:",
                            "<!i><#C7A300> ● <#FFE68A>Задаёт <#FFF3E0>внешний <#FFE68A>вид предмета. ",
                            "<!i><#C7A300> ● <#FFE68A>Не влияет на его <#FFF3E0>поведение. ",
                            "",
                            "<!i><#FFD700>После нажатия:",
                            "<!i><#C7A300> ● <#FFF3E0>/apply <id> <#C7A300>- <#FFE68A>задать по id ",
                            "<!i><#C7A300> ● <#FFF3E0>/apply hand <#C7A300>- <#FFE68A>взять из руки ",
                            ""
                        ),
                        itemModifier = {
                            buttonFactory.hideEverythingExceptTooltip().invoke(this)
                        },
                        action = { clickEvent ->
                            if (clickEvent.isLeft || clickEvent.isShiftLeft) {
                                support.transition(session) {
                                    requestApplyInput(player, session, EditorApplyKind.ITEM_MODEL) { reopenPlayer, reopenSession ->
                                        open(reopenPlayer, reopenSession)
                                    }
                                }
                            }
                        }
                    ))
                    updateEditablePreview(menu, session)
                }
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.ITEM_MODEL) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }))
        menu.setButton(33, buttonFactory.actionButton(Material.BUNDLE, "<!i><#C7A300>◎ <#FFD700>Количество: <#00FF40>$amount", listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        ), buttonFactory.hideAdditionalTooltip(), action = {
            support.transition(session) {
                requestApplyInput(player, session, EditorApplyKind.AMOUNT) { reopenPlayer, reopenSession ->
                    open(reopenPlayer, reopenSession)
                }
            }
        }))
        menu.setButton(38, buttonFactory.actionButton(Material.BRICK, stackSizeName, listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
            ""
        ), itemModifier = {
            if (stackSize != null) {
                glint(true)
            }
            this
        }, action = { event ->
            if (event.isRight || event.isShiftRight) {
                val meta = session.editableItem.itemMeta
                if (meta != null) {
                    if (meta.hasMaxStackSize()) {
                        if (MaxStackSizeSupport.clearCustomMaxStackSize(meta)) {
                            session.editableItem.itemMeta = meta
                        }
                    }
                    menu.setButton(38, buttonFactory.actionButton(
                        Material.BRICK,
                        "<!i><#C7A300>⭘ <#FFD700>Размер стака: <#FF1500>Обычный",
                        listOf(
                            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
                            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
                            "",
                            "<!i><#FFD700>После нажатия:",
                            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
                            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
                            ""
                        ),
                        action = { clickEvent ->
                            if (clickEvent.isLeft || clickEvent.isShiftLeft) {
                                support.transition(session) {
                                    requestApplyInput(player, session, EditorApplyKind.STACK_SIZE) { reopenPlayer, reopenSession ->
                                        open(reopenPlayer, reopenSession)
                                    }
                                }
                            }
                        }
                    ))
                    updateEditablePreview(menu, session)
                }
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.STACK_SIZE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }))
        val brokenTooltipKey = NamespacedKey.minecraft("null")
        val selectedTooltipIndex = if (session.editableItem.itemMeta?.tooltipStyle == brokenTooltipKey) 1 else 0
        val tooltipOptions: List<MenuButtonFactory.ListButtonOption<NamespacedKey?>> = listOf(
            MenuButtonFactory.ListButtonOption(null, "Обычный"),
            MenuButtonFactory.ListButtonOption(brokenTooltipKey, "Сломанный")
        )
        menu.setButton(39, buttonFactory.listButton(
            material = Material.PAINTING,
            options = tooltipOptions,
            selectedIndex = selectedTooltipIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Тултип: <#FFF3E0>Обычный"
                    else -> "<!i><#C7A300>② <#FFD700>Тултип: <#FFF3E0>Сломанный"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf(""),
            itemModifier = { selected ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.tooltipStyle = selected.value
                    item.itemMeta = meta
                }
            },
            action = { _, newIndex ->
                val selected = tooltipOptions[newIndex]
                val meta = session.editableItem.itemMeta ?: return@listButton
                meta.tooltipStyle = selected.value
                session.editableItem.itemMeta = meta
                menu.setButton(39, buildTooltipButton(session))
                updateEditablePreview(menu, session)
            }
        ))
        menu.setButton(40, buildUnbreakableButton(session))
        menu.setButton(41, buildGliderButton(session))
        menu.setButton(42, buildHiddenInfoButton(session))
        menu.open(player)
    }

    private fun buildHiddenInfoButton(session: ItemEditSession): Button {
        val focusedIndex = session.hiddenInfoFocusIndex.coerceIn(0, hiddenInfoOptions.lastIndex)
        val meta = session.editableItem.itemMeta
        val options = hiddenInfoOptions.mapIndexed { index, option ->
            MenuButtonFactory.FocusedToggleListOption(
                label = option.label,
                enabled = isHiddenInfoEnabled(meta, index)
            )
        }
        return buttonFactory.focusedToggleListButton(
            material = Material.BRUSH,
            title = "<!i><#C7A300>✂ <#FFD700>Скрытие информации",
            options = options,
            focusedIndex = focusedIndex,
            beforeOptionsLore = listOf(
                "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
                "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
                "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
                ""
            ),
            afterOptionsLore = listOf(""),
            itemModifier = {
                if (hasAnyHiddenInfoEnabled(meta)) {
                    glint(true)
                }
                this
            }
        ) { event, interaction ->
            session.hiddenInfoFocusIndex = session.hiddenInfoFocusIndex.coerceIn(0, hiddenInfoOptions.lastIndex)
            var itemChanged = false
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    session.hiddenInfoFocusIndex = (session.hiddenInfoFocusIndex + 1) % hiddenInfoOptions.size
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    itemChanged = toggleHiddenInfoOption(session, session.hiddenInfoFocusIndex)
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    itemChanged = resetAllHiddenInfo(session)
                }
            }
            event.menu.setButton(42, buildHiddenInfoButton(session))
            if (itemChanged) {
                updateEditablePreview(event.menu, session)
            }
        }
    }

    private fun isHiddenInfoEnabled(meta: ItemMeta?, index: Int): Boolean {
        if (meta == null) return false
        return MetaActionsApplier.isTooltipHidden(meta, hiddenInfoOptions[index].key)
    }

    private fun hasAnyHiddenInfoEnabled(meta: ItemMeta?): Boolean {
        if (meta == null) return false
        return hiddenInfoOptions.indices.any { index -> isHiddenInfoEnabled(meta, index) }
    }

    private fun toggleHiddenInfoOption(session: ItemEditSession, index: Int): Boolean {
        val meta = session.editableItem.itemMeta ?: return false
        val key = hiddenInfoOptions[index].key
        if (!MetaActionsApplier.canEnableTooltipHide(meta, key, session.editableItem.type) &&
            !MetaActionsApplier.isTooltipHidden(meta, key)
        ) {
            return false
        }
        val before = MetaActionsApplier.isTooltipHidden(meta, key)
        val changed = when (key) {
            "jukebox_playable" if before && session.vanillaDiscJukeboxComponentInjected -> {
                val cleared = MetaActionsApplier.clearVanillaDiscExplicitJukeboxComponent(meta, session.editableItem.type)
                if (cleared) {
                    session.vanillaDiscJukeboxComponentInjected = false
                    true
                } else {
                    MetaActionsApplier.setTooltipHidden(meta, key, false, session.editableItem.type)
                }
            }
            "attribute_modifiers" if before && session.attributesMaterializedForHide -> {
                val unhidden = MetaActionsApplier.setTooltipHidden(meta, key, false, session.editableItem.type)
                val restored = MetaActionsApplier.clearExplicitAttributeModifiers(meta)
                if (unhidden || restored) {
                    session.attributesMaterializedForHide = false
                }
                unhidden || restored
            }
            else -> {
                val hadExplicitJukebox = if (key == "jukebox_playable") meta.hasJukeboxPlayable() else false
                val hadExplicitAttributes = if (key == "attribute_modifiers") {
                    ItemAttributeMenuSupport.hasExplicitAttributeOverride(session.editableItem)
                } else false
                val mutation = MetaActionsApplier.setTooltipHidden(meta, key, !before, session.editableItem.type)
                if (mutation && key == "jukebox_playable" && !before && !hadExplicitJukebox && meta.hasJukeboxPlayable()) {
                    session.vanillaDiscJukeboxComponentInjected = true
                }
                if (mutation && key == "attribute_modifiers" && !before && !hadExplicitAttributes &&
                    ItemAttributeMenuSupport.hasExplicitAttributeOverride(session.editableItem)
                ) {
                    session.attributesMaterializedForHide = true
                }
                mutation
            }
        }
        if (!changed) {
            return false
        }
        val after = MetaActionsApplier.isTooltipHidden(meta, key)
        if (before == after) {
            return false
        }
        session.editableItem.itemMeta = meta
        return true
    }

    private fun resetAllHiddenInfo(session: ItemEditSession): Boolean {
        val meta = session.editableItem.itemMeta ?: return false
        val hadAnyEnabled = hiddenInfoOptions.indices.any { index ->
            MetaActionsApplier.isTooltipHidden(meta, hiddenInfoOptions[index].key)
        }
        if (!hadAnyEnabled) {
            return false
        }
        var changed = false
        for (option in hiddenInfoOptions) {
            if (option.key == "jukebox_playable" && session.vanillaDiscJukeboxComponentInjected) {
                val cleared = MetaActionsApplier.clearVanillaDiscExplicitJukeboxComponent(meta, session.editableItem.type)
                if (cleared) {
                    session.vanillaDiscJukeboxComponentInjected = false
                    changed = true
                    continue
                }
            }
            if (option.key == "attribute_modifiers" && session.attributesMaterializedForHide) {
                val unhidden = MetaActionsApplier.setTooltipHidden(meta, option.key, false, session.editableItem.type)
                val restored = MetaActionsApplier.clearExplicitAttributeModifiers(meta)
                if (unhidden || restored) {
                    session.attributesMaterializedForHide = false
                    changed = true
                }
                continue
            }
            if (MetaActionsApplier.setTooltipHidden(meta, option.key, false, session.editableItem.type)) {
                changed = true
            }
        }
        if (!changed) {
            return false
        }
        session.editableItem.itemMeta = meta
        return true
    }

    private fun buildTooltipButton(session: ItemEditSession): Button {
        val brokenTooltipKey = NamespacedKey.minecraft("null")
        val tooltipOptions: List<MenuButtonFactory.ListButtonOption<NamespacedKey?>> = listOf(
            MenuButtonFactory.ListButtonOption(null, "Обычный"),
            MenuButtonFactory.ListButtonOption(brokenTooltipKey, "Сломанный")
        )
        val selectedTooltipIndex = if (session.editableItem.itemMeta?.tooltipStyle == brokenTooltipKey) 1 else 0
        return buttonFactory.listButton(
            material = Material.PAINTING,
            options = tooltipOptions,
            selectedIndex = selectedTooltipIndex,
            titleBuilder = { _, index ->
                when (index) {
                    0 -> "<!i><#C7A300>① <#FFD700>Тултип: <#FFF3E0>Обычный"
                    else -> "<!i><#C7A300>② <#FFD700>Тултип: <#FFF3E0>Сломанный"
                }
            },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf(""),
            itemModifier = { selected ->
                edit { item ->
                    val meta = item.itemMeta ?: return@edit
                    meta.tooltipStyle = selected.value
                    item.itemMeta = meta
                }
            },
            action = { event, newIndex ->
                val selected = tooltipOptions[newIndex]
                val meta = session.editableItem.itemMeta ?: return@listButton
                meta.tooltipStyle = selected.value
                session.editableItem.itemMeta = meta
                event.menu.setButton(39, buildTooltipButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }

    private fun buildUnbreakableButton(session: ItemEditSession): Button {
        val unbreakableEnabled = session.editableItem.itemMeta?.isUnbreakable == true
        val unbreakableButtonName = if (unbreakableEnabled) {
            "<!i><#C7A300>◎ <#FFD700>Неразрушимость: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Неразрушимость: <#FF1500>Выкл"
        }
        return buttonFactory.actionButton(
            Material.NETHERITE_INGOT,
            unbreakableButtonName,
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            itemModifier = {
                if (unbreakableEnabled) {
                    glint(true)
                }
                this
            },
            action = { event ->
                val meta = session.editableItem.itemMeta ?: return@actionButton
                meta.isUnbreakable = !unbreakableEnabled
                session.editableItem.itemMeta = meta
                event.menu.setButton(40, buildUnbreakableButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }

    private fun buildGliderButton(session: ItemEditSession): Button {
        val gliderEnabled = runCatching { session.editableItem.itemMeta?.isGlider == true }.getOrDefault(false)
        val gliderButtonName = if (gliderEnabled) {
            "<!i><#C7A300>◎ <#FFD700>Парение: <#00FF40>Вкл"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Парение: <#FF1500>Выкл"
        }
        return buttonFactory.actionButton(
            Material.ELYTRA,
            gliderButtonName,
            listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                "",
                "<!i><#FFD700>Назначение:",
                "<!i><#C7A300> ● <#FFE68A>Позволяет <#FFF3E0>парить, <#FFE68A>как на элитрах. ",
                ""
            ),
            itemModifier = {
                if (gliderEnabled) {
                    glint(true)
                }
                this
            },
            action = { event ->
                val meta = session.editableItem.itemMeta ?: return@actionButton
                meta.isGlider = !gliderEnabled
                session.editableItem.itemMeta = meta
                event.menu.setButton(41, buildGliderButton(session))
                updateEditablePreview(event.menu, session)
            }
        )
    }
}
