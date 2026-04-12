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
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openPageTwo: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit
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
        menu.setButton(18, buttonFactory.backButton("◀ Назад") { support.transition(session) { openBack(player, session) } })
        menu.setButton(27, buttonFactory.backButton("◀ Назад") { support.transition(session) { openBack(player, session) } })
        menu.setButton(26, buttonFactory.forwardButton("Продвинутый режим ▶") { support.transition(session) { openPageTwo(player, session, openBack) } })
        menu.setButton(35, buttonFactory.forwardButton("Продвинутый режим ▶") { support.transition(session) { openPageTwo(player, session, openBack) } })

        refreshButtons(menu, player, session, openBack)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        menu.setButton(29, buildNameColorButton(session))
        menu.setButton(30, buildNameApplyButton(player, session, openBack))
        menu.setButton(32, buildLoreApplyButton(player, session, openBack))
        menu.setButton(33, buildLoreColorButton(session))
        menu.setButton(39, buildNameShadowButton(session))
        menu.setButton(41, buildLoreShadowButton(session))
    }

    private fun buildNameColorButton(session: ItemEditSession): Button {
        val active = session.orderedNameColors.isNotEmpty()
        val options = TextStylePalette.colors.mapIndexed { index, option ->
            val order = session.orderedNameColors.indexOf(option.key).takeIf { it >= 0 }?.plus(1)
            MenuButtonFactory.TextColorFocusOption(
                colorTag = option.key,
                label = option.maleLabel,
                enabled = order != null,
                focused = index == session.nameColorFocusIndex.coerceIn(0, TextStylePalette.colors.lastIndex),
                order = order
            )
        }
        return buttonFactory.textOrderedColorFocusButton(
            material = Material.BRUSH,
            activeTitle = "<!i><#C7A300>◎ <#FFD700>Цвет названия",
            inactiveTitle = "<!i><#C7A300>⭘ <#FFD700>Цвет названия",
            active = active,
            options = options,
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
                    applyNameStyles(event.player, session)
                    updateEditablePreview(event.menu, session)
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    session.orderedNameColors.clear()
                    applyNameStyles(event.player, session)
                    updateEditablePreview(event.menu, session)
                }
            }
            event.menu.setButton(29, buildNameColorButton(session))
        }
    }

    private fun buildNameApplyButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val hasName = textStyleService.hasCustomName(session.editableItem)
        val preview = textStyleService.preview(textStyleService.customName(session.editableItem), "")
        val escapedPreview = textStyleService.escapeForMiniMessage(preview)
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "<!i>",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            "<!i>"
        )
        val activeLore = listOf("<!i><#C7A300>▍ <#FFF3E0>$escapedPreview", "<!i>") + usageLore
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
                session.nameShadowKey = TextStylePalette.ORDINARY_SHADOW_KEY
                session.usesVanillaNameBase = true
                updateEditablePreview(it.menu, session)
                refreshButtons(it.menu, player, session, openBack)
            }
        )
    }

    private fun buildLoreApplyButton(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit): Button {
        val hasLore = textStyleService.hasLore(session.editableItem)
        val preview = textStyleService.preview(textStyleService.lore(session.editableItem).firstOrNull(), "")
        val escapedPreview = textStyleService.escapeForMiniMessage(preview)
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "<!i>",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            "<!i>"
        )
        val activeLore = listOf("<!i><#C7A300>▍ <#FFF3E0>$escapedPreview", "<!i>") + usageLore

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
                session.loreShadowKey = TextStylePalette.ORDINARY_SHADOW_KEY
                updateEditablePreview(it.menu, session)
                refreshButtons(it.menu, player, session, openBack)
            }
        )
    }

    private fun buildLoreColorButton(session: ItemEditSession): Button {
        val active = session.orderedLoreColors.isNotEmpty()
        val options = TextStylePalette.colors.mapIndexed { index, option ->
            val order = session.orderedLoreColors.indexOf(option.key).takeIf { it >= 0 }?.plus(1)
            MenuButtonFactory.TextColorFocusOption(
                colorTag = option.key,
                label = option.maleLabel,
                enabled = order != null,
                focused = index == session.loreColorFocusIndex.coerceIn(0, TextStylePalette.colors.lastIndex),
                order = order
            )
        }

        return buttonFactory.textOrderedColorFocusButton(
            material = Material.BRUSH,
            activeTitle = "<!i><#C7A300>◎ <#FFD700>Цвет описания",
            inactiveTitle = "<!i><#C7A300>⭘ <#FFD700>Цвет описания",
            active = active,
            options = options,
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
                    applyLoreStyles(session)
                    updateEditablePreview(event.menu, session)
                }
                MenuButtonFactory.FocusedToggleListInteraction.RESET_ALL -> {
                    session.orderedLoreColors.clear()
                    applyLoreStyles(session)
                    updateEditablePreview(event.menu, session)
                }
            }
            event.menu.setButton(33, buildLoreColorButton(session))
        }
    }

    private fun buildNameShadowButton(session: ItemEditSession): Button {
        val options = TextStylePalette.shadowOptions.map { option ->
            MenuButtonFactory.TextShadowOption(
                colorTag = option.key,
                label = option.femaleLabel,
                selected = option.key == session.nameShadowKey
            )
        }
        val active = session.nameShadowKey != TextStylePalette.ORDINARY_SHADOW_KEY

        return buttonFactory.textShadowSelectButton(
            material = Material.INK_SAC,
            activeTitle = "<!i><#C7A300>◎ <#FFD700>Тень названия",
            inactiveTitle = "<!i><#C7A300>⭘ <#FFD700>Тень названия",
            active = active,
            options = options,
        ) { event, newIndex ->
            session.nameShadowKey = TextStylePalette.shadowOptions[newIndex].key
            applyNameStyles(event.player, session)
            updateEditablePreview(event.menu, session)
            event.menu.setButton(39, buildNameShadowButton(session))
        }
    }

    private fun buildLoreShadowButton(session: ItemEditSession): Button {
        val options = TextStylePalette.shadowOptions.map { option ->
            MenuButtonFactory.TextShadowOption(
                colorTag = option.key,
                label = option.femaleLabel,
                selected = option.key == session.loreShadowKey
            )
        }
        val active = session.loreShadowKey != TextStylePalette.ORDINARY_SHADOW_KEY

        return buttonFactory.textShadowSelectButton(
            material = Material.INK_SAC,
            activeTitle = "<!i><#C7A300>◎ <#FFD700>Тень описания",
            inactiveTitle = "<!i><#C7A300>⭘ <#FFD700>Тень описания",
            active = active,
            options = options,
        ) { event, newIndex ->
            session.loreShadowKey = TextStylePalette.shadowOptions[newIndex].key
            applyLoreStyles(session)
            updateEditablePreview(event.menu, session)
            event.menu.setButton(41, buildLoreShadowButton(session))
        }
    }

    private fun applyNameStyles(player: Player, session: ItemEditSession) {
        val base = if (session.usesVanillaNameBase) {
            session.editableItem.effectiveName()
        } else {
            textStyleService.customName(session.editableItem) ?: session.editableItem.effectiveName()
        }
        val withColors = textStyleService.applyOrderedColors(base, session.orderedNameColors, player.locale())
        textStyleService.setCustomName(
            session.editableItem,
            textStyleService.applyShadow(withColors, textStyleService.resolveShadowColor(session.nameShadowKey))
        )
    }

    private fun applyLoreStyles(session: ItemEditSession) {
        val lore = textStyleService.lore(session.editableItem)
        if (lore.isEmpty()) return
        val shadowColor = textStyleService.resolveShadowColor(session.loreShadowKey)
        val transformed = lore.map { line ->
            textStyleService.applyShadow(
                textStyleService.applyOrderedColors(line, session.orderedLoreColors),
                shadowColor
            )
        }
        textStyleService.setLore(session.editableItem, transformed)
    }

    private fun initializeStyleState(session: ItemEditSession) {
        if (session.textStyleStateInitialized) return
        val hasCustomName = textStyleService.hasCustomName(session.editableItem)
        session.usesVanillaNameBase = !hasCustomName
        val name = textStyleService.customName(session.editableItem)
        session.orderedNameColors.clear()
        session.orderedNameColors.addAll(name?.let(textStyleService::detectOrderedColors).orEmpty())
        session.nameShadowKey = normalizeShadowKey(name?.let(textStyleService::detectShadowColor))

        val lore = textStyleService.lore(session.editableItem).firstOrNull()
        session.orderedLoreColors.clear()
        session.orderedLoreColors.addAll(lore?.let(textStyleService::detectOrderedColors).orEmpty())
        session.loreShadowKey = normalizeShadowKey(lore?.let(textStyleService::detectShadowColor))
        session.textStyleStateInitialized = true
    }

    private fun normalizeShadowKey(detectedKey: String?): String {
        val key = detectedKey ?: TextStylePalette.ORDINARY_SHADOW_KEY
        return TextStylePalette.shadowOptions.firstOrNull { it.key == key }?.key
            ?: TextStylePalette.ORDINARY_SHADOW_KEY
    }

    private fun updateEditablePreview(menu: Menu, session: ItemEditSession) {
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
    }
}
