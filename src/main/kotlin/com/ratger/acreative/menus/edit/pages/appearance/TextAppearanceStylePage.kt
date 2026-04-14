package com.ratger.acreative.menus.edit.pages.appearance

import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import org.bukkit.entity.Player
import org.bukkit.Material
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button

class TextAppearanceStylePage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val textStyleService: ItemTextStyleService,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openPageOne: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        initializeRawTextState(session)

        val menuSize = 45
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Внешнее [2/2]",
            menuSize = menuSize,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, menuSize, blackSlots)
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton("◀ Простой режим") { support.transition(session) { openPageOne(player, session, openBack) } })
        menu.setButton(26, buildTextStyleInfoButton())
        refreshButtons(menu, player, session, openBack)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        menu.setButton(30, buildRawNameButton(player, session, openBack))
        menu.setButton(32, buildAdvancedLoreButton(player, session, openBack))
    }

    private fun buildRawNameButton(
        player: Player,
        session: ItemEditSession,
        openBack: (Player, ItemEditSession) -> Unit
    ): Button {
        return buttonFactory.rawMiniMessageNameApplyButton(
            hasName = textStyleService.hasCustomName(session.editableItem),
            preview = namePreview(session),
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.NAME_RAW_MINIMESSAGE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession, openBack)
                    }
                }
            },
            onReset = {
                textStyleService.setCustomName(session.editableItem, null)
                session.rawMiniMessageNameInput = null
                updateEditablePreview(it.menu, session)
                refreshButtons(it.menu, player, session, openBack)
            }
        )
    }

    private fun buildAdvancedLoreButton(
        player: Player,
        session: ItemEditSession,
        openBack: (Player, ItemEditSession) -> Unit
    ): Button {
        return buttonFactory.advancedRawLoreEditorButton(
            virtualLines = session.virtualLoreRawLines,
            focusedIndex = session.loreRawFocusIndex,
            hasMaterializedLore = textStyleService.hasLore(session.editableItem)
        ) { event, interaction ->
            when (interaction) {
                MenuButtonFactory.AdvancedLoreInteraction.NEXT_FOCUS -> {
                    session.loreRawFocusIndex = (session.loreRawFocusIndex + 1) % session.virtualLoreRawLines.size
                }

                MenuButtonFactory.AdvancedLoreInteraction.APPLY_FOCUSED -> {
                    support.transition(session) {
                        requestApplyInput(player, session, EditorApplyKind.LORE_RAW_MINIMESSAGE_LINE) { reopenPlayer, reopenSession ->
                            open(reopenPlayer, reopenSession, openBack)
                        }
                    }
                    return@advancedRawLoreEditorButton
                }

                MenuButtonFactory.AdvancedLoreInteraction.CLEAR_FOCUSED -> {
                    val updated = textStyleService.clearVirtualLoreLine(session.virtualLoreRawLines, session.loreRawFocusIndex)
                    session.virtualLoreRawLines.clear()
                    session.virtualLoreRawLines.addAll(updated)
                    session.loreRawFocusIndex = session.loreRawFocusIndex.coerceIn(0, session.virtualLoreRawLines.lastIndex)
                    textStyleService.setLoreFromVirtualRawLines(session.editableItem, session.virtualLoreRawLines)
                    updateEditablePreview(event.menu, session)
                }
            }

            event.menu.setButton(32, buildAdvancedLoreButton(player, session, openBack))
        }
    }

    private fun initializeRawTextState(session: ItemEditSession) {
        if (session.rawTextStyleStateInitialized) {
            if (session.virtualLoreRawLines.isEmpty()) {
                session.virtualLoreRawLines.addAll(textStyleService.ensureVirtualLoreLines(emptyList()))
            }
            session.loreRawFocusIndex = session.loreRawFocusIndex.coerceIn(0, session.virtualLoreRawLines.lastIndex)
            return
        }

        session.rawMiniMessageNameInput = textStyleService.customName(session.editableItem)
            ?.let(textStyleService::serializeMiniMessage)

        session.virtualLoreRawLines.clear()
        session.virtualLoreRawLines.addAll(
            textStyleService.ensureVirtualLoreLines(
                textStyleService.serializeLoreToRawMiniMessageLines(textStyleService.lore(session.editableItem))
            )
        )
        session.loreRawFocusIndex = session.loreRawFocusIndex.coerceIn(0, session.virtualLoreRawLines.lastIndex)
        session.rawTextStyleStateInitialized = true
    }

    private fun namePreview(session: ItemEditSession): String {
        return session.rawMiniMessageNameInput
            ?: textStyleService.customName(session.editableItem)?.let(textStyleService::serializeMiniMessage)
            ?: ""
    }

    private fun updateEditablePreview(menu: Menu, session: ItemEditSession) {
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))
    }

    private fun buildTextStyleInfoButton(): Button = buttonFactory.actionButton(
        material = Material.OAK_HANGING_SIGN,
        name = "<!i><#C7A300>ℹ <#FFD700>Стили и Цвета",
        lore = listOf(
            "",
            "<!i><white> \\<white>             <gray>\\<b><white><b>Pepich</b><gray>\\</b>",
            "<!i><gray> \\<gray>             \\<i><white><i>Pepich</i><gray>\\</i>",
            "<!i><dark_gray> \\<dark_gray>      <gray>\\<u><white><u>Pepich</u><gray>\\</u>",
            "<!i><black> \\<black>             <gray>\\<st><white><st>Pepich</st><gray>\\</st>",
            "<!i><yellow> \\<yellow>            <gray>\\<obf><white><obf>Pepich</obf><gray>\\</obf>",
            "<!i><gold> \\<gold>",
            "<!i><red> \\<red>               <gray>\\<#00FF79><#00FF79>Pepich<gray>\\</#00FF79>",
            "<!i><dark_red> \\<dark_red>        <gray>\\<gradient<red>:<gray>#00FF79<red>:<gray>#FF00D9><gradient:#00FF79:#FF00D9>Pepich</gradient><gray>\\</gradient> ",
            "<!i><green> \\<green>            <gray>\\<shadow<red>:<gray>#00FF79<red>:<gray>1><shadow:#00FF79:1><white>Pepich</white></shadow><gray>\\</shadow>",
            "<!i><dark_green> \\<dark_green>",
            "<!i><aqua> \\<aqua>",
            "<!i><dark_aqua> \\<dark_aqua>",
            "<!i><blue> \\<blue>",
            "<!i><dark_blue> \\<dark_blue>",
            "<!i><light_purple> \\<light_purple>",
            "<!i><dark_purple> \\<dark_purple>",
            ""
        )
    )
}
