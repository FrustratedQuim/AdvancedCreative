package com.ratger.acreative.menus

import com.google.common.collect.LinkedHashMultimap
import com.ratger.acreative.menus.edit.container.LockItemSupport
import com.ratger.acreative.menus.edit.experimental.ComponentsService
import com.ratger.acreative.menus.decorationheads.model.Entry
import com.ratger.acreative.menus.decorationheads.model.SavedPageEntry
import com.ratger.acreative.menus.decorationheads.model.SavedPageSourceMode
import com.ratger.acreative.menus.decorationheads.support.MapPreviewColorPalette
import com.ratger.acreative.menus.edit.map.MapItemSupport
import com.ratger.acreative.menus.edit.head.PlayerProfileCopyHelper
import com.ratger.acreative.menus.edit.invisibility.FrameInvisibilitySupport
import com.ratger.acreative.menus.edit.meta.MetaActionsApplier
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.edit.potion.PotionItemSupport
import com.ratger.acreative.menus.edit.trim.ArmorTrimSupport
import com.ratger.acreative.core.TickScheduler
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
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.util.ItemBuilder

class MenuButtonFactory(
    private val parser: MiniMessageParser,
    private val componentsService: ComponentsService,
    private val tickScheduler: TickScheduler
) {
    private fun protectedButton(
        item: ItemStack,
        action: (ClickEvent) -> Unit
    ): Button {
        lateinit var restoreButton: Button
        val wrappedAction: (ClickEvent) -> Unit = { event ->
            runCatching { action(event) }
                .onFailure {
                    val slot = event.rawSlot
                    if (slot < 0) return@onFailure
                    val warningButton = actionButton(
                        material = Material.BARRIER,
                        name = "<!i><#FF1500>⚠ Предмет повреждён..",
                        lore = emptyList()
                    )
                    event.menu.setButton(slot, warningButton)
                    tickScheduler.runLater(30L) {
                        event.menu.setButton(slot, restoreButton)
                    }
                }
        }
        restoreButton = Button.simple(item).action(wrappedAction).build()
        return restoreButton
    }

    data class ListButtonOption<T>(
        val value: T,
        val label: String
    )

    data class FocusedToggleListOption(
        val label: String,
        val enabled: Boolean
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
            .hideTooltip(true)
            .build()
    ).build()

    fun grayFillerButton() = Button.simple(
        ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .hideTooltip(true)
            .build()
    ).build()

    fun backButton(
        text: String = "◀ Назад",
        action: (ClickEvent) -> Unit
    ) = protectedButton(
        ItemBuilder(Material.RED_STAINED_GLASS_PANE)
            .name(parser.parse("<!i><#FF1500>$text"))
            .build(),
        action
    )

    fun forwardButton(
        text: String = "Вперёд ▶",
        action: (ClickEvent) -> Unit
    ) = protectedButton(
        ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .name(parser.parse("<!i><#00FF40>$text"))
            .build(),
        action
    )

    fun decorationHeadsBackButton(action: () -> Unit): Button = backButton("◀ Назад") { action() }

    fun decorationHeadsForwardButton(action: () -> Unit): Button = forwardButton("Вперёд ▶") { action() }

    fun decorationHeadsMyHeadsButton(count: Int, action: () -> Unit): Button = actionButton(
        material = Material.CHEST_MINECART,
        name = "<!i><#FFD700>⭐ Мои головы <#C7A300>[<#FFF3E0>$count<#C7A300>]",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
        action = { action() }
    )

    fun decorationHeadsCategoryButton(
        options: List<String>,
        selectedIndex: Int,
        action: (Int) -> Unit
    ): Button = listButton(
        material = Material.CLOCK,
        options = options.map { ListButtonOption(it, it) },
        selectedIndex = selectedIndex,
        titleBuilder = { _, _ -> "<!i><#FFD700>⚡ Категория" },
        beforeOptionsLore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            ""
        ),
        afterOptionsLore = listOf("")
    ) { _, newIndex ->
        action(newIndex)
    }

    fun decorationHeadsSearchButton(query: String?, action: () -> Unit): Button = actionButton(
        material = Material.COMPASS,
        name = if (query.isNullOrBlank()) {
            "<!i><#FFD700>🔎 Поиск <#C7A300>[<#FFF3E0>Пусто<#C7A300>]"
        } else {
            "<!i><#FFD700>🔎 Поиск <#C7A300>[<#FFF3E0>${sanitizeMiniMessageText(query)}<#C7A300>]"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы указать"),
        itemModifier = {
            if (!query.isNullOrBlank()) glint(true)
            this
        },
        action = { action() }
    )


    fun decorationHeadsMyPagesButton(count: Int, action: () -> Unit): Button = actionButton(
        material = Material.BOOK,
        name = "<!i><#FFD700>⭐ Мои страницы <#C7A300>[<#FFF3E0>$count<#C7A300>]",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
        action = { action() }
    )

    fun decorationHeadsSavePageButton(isSaved: Boolean, action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.PAPER,
        name = if (isSaved) "<!i><#FFD700>✎ Страница сохранена" else "<!i><#FFD700>✎ Сохранить страницу",
        lore = listOf(
            if (isSaved) "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить" else "<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить"
        ),
        itemModifier = {
            if (isSaved) {
                glint(true)
            }
            this
        },
        action = action
    )

    fun decorationHeadsReminderButton(): Button = actionButton(
        material = Material.FIRE_CHARGE,
        name = "<!i><#FFD700>ℹ Важно!",
        lore = listOf(
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Здесь хранятся полученные вами",
            "<!i><#FFE68A>  головы за <#FFD700>последнюю<#FFE68A> неделю.",
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Новые головы <#FFD700>автоматически",
            "<!i><#FFE68A>  заменяют старые.",
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Если голова не была взята <#FFD700>ни разу ",
            "<!i><#FFE68A>  в <#FFD700>течение<#FFE68A> недели, то она будет",
            "<!i><#FFE68A>  удалена отсюда.",
            ""
        )
    )


    fun decorationHeadsSavedPageButton(
        entry: SavedPageEntry,
        displayCategory: String,
        action: (ClickEvent) -> Unit
    ): Button {
        val item = ItemBuilder(Material.FILLED_MAP)
            .name(parser.parse("<!i><#FFD700>Страница #${entry.sourcePage}"))
            .lore(
                buildList {
                    add("<!i><#FFD700>▍ <#FFE68A>Категория: <#FFF3E0>${sanitizeMiniMessageText(displayCategory)}")
                    if (entry.sourceMode == SavedPageSourceMode.SEARCH) {
                        entry.searchQuery?.takeIf { it.isNotBlank() }?.let {
                            add("<!i><#FFD700>▍ <#FFE68A>Запрос: <#FFF3E0>${sanitizeMiniMessageText(it)}")
                        }
                    }
                    entry.note?.let { add("<!i><#FFD700>▍ <#FFE68A>Пометка: <#FFF3E0>${allowMiniMessageColors(it)}") }
                    add("")
                    add("<!i><#FFD700>ЛКМ, <#FFE68A>чтобы перейти")
                    add("<!i><#FFD700>ПКМ, <#FFE68A>чтобы изменить")
                }.map(parser::parse)
            )
            .flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build()

        MapItemSupport.setColor(item, MapPreviewColorPalette.colorFor(entry.mapColorKey))
        return protectedButton(item, action)
    }

    fun decorationHeadsSavedPageNoteButton(note: String?, onApply: (ClickEvent) -> Unit, onReset: (ClickEvent) -> Unit): Button {
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            ""
        )
        val active = !note.isNullOrBlank()
        val activeLore = if (note.isNullOrBlank()) usageLore else listOf("<!i><#C7A300>▍ <#FFF3E0>${allowMiniMessageColors(note)}", "") + usageLore
        return applyResetButton(
            material = Material.WRITABLE_BOOK,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Пометка: <#00FF40>Задана",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Пометка: <#FF1500>Пусто",
            activeLore = activeLore,
            inactiveLore = usageLore,
            onApply = onApply,
            onReset = onReset
        )
    }

    fun decorationHeadsSavedPageNumberButton(sourcePage: Int, action: () -> Unit): Button = actionButton(
        material = Material.SPYGLASS,
        name = "<!i><#C7A300>₪ <#FFD700>Страница: <#00FF40>$sourcePage",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        action = { action() }
    )

    fun decorationHeadsSavedPageColorButton(
        mapColorKey: String?,
        onChange: (ClickEvent, Boolean) -> Unit
    ): Button {
        val selected = MapPreviewColorPalette.optionFor(mapColorKey)
        val options = MapPreviewColorPalette.options.map { option ->
            TextShadowOption(
                colorTag = if (option.key == MapPreviewColorPalette.ORDINARY_KEY) "ordinary" else option.legacyMiniColorTag,
                label = option.label,
                selected = option.key == selected.key
            )
        }
        val selectedIndex = options.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
        return textShadowSelectButton(
            material = Material.BRUSH,
            activeTitle = "<!i><#C7A300>◎ <#FFD700>Цвет предмета",
            inactiveTitle = "<!i><#C7A300>⭘ <#FFD700>Цвет предмета",
            active = selected.key != MapPreviewColorPalette.ORDINARY_KEY,
            options = options
        ) { event, newIndex ->
            val isForward = newIndex == (selectedIndex + 1) % options.size
            onChange(event, isForward)
        }
    }

    fun decorationHeadsSavedPageDeleteButton(action: () -> Unit): Button = actionButton(
        material = Material.BARRIER,
        name = "<!i><#FF1500>⚠ Удалить страницу",
        lore = emptyList(),
        action = { action() }
    )

    fun decorationHeadsResultButton(
        entry: Entry,
        categoryName: String,
        showCategoryLine: Boolean,
        action: (ClickEvent) -> Unit
    ): Button = actionButton(
        material = Material.PLAYER_HEAD,
        name = "<!i><#FFD700>${sanitizeMiniMessageText(entry.name)}",
        lore = buildList {
            if (showCategoryLine) {
                add("<!i><#FFD700>▍ <#FFE68A>Категория: <#FFF3E0>${sanitizeMiniMessageText(categoryName)}")
            }
        },
        action = { action(it) }
    ).let {
        val item = ItemBuilder(Material.PLAYER_HEAD)
            .name(parser.parse("<!i><#FFD700>${sanitizeMiniMessageText(entry.name)}"))
            .lore(
                buildList {
                    if (showCategoryLine) {
                        add("<!i><#FFD700>▍ <#FFE68A>Категория: <#FFF3E0>${sanitizeMiniMessageText(categoryName)}")
                    }
                }.map(parser::parse)
            )
            .build()
        (item.itemMeta as? SkullMeta)?.let { skull ->
            val profile = org.bukkit.Bukkit.createProfile(null as java.util.UUID?, null)
            profile.setProperty(com.destroystokyo.paper.profile.ProfileProperty("textures", entry.textureValue))
            skull.playerProfile = profile
            item.itemMeta = skull
        }
        protectedButton(item) { action(it) }
    }

    fun decorationHeadsGrayFiller(): Button = grayFillerButton()
    fun decorationHeadsBlackFiller(): Button = blackFillerButton()


    private fun sanitizeMiniMessageText(input: String): String =
        input
            .replace("§", "§\u200B")
                        .replace("<", "\\<")
                        .replace(">", "\\>")

    private fun allowMiniMessageColors(input: String): String =
        input.replace("§", "§\u200B")

    fun actionButton(
        material: Material,
        name: String,
        lore: List<String>,
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        action: ((ClickEvent) -> Unit)? = null
    ): Button {
        val builder = ItemBuilder(material)
            .name(parser.parse(name))
            .lore(lore.map(parser::parse))
        if (itemModifier != null) {
            builder.itemModifier()
        }
        return protectedButton(builder.build(), action ?: {})
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
        action: (ClickEvent) -> Unit
    ): Button {
        val lore = if (!active) {
            emptyLore
        } else {
            val selectedBlock = buildList {
                if (selectedEntriesLore.isNotEmpty()) {
                    add(selectedHeader)
                    addAll(selectedEntriesLore)
                    add("")
                }
            }
            emptyLore + listOf("") + selectedBlock
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
        onApply: (ClickEvent) -> Unit,
        onReset: (ClickEvent) -> Unit
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


    fun rawMiniMessageNameApplyButton(
        hasName: Boolean,
        preview: String,
        onApply: (ClickEvent) -> Unit,
        onReset: (ClickEvent) -> Unit
    ): Button {
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
        val activeLore = listOf("<!i><#C7A300>▍ <#FFF3E0>$preview", "") + usageLore

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
        focusedIndex: Int,
        hasMaterializedLore: Boolean,
        action: (ClickEvent, AdvancedLoreInteraction) -> Unit
    ): Button {
        val safeFocusedIndex = focusedIndex.coerceIn(0, virtualLines.lastIndex.coerceAtLeast(0))
        val loreLines = buildList {
            add("<!i><#FFD700>ЛКМ, <#FFE68A>следующая строка")
            add("<!i><#FFD700>ПКМ, <#FFE68A>изменить текущую")
            add("<!i><#FFD700>Q, <#FFE68A>очистить текущую")
            add("")
            virtualLines.forEachIndexed { index, line ->
                val prefix = if (index == safeFocusedIndex) {
                    "<!i><#00FF40>  » "
                } else {
                    "<!i><#C7A300> » "
                }
                add(if (line.isBlank()) prefix else "$prefix<#FFF3E0>$line ")
            }
            add("")
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
        action: (ClickEvent) -> Unit
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
        action: (ClickEvent) -> Unit
    ): Button {
        val item = editedItem.clone().let {
            if (it.type != Material.PLAYER_HEAD) {
                it.withType(Material.PLAYER_HEAD)
            } else {
                it
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
        return protectedButton(item, action)
    }

    fun <T> listButton(
        material: Material,
        options: List<ListButtonOption<T>>,
        selectedIndex: Int,
        titleBuilder: (ListButtonOption<T>, Int) -> String,
        beforeOptionsLore: List<String> = emptyList(),
        afterOptionsLore: List<String> = emptyList(),
        itemModifier: (ItemBuilder.(ListButtonOption<T>) -> ItemBuilder)? = null,
        action: (ClickEvent, Int) -> Unit
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

        return protectedButton(builder.build()) handler@{ event ->
                val newIndex = when {
                    event.isLeft || event.isShiftLeft -> (safeSelectedIndex + 1) % options.size
                    event.isRight || event.isShiftRight -> (safeSelectedIndex - 1 + options.size) % options.size
                    else -> return@handler
                }
                action(event, newIndex)
            }
    }

    fun focusedToggleListButton(
        material: Material,
        title: String,
        options: List<FocusedToggleListOption>,
        focusedIndex: Int,
        beforeOptionsLore: List<String> = emptyList(),
        afterOptionsLore: List<String> = emptyList(),
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null,
        action: (ClickEvent, FocusedToggleListInteraction) -> Unit
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
        return protectedButton(builder.build()) handler@{ event ->
            val interaction = when {
                event.isLeft || event.isShiftLeft -> FocusedToggleListInteraction.NEXT_FOCUS
                event.isRight || event.isShiftRight -> FocusedToggleListInteraction.TOGGLE_FOCUSED
                isDropClick(event) -> FocusedToggleListInteraction.RESET_ALL
                else -> return@handler
            }
            action(event, interaction)
        }
    }

    fun textOrderedColorFocusButton(
        material: Material,
        activeTitle: String,
        inactiveTitle: String,
        active: Boolean,
        options: List<TextColorFocusOption>,
        action: (ClickEvent, FocusedToggleListInteraction) -> Unit
    ): Button {
        val lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы идти дальше",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы переключить",
            "<!i><#FFD700>Q, <#FFE68A>чтобы всё сбросить",
            ""
        ) + options.map { option ->
            when {
                option.enabled && option.focused ->
                    "<!i><#00FF40>[<#00FF40>✔<#00FF40>]<#00FF40>  » <b><${option.colorTag}>${option.label}</b><white> <#C7A300>[<#FFD700>${option.order}<#C7A300>] "

                option.enabled && !option.focused ->
                    "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]<#C7A300><b> </b>» <b><${option.colorTag}>${option.label}</b><white> <#C7A300>[<#FFD700>${option.order}<#C7A300>] "

                !option.enabled && option.focused ->
                    "<!i><#00FF40>[<#FF1500>✘<#00FF40>]<#00FF40>  » <${option.colorTag}>${option.label} "

                else ->
                    "<!i><#FFF3E0>[<#FF1500>✘<#FFF3E0>]<#C7A300><b> </b>» <${option.colorTag}>${option.label} "
            }
        } + ""

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
        action: (ClickEvent, Int) -> Unit
    ): Button {
        require(options.isNotEmpty()) { "Text shadow options cannot be empty" }

        val selectedIndex = options.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
        val lore = listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
            ""
        ) + options.map { option ->
            val displayColorTag = if (option.colorTag == "ordinary") "white" else option.colorTag
            if (option.selected) {
                "<!i><#00FF40>  » <b><$displayColorTag>${option.label} "
            } else {
                "<!i><#C7A300><b> </b>» <$displayColorTag>${option.label} "
            }
        } + listOf("")

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

    private fun isDropClick(event: ClickEvent): Boolean {
        return event.type == ClickType.DROP || event.type == ClickType.CONTROL_DROP
    }

    fun editablePreviewButton(item: ItemStack): Button = protectedButton(item.clone()) { }

    fun headTextureValueInputSlotButton(
        valueBook: ItemStack?,
        action: (ClickEvent) -> Unit
    ): Button = itemInputSlotButton(
        storedItem = valueBook,
        placeholderName = "<!i><#FFD700>→ <#FFE68A>Слот для value <#FFD700>←",
        action = action
    )

    fun lockKeySlotButton(
        keyItem: ItemStack?,
        action: (ClickEvent) -> Unit
    ): Button = itemInputSlotButton(
        storedItem = keyItem,
        placeholderName = "<!i><#FFD700>→ <#FFE68A>Слот ключа<#FFD700> ←",
        action = action
    )

    fun useRemainderSlotButton(
        remainder: ItemStack?,
        action: (ClickEvent) -> Unit
    ): Button = itemInputSlotButton(
        storedItem = remainder,
        placeholderName = "<!i><#FFD700>→ <#FFE68A>Слот предмета<#FFD700> ←",
        action = action
    )

    private fun itemInputSlotButton(
        storedItem: ItemStack?,
        placeholderName: String,
        action: (ClickEvent) -> Unit
    ): Button {
        val buttonItem = storedItem?.clone()
            ?: ItemBuilder(Material.BARRIER)
                .name(parser.parse(placeholderName))
                .build()

        return protectedButton(buttonItem, action)
    }

    fun specialParameterButton(
        editedItem: ItemStack,
        viewer: Player,
        action: (ClickEvent) -> Unit = { }
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
                buildFrameInvisibilityButtonItem(editedItem)

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
                    .flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .build()

            else -> ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .name(parser.parse("<!i><#FFD700>Особый параметр"))
                .build()
        }
        return protectedButton(buttonItem, action)
    }

    private fun buildFrameInvisibilityButtonItem(editedItem: ItemStack): ItemStack {
        val enabled = FrameInvisibilitySupport.isEnabled(editedItem)
        return ItemBuilder(Material.ITEM_FRAME)
            .name(
                parser.parse(
                    if (enabled) {
                        "<!i><#C7A300>◎ <#FFD700>Невидимость рамки: <#00FF40>Вкл"
                    } else {
                        "<!i><#C7A300>⭘ <#FFD700>Невидимость рамки: <#FF1500>Выкл"
                    }
                )
            )
            .lore(listOf(parser.parse("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")))
            .apply {
                if (enabled) {
                    glint(true)
                }
            }
            .build()
    }


    fun potionRemoveEffectEntryButton(
        type: PotionEffectType,
        displayName: String,
        action: (ClickEvent) -> Unit
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

    fun potionApplyEffectEntryButton(
        index: Int,
        displayName: String,
        durationLabel: String,
        level: Int,
        chancePercent: String,
        showParticles: Boolean,
        showIcon: Boolean,
        type: PotionEffectType,
        action: (ClickEvent) -> Unit
    ): Button {
        val previewPotionType = PotionItemSupport.previewPotionType(type)
        return actionButton(
            material = Material.POTION,
            name = "<!i><#C7A300>◎ <#FFD700>Эффект №$index",
            lore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить",
                "",
                "<!i><#FFD700>Параметры:",
                "<!i><#C7A300> ● <#FFE68A>Шанс: <#00FF40>$chancePercent% ",
                "<!i><#C7A300> ● <#FFE68A>Название: <#FFF3E0>$displayName ",
                "<!i><#C7A300> ● <#FFE68A>Длительность: <#FFF3E0>$durationLabel ",
                "<!i><#C7A300> ● <#FFE68A>Уровень: <#FFF3E0>$level ",
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

    fun effectListAddByCommandButton(action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.LIME_DYE,
        name = "<!i><#00FF40>₪ Добавить эффект <#7BFF00>[Команда]",
        lore = emptyList(),
        action = action
    )

    fun effectListAddByMenuButton(action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.MAGENTA_DYE,
        name = "<!i><#FF00FF>₪ Добавить эффект <#FF66FF>[Меню]",
        lore = emptyList(),
        action = action
    )

    fun effectListClearAllButton(action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.RED_DYE,
        name = "<!i><#FF1500>⚠ Удалить всё",
        lore = emptyList(),
        action = action
    )

    fun visualEffectTypeEntryButton(
        displayName: String,
        material: Material,
        selected: Boolean,
        action: (ClickEvent) -> Unit
    ): Button = actionButton(
        material = material,
        name = if (selected) "<!i><#00FF40>$displayName" else "<!i><#FFD700>$displayName",
        lore = emptyList(),
        itemModifier = {
            zeroFoodPreview().invoke(this)
            hideEverythingExceptTooltip().invoke(this)
            hideJukeboxTooltip(material).invoke(this)
            if (selected) glint(true)
            this
        },
        action = action
    )

    fun visualEffectLevelButton(level: Int, action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.GLOWSTONE_DUST,
        name = if (level > 1) {
            "<!i><#C7A300>◎ <#FFD700>Уровень:<#FFF3E0> $level"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Уровень:<#FFF3E0> 1"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            if (level > 1) glint(true)
            this
        },
        action = action
    )

    fun visualEffectDurationButton(durationSeconds: Int, action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.CLOCK,
        name = when {
            durationSeconds == -1 -> "<!i><#C7A300>◎ <#FFD700>Длительность:<#FFF3E0> ∞"
            durationSeconds != 30 -> "<!i><#C7A300>◎ <#FFD700>Длительность:<#FFF3E0> $durationSeconds сек"
            else -> "<!i><#C7A300>⭘ <#FFD700>Длительность:<#FFF3E0> $durationSeconds сек"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            if (durationSeconds != 30) glint(true)
            this
        },
        action = action
    )

    fun visualEffectProbabilityButton(probabilityPercent: Int, action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.EMERALD,
        name = if (probabilityPercent < 100) {
            "<!i><#C7A300>⭘ <#FFD700>Шанс наложения:<#FFF3E0> $probabilityPercent%"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Шанс наложения:<#FFF3E0> 100%"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            if (probabilityPercent == 100) glint(true)
            this
        },
        action = action
    )

    fun visualEffectParticlesToggleButton(visible: Boolean, action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.ENDER_PEARL,
        name = if (visible) {
            "<!i><#C7A300>◎ <#FFD700>Партиклы: <#00FF40>Видны"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Партиклы: <#FF1500>Скрыты"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            if (visible) glint(true)
            this
        },
        action = action
    )

    fun visualEffectIconToggleButton(visible: Boolean, action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.ENDER_EYE,
        name = if (visible) {
            "<!i><#C7A300>◎ <#FFD700>Иконка в углу: <#00FF40>Видна"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Иконка в углу: <#FF1500>Скрыта"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить"),
        itemModifier = {
            if (visible) glint(true)
            this
        },
        action = action
    )

    fun visualEffectConfirmButton(action: (ClickEvent) -> Unit): Button = actionButton(
        material = Material.LIME_DYE,
        name = "<!i><#00FF40>✔ Подтвердить создание",
        lore = emptyList(),
        action = action
    )

    fun visualEffectSelectedPreviewButton(
        displayName: String,
        material: Material
    ): Button = actionButton(
        material = material,
        name = "<!i><#FFD700>$displayName",
        lore = emptyList(),
        itemModifier = {
            zeroFoodPreview().invoke(this)
            hideEverythingExceptTooltip().invoke(this)
            hideJukeboxTooltip(material).invoke(this)
            this
        }
    )

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
