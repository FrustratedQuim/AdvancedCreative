package com.ratger.acreative.menus.banner

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.banner.model.BannedPatternEntry
import com.ratger.acreative.menus.banner.model.BannedUserEntry
import com.ratger.acreative.menus.banner.model.BannerColorDescriptor
import com.ratger.acreative.menus.banner.model.BannerPatternDescriptor
import com.ratger.acreative.menus.banner.model.PublishedBannerEntry
import com.ratger.acreative.menus.banner.service.BannerPatternSupport
import com.ratger.acreative.menus.banner.service.BannerTextSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent
import ru.violence.coreapi.bukkit.api.util.ItemBuilder

class BannerButtonFactory(
    private val parser: MiniMessageParser,
    private val shared: MenuButtonFactory
) {
    fun blackFiller(): Button = shared.blackFillerButton()
    fun grayFiller(): Button = shared.grayFillerButton()
    fun whiteFillerButton(): Button = shared.whiteFillerButton()
    fun backButton(action: () -> Unit): Button = shared.backButton { action() }
    fun forwardButton(action: () -> Unit): Button = shared.forwardButton { action() }

    fun mainMenuEditorButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.BRUSH,
        name = "<!i><#C7A300>⭐ <#FFD700>Редактор флага",
        lore = listOf("<!i><#FFD700> ● <#FFE68A>Аналог: <#FFF3E0>/bedit"),
        action = { action() }
    )

    fun mainMenuGalleryButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.BELL,
        name = "<!i><#C7A300>🔔 <#FFD700>Флаги игроков",
        lore = listOf("<!i><#FFD700> ● <#FFE68A>Аналог: <#FFF3E0>/db"),
        action = { action() }
    )

    fun mainMenuStorageButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.FIELD_MASONED_BANNER_PATTERN,
        name = "<!i><#C7A300>🛡 <#FFD700>Хранилище флагов",
        lore = listOf("<!i><#FFD700> ● <#FFE68A>Аналог: <#FFF3E0>/myflags"),
        action = { action() }
    )

    fun postTitleButton(title: String?, onApply: (ClickEvent) -> Unit, onReset: (ClickEvent) -> Unit): Button {
        val usageLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <текст> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            ""
        )
        val previewLore = title?.takeIf { it.isNotBlank() }?.let {
            listOf("<!i><#C7A300>▍ <#FFF3E0>${BannerTextSupport.escapeMiniMessage(it)}", "") + usageLore
        } ?: usageLore

        return shared.applyResetButton(
            material = Material.WRITABLE_BOOK,
            active = !title.isNullOrBlank(),
            activeName = "<!i><#C7A300>◎ <#FFD700>Название: <#00FF40>Задано",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Название: <#FF1500>Пусто",
            activeLore = previewLore,
            inactiveLore = usageLore,
            onApply = onApply,
            onReset = onReset
        )
    }

    fun postCategoryButton(options: List<String>, selectedIndex: Int, action: (ClickEvent, Int) -> Unit): Button =
        shared.listButton(
            material = Material.SPYGLASS,
            options = options.map { MenuButtonFactory.ListButtonOption(it, it) },
            selectedIndex = selectedIndex,
            titleBuilder = { _, _ -> "<!i><#C7A300>₪ <#FFD700>Категория" },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf("")
        ) { event, newIndex -> action(event, newIndex) }

    fun postConfirmButton(action: (ClickEvent) -> Unit): Button = shared.actionButton(
        material = Material.LIME_DYE,
        name = "<!i><#00FF40>✔ Подтвердить",
        lore = emptyList(),
        action = action
    )

    fun temporaryBarrierButton(title: String): Button = shared.actionButton(
        material = Material.BARRIER,
        name = title,
        lore = emptyList()
    )

    fun previewButton(item: ItemStack): Button = shared.itemAsIsButton(item.clone().apply { amount = 1 }) { }

    fun myFlagsButton(count: Int, action: () -> Unit): Button = shared.actionButton(
        material = Material.CHEST_MINECART,
        name = "<!i><#C7A300>⭐ <#FFD700>Мои флаги <#C7A300>[<#FFF3E0>$count<#C7A300>]",
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
        action = { action() }
    )

    fun filterButton(options: List<String>, selectedIndex: Int, action: (Int) -> Unit): Button =
        shared.listButton(
            material = Material.ENDER_EYE,
            options = options.map { MenuButtonFactory.ListButtonOption(it, it) },
            selectedIndex = selectedIndex,
            titleBuilder = { _, _ -> "<!i><#C7A300>✂ <#FFD700>Фильтр" },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf("")
        ) { _, newIndex -> action(newIndex) }

    fun categoryButton(options: List<String>, selectedIndex: Int, action: (Int) -> Unit): Button =
        shared.listButton(
            material = Material.CLOCK,
            options = options.map { MenuButtonFactory.ListButtonOption(it, it) },
            selectedIndex = selectedIndex,
            titleBuilder = { _, _ -> "<!i><#C7A300>⚡ <#FFD700>Категория" },
            beforeOptionsLore = listOf(
                "<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить",
                ""
            ),
            afterOptionsLore = listOf("")
        ) { _, newIndex -> action(newIndex) }

    fun searchButton(query: String?, action: (ClickEvent) -> Unit): Button = shared.actionButton(
        material = Material.COMPASS,
        name = if (query.isNullOrBlank()) {
            "<!i><#C7A300>🔎 <#FFD700>Поиск <#C7A300>[<#FFF3E0>Пусто<#C7A300>]"
        } else {
            "<!i><#C7A300>🔎 <#FFD700>Поиск <#C7A300>[<#FFF3E0>${BannerTextSupport.escapeMiniMessage(query)}<#C7A300>]"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы указать"),
        itemModifier = {
            if (!query.isNullOrBlank()) {
                glint(true)
            }
            this
        },
        action = action
    )

    fun postInfoButton(): Button = shared.actionButton(
        material = Material.BOOK,
        name = "<!i><#C7A300>ℹ <#FFD700>Хотите выложить свой флаг?",
        lore = listOf("<!i><#FFD700>▍ <#FFE68A>Используйте <#FFF3E0>/banner post"),
        action = {}
    )

    fun limitInfoButton(current: Int, limit: Int): Button = shared.actionButton(
        material = Material.BOOK,
        name = "<!i><#C7A300>ℹ <#FFD700>Лимит <#C7A300>[<#FFF3E0>$current/$limit<#C7A300>]",
        lore = emptyList(),
        action = {}
    )

    fun storageInfoButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.FIRE_CHARGE,
        name = "<!i><#FFD700>ℹ Что это такое?",
        lore = listOf(
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Это ваши <#FFD700>личные флаги,<#FFE68A> которые",
            "<!i>  <#FFE68A>можете видеть только вы.",
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Они <#FFD700>не удаляются<#FFE68A> спустя время,",
            "<!i>  <#FFE68A>всё ограничено лишь лимитом.",
            ""
        ),
        action = { action() }
    )

    fun storageEditInfoButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.EMERALD,
        name = "<!i><#FFD700>ℹ Что это такое?",
        lore = listOf(
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Измените содержимое так, как",
            "<!i>  <#FFD700>вам удобно<#FFE68A>, чтобы потом брать.",
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Можно вложить лишь <#FFD700>флаги,<#FFE68A> меню",
            "<!i>  <#FFE68A>ведь предназначено для них.",
            "",
            "<!i><#FFD700> ◆ <#FFE68A>Сохраняется лишь <#FFD700>рисунок<#FFE68A> и",
            "<!i>  <#FFD700>название<#FFE68A> флага до <#FFD700>64 символов.",
            ""
        ),
        action = { action() }
    )

    fun storageModeButton(editMode: Boolean, action: () -> Unit): Button = shared.actionButton(
        material = Material.BUNDLE,
        name = if (editMode) {
            "<!i><#C7A300>⭐ <#FFD700>Обычный режим"
        } else {
            "<!i><#C7A300>⭐ <#FFD700>Режим редактирования"
        },
        lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы переключить"),
        itemModifier = {
            if (editMode) {
                flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                glint(true)
            } else {
                flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            }
            this
        },
        action = { action() }
    )

    fun storageLimitButton(current: Int, limitText: String): Button = shared.actionButton(
        material = Material.BOOK,
        name = "<!i><#C7A300>ℹ <#FFD700>Лимит <#C7A300>[<#FFF3E0>$current/$limitText<#C7A300>]",
        lore = emptyList(),
        action = {}
    )

    fun storageStoredBannerButton(item: ItemStack, action: (ClickEvent) -> Unit): Button =
        shared.itemAsIsButton(item.clone().apply { amount = 1 }) { action(it) }

    fun publishedBannerButton(
        entry: PublishedBannerEntry,
        categoryName: String,
        showAuthor: Boolean,
        showDeleteHint: Boolean,
        moderationMode: Boolean,
        action: (ClickEvent) -> Unit
    ): Button {
        val title = entry.title?.takeIf { it.isNotBlank() } ?: BannerPatternSupport.localizedBaseName(entry.bannerItem)
        val lore = buildList {
            if (showAuthor) {
                add("<!i><#FFD700>▍ <#FFE68A>Автор: <#FFF3E0>${BannerTextSupport.escapeMiniMessage(entry.authorName)}")
            }
            add("<!i><#FFD700>▍ <#FFE68A>Категория: <#FFF3E0>${BannerTextSupport.escapeMiniMessage(categoryName)}")
            add("<!i><#FFD700>▍ <#FFE68A>Взято: <#FFF3E0>${BannerTextSupport.formatTakes(entry.takes)}")
            if (moderationMode) {
                add("")
                add("<!i><#FFD700>Нажмите, <#FFE68A>чтобы удалить")
            } else if (showDeleteHint) {
                add("")
                add("<!i><#FFD700>ЛКМ, <#FFE68A>чтобы взять")
                add("<!i><#FFD700>ПКМ, <#FFE68A>чтобы удалить")
            }
        }
        return shared.itemAsIsButton(decorateBanner(entry.bannerItem, title, lore)) { action(it) }
    }

    fun bannedPatternButton(entry: BannedPatternEntry, action: (ClickEvent) -> Unit): Button {
        val title = BannerPatternSupport.localizedBaseName(entry.bannerItem)
        val lore = listOf(
            "<!i><#FFD700>▍ <#FFE68A>Дата: <#FFF3E0>${BannerTextSupport.formatDate(entry.bannedAtEpochMillis)}",
            "",
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы разбанить"
        )
        return shared.itemAsIsButton(decorateBanner(entry.bannerItem, title, lore)) { action(it) }
    }

    fun bannedUserButton(entry: BannedUserEntry, action: (ClickEvent) -> Unit): Button {
        val head = BannerPatternSupport.createProfiledHead(entry.playerName, entry.profileSnapshot)
        val lore = buildList {
            add("<!i><#FFD700>▍ <#FFE68A>Дата: <#FFF3E0>${BannerTextSupport.formatDate(entry.bannedAtEpochMillis)}")
            entry.reason?.takeIf { it.isNotBlank() }?.let {
                add("<!i><#FFD700>▍ <#FFE68A>Пометка: <#FFF3E0>${BannerTextSupport.escapeMiniMessage(it)}")
            }
            add("")
            add("<!i><#FFD700>Нажмите, <#FFE68A>чтобы разбанить")
        }
        return shared.itemAsIsButton(decorateItem(head, entry.playerName, lore)) { action(it) }
    }

    fun airButton(): Button = shared.itemAsIsButton(ItemStack(Material.AIR)) { }

    fun editorInsertSlotButton(item: ItemStack?, action: (ClickEvent) -> Unit): Button {
        val buttonItem = item?.clone() ?: ItemBuilder(Material.BARRIER)
            .name(parser.parse("<!i><#FFD700>→ <#FFE68A>Вложите флаг или Нажмите <#FFD700>←"))
            .build()
        return shared.itemAsIsButton(buttonItem) { action(it) }
    }

    fun editorAddPatternButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.LIME_DYE,
        name = "<!i><#00FF40>₪ Добавить рисунок",
        lore = emptyList(),
        action = { action() }
    )

    fun editorClearPatternsButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.RED_DYE,
        name = "<!i><#FF1500>⚠ Удалить всё",
        lore = emptyList(),
        action = { action() }
    )

    fun editorPatternEntryButton(
        baseMaterial: Material,
        pattern: Pattern,
        displayName: String,
        action: (ClickEvent) -> Unit
    ): Button {
        val item = ItemStack(baseMaterial, 1)
        val meta = item.itemMeta as? BannerMeta
        if (meta != null) {
            meta.setPatterns(listOf(pattern))
            meta.displayName(parser.parse("<!i><#C7A300>◎ <#FFD700>${BannerTextSupport.escapeMiniMessage(displayName)}"))
            meta.lore(
                listOf(
                    parser.parse("<!i><#FFD700>ЛКМ, <#FFE68A>чтобы удалить"),
                    parser.parse("<!i><#FFD700>ПКМ, <#FFE68A>чтобы изменить"),
                    parser.parse("<!i><#FFD700>Q, <#FFE68A>чтобы переместить")
                )
            )
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            item.itemMeta = meta
        }
        return shared.itemAsIsButton(item) { action(it) }
    }

    fun baseBannerChoiceButton(color: BannerColorDescriptor, action: (ClickEvent) -> Unit): Button {
        val item = ItemBuilder(color.bannerMaterial)
            .name(parser.parse("<!i><#FFD700>${color.displayName} флаг"))
            .build()
        return shared.itemAsIsButton(item) { action(it) }
    }

    fun pickerPatternButton(baseMaterial: Material, descriptor: BannerPatternDescriptor, action: (ClickEvent) -> Unit): Button {
        val item = ItemStack(baseMaterial, 1)
        val meta = item.itemMeta as? BannerMeta
        if (meta != null) {
            meta.setPatterns(listOf(Pattern(DyeColor.WHITE, descriptor.patternType)))
            meta.displayName(parser.parse("<!i><#FFD700>${BannerTextSupport.escapeMiniMessage(descriptor.displayName)}"))
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            item.itemMeta = meta
        }
        return shared.itemAsIsButton(item) { action(it) }
    }

    fun pickerColorButton(color: BannerColorDescriptor, action: (ClickEvent) -> Unit): Button {
        val item = ItemBuilder(color.dyeMaterial)
            .name(parser.parse("<!i><#FFD700>${color.displayName}"))
            .build()
        return shared.itemAsIsButton(item) { action(it) }
    }

    fun pickerSelectedColorButton(color: BannerColorDescriptor): Button = shared.itemAsIsButton(
        ItemBuilder(color.dyeMaterial)
            .name(parser.parse("<!i><#FFD700>${color.displayName}"))
            .build()
    ) { }

    fun pickerSelectedPatternButton(descriptor: BannerPatternDescriptor): Button {
        val item = ItemStack(Material.BLACK_BANNER)
        val meta = item.itemMeta as? BannerMeta
        if (meta != null) {
            meta.setPatterns(listOf(Pattern(DyeColor.WHITE, descriptor.patternType)))
            meta.displayName(parser.parse("<!i><#FFD700>${BannerTextSupport.escapeMiniMessage(descriptor.displayName)}"))
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            item.itemMeta = meta
        }
        return shared.itemAsIsButton(item) { }
    }

    fun pickerConfirmButton(action: () -> Unit): Button = shared.actionButton(
        material = Material.LIME_DYE,
        name = "<!i><#00FF40>✔ Подтвердить",
        lore = emptyList(),
        action = { action() }
    )

    private fun decorateBanner(source: ItemStack, title: String, lore: List<String>): ItemStack {
        return decorateItem(source.clone().apply { amount = 1 }, title, lore)
    }

    private fun decorateItem(source: ItemStack, title: String, lore: List<String>): ItemStack {
        source.editMeta { meta ->
            meta.displayName(parser.parse("<!i><#FFD700>${BannerTextSupport.escapeMiniMessage(title)}"))
            meta.lore(lore.map(parser::parse))
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
        }
        return source
    }
}
