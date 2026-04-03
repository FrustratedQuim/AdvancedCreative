package com.ratger.acreative.menus

import com.ratger.acreative.itemedit.experimental.ComponentsService
import com.ratger.acreative.itemedit.head.PlayerProfileCopyHelper
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import org.bukkit.event.inventory.ClickType
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
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

    enum class FocusedToggleListInteraction {
        NEXT_FOCUS,
        TOGGLE_FOCUSED,
        RESET_ALL
    }

    companion object {
        val ADVANCED_RESTRICTIONS_ICON_MATERIAL: Material = Material.BARRIER
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
        action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent, FocusedToggleListInteraction) -> Unit
    ): Button {
        require(options.isNotEmpty()) { "Focused toggle list options cannot be empty" }
        val safeFocusedIndex = focusedIndex.coerceIn(0, options.lastIndex)
        val lore = beforeOptionsLore + buildFocusedToggleListLore(options, safeFocusedIndex) + afterOptionsLore
        return Button.simple(
            ItemBuilder(material)
                .name(parser.parse(title))
                .lore(lore.map(parser::parse))
                .build()
        ).action { event ->
            val interaction = when {
                event.isLeft || event.isShiftLeft -> FocusedToggleListInteraction.NEXT_FOCUS
                event.isRight || event.isShiftRight -> FocusedToggleListInteraction.TOGGLE_FOCUSED
                isDropClick(event) -> FocusedToggleListInteraction.RESET_ALL
                else -> return@action
            }
            action(event, interaction)
        }.build()
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
            val statePrefix = if (option.enabled) {
                "<!i><#FFF3E0>[<#00FF40>✔<#FFF3E0>]"
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

    fun specialParameterButton(editedItem: ItemStack, viewer: Player): Button {
        val type = editedItem.type
        val typeName = type.name
        val lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть")
        val buttonItem = when {
            typeName.endsWith("HELMET") || typeName.endsWith("CHESTPLATE") || typeName.endsWith("LEGGINGS") || typeName.endsWith("BOOTS") ->
                ItemBuilder(Material.NETHERITE_SCRAP)
                    .name(parser.parse("<!i><#C7A300>₪ <#FFD700>Отделка брони"))
                    .lore(lore.map(parser::parse))
                    .build()

            typeName.contains("SHULKER_BOX") ->
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
        return Button.simple(buttonItem).action { }.build()
    }

    fun hideAttributes(): ItemBuilder.() -> ItemBuilder = {
        edit { item ->
            val meta = item.itemMeta ?: return@edit
            meta.attributeModifiers = null
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            item.itemMeta = meta
        }
    }

    fun hideAdditionalTooltip(): ItemBuilder.() -> ItemBuilder = {
        flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
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
