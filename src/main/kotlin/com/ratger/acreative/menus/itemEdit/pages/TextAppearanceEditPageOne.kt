package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.itemedit.text.TextStylePalette
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class TextAppearanceEditPageOne(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val textStyleService: ItemTextStyleService,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        initializeStyleState(session)

        val menuSize = 54
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Внешнее [1/2]",
            menuSize = menuSize,
            rows = MenuRows.SIX,
            interactiveTopSlots = setOf(18, 27, 26, 35, 29, 30, 32, 33, 39, 41),
            session = session
        )

        support.fillBase(menu, menuSize, support.advancedBlackSlots)
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(27, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(26, buttonFactory.forwardButton { })
        menu.setButton(35, buttonFactory.forwardButton { })

        refreshButtons(menu, player, session, openBack)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        menu.setButton(29, buildNameColorButton(menu, session))
        menu.setButton(30, buildNameApplyButton(player, session, openBack))
        menu.setButton(32, buildLoreApplyButton(player, session, openBack))
        menu.setButton(33, buildLoreColorButton(menu, session))
        menu.setButton(39, buildNameShadowButton(menu, session))
        menu.setButton(41, buildLoreShadowButton(menu, session))
    }

    private fun buildNameColorButton(menu: Menu, session: ItemEditSession): Button {
        val active = session.orderedNameColors.isNotEmpty()
        val options = TextStylePalette.colors.mapIndexed { index, option ->
            val order = session.orderedNameColors.indexOf(option.key).takeIf { it >= 0 }?.plus(1)
            MenuButtonFactory.OrderedFocusedToggleListOption(
                label = option.maleLabel,
                enabled = order != null,
                order = order
            )
        }
        return buttonFactory.orderedFocusedToggleListButton(
            material = Material.BRUSH,
            title = if (active) "<!i><#C7A300>◎ <#FFD700>Цвет названия" else "<!i><#C7A300>⭘ <#FFD700>Цвет названия",
            options = options,
            focusedIndex = session.nameColorFocusIndex,
            beforeOptionsLore = listOf(
                "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
                "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
                "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
                "<!i>"
            ),
            itemModifier = {
                if (active) glint(true)
                this
            }
        ) { event, interaction ->
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    session.nameColorFocusIndex = (session.nameColorFocusIndex + 1) % TextStylePalette.colors.size
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    val key = TextStylePalette.colors[session.nameColorFocusIndex.coerceIn(0, TextStylePalette.colors.lastIndex)].key
                    if (!session.orderedNameColors.remove(key)) {
                        session.orderedNameColors.add(key)
                    }
                    applyNameColors(session)
                    updateEditablePreview(event.menu, session)
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    session.orderedNameColors.clear()
                    applyNameColors(session)
                    updateEditablePreview(event.menu, session)
                }
            }
            event.menu.setButton(29, buildNameColorButton(event.menu, session))
        }
    }

    private fun buildNameApplyButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val hasName = textStyleService.hasCustomName(session.editableItem)
        val preview = textStyleService.preview(textStyleService.customName(session.editableItem), "")
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "<!i>",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            "<!i>"
        )
        val activeLore = listOf("<!i><#C7A300>▍ <#FFF3E0>$preview", "<!i>") + usageLore
        return buttonFactory.applyResetButton(
            material = Material.PAPER,
            active = hasName,
            activeName = "<!i><#C7A300>◎ <#FFD700>Название: <#00FF40>Задано",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Название: <#FF1500>Обычное",
            activeLore = activeLore,
            inactiveLore = usageLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.NAME_TEXT) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = {
                textStyleService.setCustomName(session.editableItem, null)
                session.orderedNameColors.clear()
                updateEditablePreview(it.menu, session)
                refreshButtons(it.menu, player, session, openBack)
            }
        )
    }

    private fun buildLoreApplyButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val hasLore = textStyleService.hasLore(session.editableItem)
        val preview = textStyleService.preview(textStyleService.lore(session.editableItem).firstOrNull(), "")
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "<!i>",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            "<!i>"
        )
        val activeLore = listOf("<!i><#C7A300>▍ <#FFF3E0>$preview", "<!i>") + usageLore

        return buttonFactory.applyResetButton(
            material = Material.BOOK,
            active = hasLore,
            activeName = "<!i><#C7A300>◎ <#FFD700>Описание: <#00FF40>Задано",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Описание: <#FF1500>Нет",
            activeLore = activeLore,
            inactiveLore = usageLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.LORE_TEXT) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = {
                textStyleService.setLore(session.editableItem, null)
                session.orderedLoreColors.clear()
                updateEditablePreview(it.menu, session)
                refreshButtons(it.menu, player, session, openBack)
            }
        )
    }

    private fun buildLoreColorButton(menu: Menu, session: ItemEditSession): Button {
        val active = session.orderedLoreColors.isNotEmpty()
        val options = TextStylePalette.colors.map { option ->
            val order = session.orderedLoreColors.indexOf(option.key).takeIf { it >= 0 }?.plus(1)
            MenuButtonFactory.OrderedFocusedToggleListOption(
                label = option.femaleLabel,
                enabled = order != null,
                order = order
            )
        }

        return buttonFactory.orderedFocusedToggleListButton(
            material = Material.BRUSH,
            title = if (active) "<!i><#C7A300>◎ <#FFD700>Цвет описания" else "<!i><#C7A300>⭘ <#FFD700>Цвет описания",
            options = options,
            focusedIndex = session.loreColorFocusIndex,
            beforeOptionsLore = listOf(
                "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
                "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
                "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
                "<!i>"
            ),
            itemModifier = {
                if (active) glint(true)
                this
            }
        ) { event, interaction ->
            when (interaction) {
                MenuButtonFactory.FocusedToggleListInteraction.NEXT_FOCUS -> {
                    session.loreColorFocusIndex = (session.loreColorFocusIndex + 1) % TextStylePalette.colors.size
                }
                MenuButtonFactory.FocusedToggleListInteraction.TOGGLE_FOCUSED -> {
                    val key = TextStylePalette.colors[session.loreColorFocusIndex.coerceIn(0, TextStylePalette.colors.lastIndex)].key
                    if (!session.orderedLoreColors.remove(key)) {
                        session.orderedLoreColors.add(key)
                    }
                    applyLoreColors(session)
                    updateEditablePreview(event.menu, session)
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    session.orderedLoreColors.clear()
                    applyLoreColors(session)
                    updateEditablePreview(event.menu, session)
                }
            }
            event.menu.setButton(33, buildLoreColorButton(event.menu, session))
        }
    }

    private fun buildNameShadowButton(menu: Menu, session: ItemEditSession): Button {
        val name = textStyleService.customName(session.editableItem)
        val selected = name?.let(textStyleService::detectShadowColor)
        val selectedIndex = TextStylePalette.shadowOptions.indexOfFirst { it?.key == selected }.takeIf { it >= 0 } ?: 0
        val active = selected != null

        val options = TextStylePalette.shadowOptions.map { option ->
            val label = option?.maleLabel ?: "Обычная"
            MenuButtonFactory.ListButtonOption(option?.key, label)
        }

        return buttonFactory.listButton(
            material = Material.INK_SAC,
            options = options,
            selectedIndex = selectedIndex,
            titleBuilder = { _, _ ->
                if (active) "<!i><#C7A300>◎ <#FFD700>Тень названия" else "<!i><#C7A300>⭘ <#FFD700>Тень названия"
            },
            beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", "<!i>"),
            itemModifier = {
                if (active) glint(true)
                this
            }
        ) { event, newIndex ->
            val shadow = options[newIndex].value
            val base = textStyleService.customName(session.editableItem) ?: textStyleService.materializeVisibleNameIntoCustomName(session.editableItem)
            textStyleService.setCustomName(session.editableItem, textStyleService.applyShadow(base, shadow))
            updateEditablePreview(event.menu, session)
            event.menu.setButton(39, buildNameShadowButton(event.menu, session))
        }
    }

    private fun buildLoreShadowButton(menu: Menu, session: ItemEditSession): Button {
        val lore = textStyleService.lore(session.editableItem)
        val selected = lore.firstOrNull()?.let(textStyleService::detectShadowColor)
        val selectedIndex = TextStylePalette.shadowOptions.indexOfFirst { it?.key == selected }.takeIf { it >= 0 } ?: 0
        val active = selected != null
        val options = TextStylePalette.shadowOptions.map { option ->
            val label = option?.femaleLabel ?: "Обычная"
            MenuButtonFactory.ListButtonOption(option?.key, label)
        }

        return buttonFactory.listButton(
            material = Material.INK_SAC,
            options = options,
            selectedIndex = selectedIndex,
            titleBuilder = { _, _ ->
                if (active) "<!i><#C7A300>◎ <#FFD700>Тень описания" else "<!i><#C7A300>⭘ <#FFD700>Тень описания"
            },
            beforeOptionsLore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить", "<!i>"),
            itemModifier = {
                if (active) glint(true)
                this
            }
        ) { event, newIndex ->
            val shadow = options[newIndex].value
            val currentLore = textStyleService.lore(session.editableItem)
            if (currentLore.isNotEmpty()) {
                textStyleService.setLore(session.editableItem, currentLore.map { line -> textStyleService.applyShadow(line, shadow) })
                updateEditablePreview(event.menu, session)
            }
            event.menu.setButton(41, buildLoreShadowButton(event.menu, session))
        }
    }

    private fun applyNameColors(session: ItemEditSession) {
        val base = textStyleService.customName(session.editableItem) ?: textStyleService.materializeVisibleNameIntoCustomName(session.editableItem)
        textStyleService.setCustomName(session.editableItem, textStyleService.applyOrderedColors(base, session.orderedNameColors))
    }

    private fun applyLoreColors(session: ItemEditSession) {
        val lore = textStyleService.lore(session.editableItem)
        if (lore.isEmpty()) return
        val transformed = lore.map { line -> textStyleService.applyOrderedColors(line, session.orderedLoreColors) }
        textStyleService.setLore(session.editableItem, transformed)
    }

    private fun initializeStyleState(session: ItemEditSession) {
        if (session.textStyleStateInitialized) return
        textStyleService.customName(session.editableItem)?.let { name ->
            session.orderedNameColors.clear()
            session.orderedNameColors.addAll(textStyleService.detectOrderedColors(name))
        }
        textStyleService.lore(session.editableItem).firstOrNull()?.let { lore ->
            session.orderedLoreColors.clear()
            session.orderedLoreColors.addAll(textStyleService.detectOrderedColors(lore))
        }
        session.textStyleStateInitialized = true
    }

    private fun updateEditablePreview(menu: Menu, session: ItemEditSession) {
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
    }
}
