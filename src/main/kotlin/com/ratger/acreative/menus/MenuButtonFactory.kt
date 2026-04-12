package com.ratger.acreative.menus

import com.google.common.collect.LinkedHashMultimap
import com.ratger.acreative.itemedit.container.LockItemSupport
import com.ratger.acreative.itemedit.experimental.ComponentsService
import com.ratger.acreative.itemedit.head.PlayerProfileCopyHelper
import com.ratger.acreative.itemedit.meta.MetaActionsApplier
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.itemedit.trim.ArmorTrimSupport
import org.bukkit.event.inventory.ClickType
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffectType
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.util.ItemBuilder

class MenuButtonFactory(
    private val parser: MiniMessageParser,
    private val componentsService: ComponentsService
) {
    data class ListButtonOption<T>(
        val value: T,
        val label: String
    )

    data class FocusedToggleListOption(
        val label: String,
        val enabled: Boolean
    )
    data class OrderedFocusedToggleListOption(
        val label: String,
        val enabled: Boolean,
        val order: Int?
    )

    data class TextColorFocusOption(
        val colorTag: String,
        val label: String,
        val enabled: Boolean,
        val focused: Boolean,
        val order: Int?
    )

    data class TextShadowOption(
        val colorTag: String,
        val label: String,
        val selected: Boolean
    )

    enum class FocusedToggleListInteraction {
        NEXT_FOCUS,
        TOGGLE_FOCUSED,
        RESET_ALL
    }

    enum class AdvancedLoreInteraction {
        NEXT_FOCUS,
        APPLY_FOCUSED,
        CLEAR_FOCUSED
    }

    fun blackFillerButton() = Button.simple(
        ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(parser.parse("<!i>"))
            .hideTooltip(true)
            .build()
    ).build()

    fun grayFillerButton() = Button.simple(
        ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(parser.parse("<!i>"))
            .hideTooltip(true)
            .build()
    ).build()

    fun simpleModeButton(action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit) = Button.simple(
        ItemBuilder(Material.ENDER_PEARL)
            .name(parser.parse("<!i><#C7A300>⏺ <#FFD700>Простой режим"))
            .lore(listOf(parser.parse("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")))
            .build()
    ).action(action).build()

    fun advancedModeButton(action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit) = Button.simple(
        ItemBuilder(Material.ENDER_EYE)
            .name(parser.parse("<!i><#C7A300>⭐ <#FFD700>Продвинутый режим"))
            .lore(
                listOf(
                    parser.parse("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
                    parser.parse("<!i>"),
                    parser.parse("<!i><dark_red>▍ <#FF1500>Если разбираетесь")
                )
            )
            .build()
    ).action(action).build()

    fun backButton(action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit) = Button.simple(
        ItemBuilder(Material.RED_STAINED_GLASS_PANE)
            .name(parser.parse("<!i><#FF1500>◀ Назад"))
            .build()
    ).action(action).build()

    fun forwardButton(action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit) = Button.simple(
        ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .name(parser.parse("<!i><#00FF40>Вперёд ▶"))
            .build()
    ).action(action).build()

    fun actionButton(
        material: Material,
        name: String,
        lore: List<String>,
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        action: ((ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit)? = null
    ): Button {
        val builder = ItemBuilder(material)
            .name(parser.parse(name))
            .lore(lore.map(parser::parse))
        if (itemModifier != null) {
            builder.itemModifier()
        }
        val buttonBuilder = Button.simple(builder.build())
        if (action != null) {
            buttonBuilder.action(action)
        } else {
            buttonBuilder.action { }
        }
        return buttonBuilder.build()
    }

    fun statefulSummaryButton(
        material: Material,
        active: Boolean,
        activeName: String,
        inactiveName: String,
        selectedEntriesLore: List<String> = emptyList(),
        selectedHeader: String = "<!i><#FFD700>Выбрано:",
        emptyLore: List<String> = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        glintWhenActive: Boolean = true,
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val lore = if (!active) {
            emptyLore
        } else {
            val selectedBlock = buildList {
                if (selectedEntriesLore.isNotEmpty()) {
                    add(selectedHeader)
                    addAll(selectedEntriesLore)
                    add("<!i>")
                }
            }
            emptyLore + listOf("<!i>") + selectedBlock
        }

        return actionButton(
            material = material,
            name = if (active) activeName else inactiveName,
            lore = lore,
            itemModifier = {
                if (active && glintWhenActive) {
                    glint(true)
                }
                itemModifier?.invoke(this)
                this
            },
            action = action
        )
    }

    fun applyResetButton(
        material: Material,
        active: Boolean,
        activeName: String,
        inactiveName: String,
        activeLore: List<String>,
        inactiveLore: List<String>,
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        onApply: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit,
        onReset: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = actionButton(
        material = material,
        name = if (active) activeName else inactiveName,
        lore = if (active) activeLore else inactiveLore,
        itemModifier = {
            if (active) {
                glint(true)
            }
            itemModifier?.invoke(this)
            this
        },
        action = { event ->
            when {
                event.isLeft || event.isShiftLeft -> onApply(event)
                event.isRight || event.isShiftRight -> onReset(event)
            }
        }
    )


    fun textStyleInfoButton(): Button = actionButton(
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

    fun rawMiniMessageNameApplyButton(
        hasName: Boolean,
        escapedPreview: String,
        onApply: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit,
        onReset: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
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

        return applyResetButton(
            material = Material.PAPER,
            active = hasName,
            activeName = "<!i><#C7A300>◎ <#FFD700>Название: <#00FF40>Задано",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Название: <#FF1500>Обычное",
            activeLore = activeLore,
            inactiveLore = usageLore,
            onApply = onApply,
            onReset = onReset
        )
    }

    fun advancedRawLoreEditorButton(
        virtualLines: List<String>,
        escapedVirtualLines: List<String>,
        focusedIndex: Int,
        hasMaterializedLore: Boolean,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent, AdvancedLoreInteraction) -> Unit
    ): Button {
        val safeFocusedIndex = focusedIndex.coerceIn(0, virtualLines.lastIndex.coerceAtLeast(0))
        val loreLines = buildList {
            add("<!i><#FFD700>ЛКМ, <#FFE68A>следующая строка")
            add("<!i><#FFD700>ПКМ, <#FFE68A>изменить текущую")
            add("<!i><#FFD700>Q, <#FFE68A>очистить текущую")
            add("<!i>")
            virtualLines.forEachIndexed { index, line ->
                val escaped = escapedVirtualLines.getOrNull(index).orEmpty()
                val prefix = if (index == safeFocusedIndex) {
                    "<!i><#00FF40>  » "
                } else {
                    "<!i><#C7A300> » "
                }
                add(if (line.isBlank()) prefix else "$prefix<#FFF3E0>$escaped")
            }
            add("<!i>")
        }

        return actionButton(
            material = Material.BOOK,
            name = if (hasMaterializedLore) {
                "<!i><#C7A300>◎ <#FFD700>Описание: <#00FF40>Задано"
            } else {
                "<!i><#C7A300>⭘ <#FFD700>Описание: <#FF1500>Нет"
            },
            lore = loreLines,
            itemModifier = {
                if (hasMaterializedLore) {
                    glint(true)
                }
                this
            },
            action = { event ->
                val interaction = when {
                    event.isLeft || event.isShiftLeft -> AdvancedLoreInteraction.NEXT_FOCUS
                    event.isRight || event.isShiftRight -> AdvancedLoreInteraction.APPLY_FOCUSED
                    isDropClick(event) -> AdvancedLoreInteraction.CLEAR_FOCUSED
                    else -> return@actionButton
                }
                action(event, interaction)
            }
        )
    }

    fun toggleButton(
        material: Material,
        enabled: Boolean,
        enabledName: String,
        disabledName: String,
        lore: List<String>,
        glintWhenEnabled: Boolean = true,
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = actionButton(
        material = material,
        name = if (enabled) enabledName else disabledName,
        lore = lore,
        itemModifier = {
            if (enabled && glintWhenEnabled) {
                glint(true)
            }
            itemModifier?.invoke(this)
            this
        },
        action = action
    )


    fun headTextureSourceButton(
        editedItem: ItemStack,
        activeName: String,
        lore: List<String>,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val item = editedItem.clone().also {
            if (it.type != Material.PLAYER_HEAD) {
                it.type = Material.PLAYER_HEAD
            }
        }
        val meta = item.itemMeta
        meta?.let {
            it.displayName(parser.parse(activeName))
            it.lore(lore.map(parser::parse))
            it.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            it.setEnchantmentGlintOverride(true)
            item.itemMeta = it
        }
        return Button.simple(item).action(action).build()
    }

    fun <T> listButton(
        material: Material,
        options: List<ListButtonOption<T>>,
        selectedIndex: Int,
        titleBuilder: (ListButtonOption<T>, Int) -> String,
        beforeOptionsLore: List<String> = emptyList(),
        afterOptionsLore: List<String> = emptyList(),
        itemModifier: (ItemBuilder.(ListButtonOption<T>) -> ItemBuilder)? = null,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent, Int) -> Unit
    ): Button {
        require(options.isNotEmpty()) { "List button options cannot be empty" }

        val safeSelectedIndex = selectedIndex.coerceIn(0, options.lastIndex)
        val selected = options[safeSelectedIndex]
        val lore = beforeOptionsLore + buildListButtonLore(options, safeSelectedIndex) + afterOptionsLore
        val builder = ItemBuilder(material)
            .name(parser.parse(titleBuilder(selected, safeSelectedIndex)))
            .lore(lore.map(parser::parse))

        if (itemModifier != null) {
            builder.itemModifier(selected)
        }

        return Button.simple(builder.build())
            .action { event ->
                val newIndex = when {
                    event.isLeft || event.isShiftLeft -> (safeSelectedIndex + 1) % options.size
                    event.isRight || event.isShiftRight -> (safeSelectedIndex - 1 + options.size) % options.size
                    else -> return@action
                }
                action(event, newIndex)
            }
            .build()
    }

    fun focusedToggleListButton(
        material: Material,
        title: String,
        options: List<FocusedToggleListOption>,
        focusedIndex: Int,
        beforeOptionsLore: List<String> = emptyList(),
        afterOptionsLore: List<String> = emptyList(),
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent, FocusedToggleListInteraction) -> Unit
    ): Button {
        require(options.isNotEmpty()) { "Focused toggle list options cannot be empty" }
        val safeFocusedIndex = focusedIndex.coerceIn(0, options.lastIndex)
        val lore = beforeOptionsLore + buildFocusedToggleListLore(options, safeFocusedIndex) + afterOptionsLore
        val builder = ItemBuilder(material)
            .name(parser.parse(title))
            .lore(lore.map(parser::parse))
        if (itemModifier != null) {
            builder.itemModifier()
        }
        return Button.simple(builder.build()).action { event ->
            val interaction = when {
                event.isLeft || event.isShiftLeft -> FocusedToggleListInteraction.NEXT_FOCUS
                event.isRight || event.isShiftRight -> FocusedToggleListInteraction.TOGGLE_FOCUSED
                isDropClick(event) -> FocusedToggleListInteraction.RESET_ALL
                else -> return@action
            }
            action(event, interaction)
        }.build()
    }

    fun textOrderedColorFocusButton(
        material: Material,
        activeTitle: String,
        inactiveTitle: String,
        active: Boolean,
        options: List<TextColorFocusOption>,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent, FocusedToggleListInteraction) -> Unit
    ): Button {
        val lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
            "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
            "<!i>"
        ) + options.map { option ->
            when {
                option.enabled && option.focused ->
                    "<!i><#00FF40>[<#00FF40>✔<#00FF40>]<#00FF40>  » <b><${option.colorTag}>${option.label}</b><white> <#C7A300>[<#FFD700>${option.order}<#C7A300>]"

                option.enabled && !option.focused ->
                    "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<#C7A300><b> </b>» <b><${option.colorTag}>${option.label}</b><white> <#C7A300>[<#FFD700>${option.order}<#C7A300>]"

                !option.enabled && option.focused ->
                    "<!i><#00FF40>[<#FF1500>✘<#00FF40>]<#00FF40>  » <${option.colorTag}>${option.label}"

                else ->
                    "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<#C7A300><b> </b>» <${option.colorTag}>${option.label}"
            }
        } + "<!i>"

        return actionButton(
            material = material,
            name = if (active) activeTitle else inactiveTitle,
            lore = lore,
            itemModifier = {
                if (active) glint(true)
                this
            },
            action = { event ->
                val interaction = when {
                    event.isLeft || event.isShiftLeft -> FocusedToggleListInteraction.NEXT_FOCUS
                    event.isRight || event.isShiftRight -> FocusedToggleListInteraction.TOGGLE_FOCUSED
                    isDropClick(event) -> FocusedToggleListInteraction.RESET_ALL
                    else -> return@actionButton
                }
                action(event, interaction)
            }
        )
    }

    fun textShadowSelectButton(
        material: Material,
        activeTitle: String,
        inactiveTitle: String,
        active: Boolean,
        options: List<TextShadowOption>,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent, Int) -> Unit
    ): Button {
        require(options.isNotEmpty()) { "Text shadow options cannot be empty" }

        val selectedIndex = options.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
        val lore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            "<!i>"
        ) + options.map { option ->
            val displayColorTag = if (option.colorTag == "ordinary") "white" else option.colorTag
            if (option.selected) {
                "<!i><#00FF40>  » <b><$displayColorTag>${option.label}"
            } else {
                "<!i><#C7A300><b> </b>» <$displayColorTag>${option.label}"
            }
        } + listOf("<!i>")

        return actionButton(
            material = material,
            name = if (active) activeTitle else inactiveTitle,
            lore = lore,
            itemModifier = {
                if (active) glint(true)
                this
            },
            action = { event ->
                val newIndex = when {
                    event.isRight || event.isShiftRight -> (selectedIndex - 1 + options.size) % options.size
                    event.isLeft || event.isShiftLeft -> (selectedIndex + 1) % options.size
                    else -> return@actionButton
                }
                action(event, newIndex)
            }
        )
    }

    private fun <T> buildListButtonLore(options: List<ListButtonOption<T>>, selectedIndex: Int): List<String> {
        return options.mapIndexed { index, option ->
            if (index == selectedIndex) {
                "<!i>  <#00FF40>» ${option.label} "
            } else {
                "<!i><b> </b><#C7A300>» ${option.label} "
            }
        }
    }

    private fun buildFocusedToggleListLore(options: List<FocusedToggleListOption>, focusedIndex: Int): List<String> {
        return options.mapIndexed { index, option ->
            val isFocused = index == focusedIndex

            val statePrefix = if (option.enabled) {
                if (isFocused) {
                    "<!i><#00FF40>[<#00FF40>✔<#00FF40>]"
                } else {
                    "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]"
                }
            } else {
                if (isFocused) {
                    "<!i><#00FF40>[<#FF1500>✘<#00FF40>]"
                } else {
                    "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]"
                }
            }

            val focusSuffix = if (isFocused) {
                "  <#00FF40>» ${option.label} "
            } else {
                "<b> </b><#C7A300>» ${option.label} "
            }

            statePrefix + focusSuffix
        }
    }

    private fun buildOrderedFocusedToggleListLore(options: List<OrderedFocusedToggleListOption>, focusedIndex: Int): List<String> {
        return options.mapIndexed { index, option ->
            val statePrefix = if (option.enabled) {
                val orderSuffix = option.order?.let { " <#FFF3E0>[<#00FF40>$it<#FFF3E0>]" } ?: ""
                "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]$orderSuffix"
            } else {
                "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]"
            }
            val focusSuffix = if (index == focusedIndex) {
                "  <#00FF40>» ${option.label} "
            } else {
                "<b> </b><#C7A300>» ${option.label} "
            }
            statePrefix + focusSuffix
        }
    }

    private fun isDropClick(event: ru.violence.coreapi.bukkit.api.menu.event.ClickEvent): Boolean {
        return event.type == ClickType.DROP || event.type == ClickType.CONTROL_DROP
    }

    fun editablePreviewButton(item: ItemStack): Button = Button.simple(item.clone()).action { }.build()

    fun headTextureValueInputSlotButton(
        valueBook: ItemStack?,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = itemInputSlotButton(
        storedItem = valueBook,
        placeholderName = "<!i><#FFD700>→ <#FFE68A>Слот для value <#FFD700>←",
        action = action
    )

    fun lockKeySlotButton(
        keyItem: ItemStack?,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = itemInputSlotButton(
        storedItem = keyItem,
        placeholderName = "<!i><#FFD700>→ <#FFE68A>Слот ключа<#FFD700> ←",
        action = action
    )

    fun useRemainderSlotButton(
        remainder: ItemStack?,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = itemInputSlotButton(
        storedItem = remainder,
        placeholderName = "<!i><#FFD700>→ <#FFE68A>Слот предмета<#FFD700> ←",
        action = action
    )

    private fun itemInputSlotButton(
        storedItem: ItemStack?,
        placeholderName: String,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val buttonItem = storedItem?.clone()
            ?: ItemBuilder(Material.BARRIER)
                .name(parser.parse(placeholderName))
                .build()

        return Button.simple(buttonItem).action(action).build()
    }

    fun specialParameterButton(
        editedItem: ItemStack,
        viewer: Player,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit = { }
    ): Button {
        val type = editedItem.type
        val lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")
        val buttonItem = when {
            ArmorTrimSupport.supports(editedItem) ->
                ItemBuilder(Material.NETHERITE_SCRAP)
                    .name(parser.parse("<!i><#C7A300>₪ <#FFD700>Отделка брони"))
                    .lore(lore.map(parser::parse))
                    .build()

            LockItemSupport.supports(editedItem) ->
                ItemBuilder(Material.FURNACE_MINECART)
                    .name(parser.parse("<!i><#C7A300>🛡 <#FFD700>Запереть на ключ"))
                    .lore(lore.map(parser::parse))
                    .build()

            type == Material.PLAYER_HEAD ->
                ItemBuilder(Material.PLAYER_HEAD)
                    .name(parser.parse("<!i><#C7A300>⭐ <#FFD700>Текстура головы"))
                    .lore(lore.map(parser::parse))
                    .meta(SkullMeta::class.java) { meta ->
                        meta.playerProfile = PlayerProfileCopyHelper.copyProfile(viewer.playerProfile)
                    }
                    .build()

            type == Material.DECORATED_POT ->
                ItemBuilder(Material.DANGER_POTTERY_SHERD)
                    .name(parser.parse("<!i><#C7A300>🛡 <#FFD700>Параметры вазы"))
                    .lore(lore.map(parser::parse))
                    .build()

            type == Material.ITEM_FRAME || type == Material.GLOW_ITEM_FRAME ->
                ItemBuilder(Material.ITEM_FRAME)
                    .name(parser.parse("<!i><#C7A300>⭘ <#FFD700>Невидимость рамки: <#FF1500>Выкл"))
                    .lore(listOf(parser.parse("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")))
                    .build()

            type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION || type == Material.TIPPED_ARROW ->
                ItemBuilder(type)
                    .name(parser.parse("<!i><#C7A300>🧪 <#FFD700>Параметры зелья"))
                    .lore(lore.map(parser::parse))
                    .flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .build()

            type == Material.FILLED_MAP ->
                ItemBuilder(Material.FILLED_MAP)
                    .name(parser.parse("<!i><#C7A300>🔔 <#FFD700>Параметры карты"))
                    .lore(lore.map(parser::parse))
                    .build()

            else -> ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .name(parser.parse("<!i><#FFD700>Особый параметр"))
                .build()
        }
        return Button.simple(buttonItem).action(action).build()
    }


    fun armorTrimRootPatternButton(
        patternDisplayName: String?,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = actionButton(
        material = Material.NETHERITE_SCRAP,
        name = patternDisplayName?.let {
            "<!i><#C7A300>◎ <#FFD700>Отделка: <#00FF40>$it"
        } ?: "<!i><#C7A300>⭘ <#FFD700>Отделка: <#FF1500>Нет",
        lore = listOf(
            if (patternDisplayName == null) {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы задать"
            } else {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"
            }
        ),
        itemModifier = {
            if (patternDisplayName != null) {
                glint(true)
                hideAdditionalTooltip().invoke(this)
            }
            this
        },
        action = action
    )

    fun armorTrimRootMaterialButton(
        materialDisplayName: String?,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = actionButton(
        material = Material.STRUCTURE_VOID,
        name = materialDisplayName?.let {
            "<!i><#C7A300>◎ <#FFD700>Материал: <#FFF3E0>$it"
        } ?: "<!i><#C7A300>⭘ <#FFD700>Материал: <#FF1500>Нет",
        lore = listOf(
            if (materialDisplayName == null) {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы задать"
            } else {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"
            }
        ),
        itemModifier = {
            if (materialDisplayName != null) {
                glint(true)
            }
            this
        },
        action = action
    )

    fun armorTrimPatternOptionButton(
        icon: Material,
        displayName: String,
        selected: Boolean,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = actionButton(
        material = icon,
        name = if (selected) {
            "<!i><#C7A300>◎ <#FFD700>Отделка «$displayName»"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Отделка «$displayName»"
        },
        lore = listOf(
            if (selected) {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы снять"
            } else {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы выбрать"
            }
        ),
        itemModifier = {
            hideAdditionalTooltip().invoke(this)
            if (selected) {
                glint(true)
            }
            this
        },
        action = action
    )

    fun armorTrimMaterialOptionButton(
        icon: Material,
        displayName: String,
        selected: Boolean,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button = actionButton(
        material = icon,
        name = if (selected) {
            "<!i><#C7A300>◎ <#FFD700>$displayName"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>$displayName"
        },
        lore = listOf(
            if (selected) {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы снять"
            } else {
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы выбрать"
            }
        ),
        itemModifier = {
            if (selected) {
                glint(true)
            }
            this
        },
        action = action
    )

    fun decoratedPotPartButton(
        partLabel: String,
        material: Material?,
        materialDisplayName: String?,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val displayMaterial = material ?: Material.BRICK
        val displayName = materialDisplayName ?: "Кирпич"
        return actionButton(
            material = displayMaterial,
            name = "<!i><#C7A300>$partLabel <#FFD700>Часть: <#FFF3E0>$displayName",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
            action = action
        )
    }

    fun potionEffectEntryButton(
        entry: PotionItemSupport.PotionEffectEntry,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val previewPotionType = PotionItemSupport.previewPotionType(entry.effect.type)
        return actionButton(
            material = Material.POTION,
            name = "<!i><#C7A300>◎ <#FFD700>Эффект №${entry.index + 1}",
            lore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
                "<!i>",
                "<!i><#FFD700>Параметры:",
                "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>${entry.displayName}",
                "<!i><#C7A300> ● <#FFE68A>Длительность: <#FFF3E0>${entry.seconds}",
                "<!i><#C7A300> ● <#FFE68A>Уровень: <#FFF3E0>${entry.displayLevel}",
                "<!i><#C7A300> ● <#FFE68A>Видны партиклы: ${if (entry.showParticles) "<#00FF40>Да" else "<#FF1500>Нет"}",
                "<!i><#C7A300> ● <#FFE68A>Иконка в углу: ${if (entry.showIcon) "<#00FF40>Да" else "<#FF1500>Нет"}",
                "<!i>"
            ),
            itemModifier = {
                if (previewPotionType != null) {
                    edit { item ->
                        val meta = item.itemMeta as? PotionMeta ?: return@edit
                        meta.basePotionType = previewPotionType
                        meta.addCustomEffect(entry.effect, true)
                        item.itemMeta = meta
                    }
                }
                flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                this
            },
            action = action
        )
    }

    fun deathProtectionRemoveEffectEntryButton(
        type: PotionEffectType,
        displayName: String,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val previewPotionType = PotionItemSupport.previewPotionType(type)
        return actionButton(
            material = Material.POTION,
            name = "<!i><#C7A300>◎ <#FFD700>$displayName",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить"),
            itemModifier = {
                if (previewPotionType != null) {
                    edit { item ->
                        val meta = item.itemMeta as? PotionMeta ?: return@edit
                        meta.basePotionType = previewPotionType
                        item.itemMeta = meta
                    }
                }
                flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                this
            },
            action = action
        )
    }

    fun deathProtectionApplyEffectEntryButton(
        index: Int,
        displayName: String,
        seconds: Int,
        level: Int,
        chancePercent: String,
        showParticles: Boolean,
        showIcon: Boolean,
        type: PotionEffectType,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val previewPotionType = PotionItemSupport.previewPotionType(type)
        return actionButton(
            material = Material.POTION,
            name = "<!i><#C7A300>◎ <#FFD700>Эффект №$index",
            lore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
                "",
                "<!i><#FFD700>Параметры:",
                "<!i><#C7A300> ● <#FFE68A>Шанс: <#00FF40>$chancePercent%",
                "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>$displayName",
                "<!i><#C7A300> ● <#FFE68A>Длительность: <#FFF3E0>$seconds",
                "<!i><#C7A300> ● <#FFE68A>Уровень: <#FFF3E0>$level",
                "<!i><#C7A300> ● <#FFE68A>Видны партиклы: ${if (showParticles) "<#00FF40>Да" else "<#FF1500>Нет"}",
                "<!i><#C7A300> ● <#FFE68A>Иконка в углу: ${if (showIcon) "<#00FF40>Да" else "<#FF1500>Нет"}",
                ""
            ),
            itemModifier = {
                if (previewPotionType != null) {
                    edit { item ->
                        val meta = item.itemMeta as? PotionMeta ?: return@edit
                        meta.basePotionType = previewPotionType
                        item.itemMeta = meta
                    }
                }
                flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                this
            },
            action = action
        )
    }

    fun potterySherdButton(
        material: Material,
        sherdDisplayName: String,
        selected: Boolean,
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit
    ): Button {
        val marker = if (selected) "◎" else "⭘"
        val lore = if (selected) {
            listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы снять",
                "<!i>",
                "<!i><#00FF40>▍ Выбрано"
            )
        } else {
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы выбрать")
        }
        return actionButton(
            material = material,
            name = "<!i><#C7A300>$marker <#FFD700>Глиняный черепок «$sherdDisplayName»",
            lore = lore,
            itemModifier = {
                if (selected) {
                    glint(true)
                }
                this
            },
            action = action
        )
    }

    fun hideAttributes(): ItemBuilder.() -> ItemBuilder = {
        edit { item ->
            val meta = item.itemMeta ?: return@edit
            meta.attributeModifiers = LinkedHashMultimap.create<Attribute, AttributeModifier>()
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            item.itemMeta = meta
        }
    }

    fun hideAdditionalTooltip(): ItemBuilder.() -> ItemBuilder = {
        flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
    }

    fun hideJukeboxTooltip(itemType: Material): ItemBuilder.() -> ItemBuilder = {
        edit { item ->
            val meta = item.itemMeta ?: return@edit
            MetaActionsApplier.setTooltipHidden(meta, "jukebox_playable", true, itemType)
            item.itemMeta = meta
        }
    }

    fun zeroFoodPreview(): ItemBuilder.() -> ItemBuilder = {
        edit { item ->
            componentsService.applyConsumableToggle(item, true)
            componentsService.applyFoodNutrition(item, 0)
            componentsService.applyFoodSaturation(item, 0f)
        }
    }

    fun hideEverythingExceptTooltip(): ItemBuilder.() -> ItemBuilder = {
        zeroFoodPreview().invoke(this)
        flags(
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_DESTROYS,
            ItemFlag.HIDE_PLACED_ON,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
            ItemFlag.HIDE_DYE,
            ItemFlag.HIDE_ARMOR_TRIM,
            ItemFlag.HIDE_STORED_ENCHANTS
        )
        hideAttributes().invoke(this)
    }
}
