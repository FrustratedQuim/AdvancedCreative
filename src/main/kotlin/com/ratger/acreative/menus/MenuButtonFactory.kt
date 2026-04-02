package com.ratger.acreative.menus

import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.itemedit.head.PlayerProfileCopyHelper
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.FoodProperties
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.util.ItemBuilder

class MenuButtonFactory(
    private val parser: MiniMessageParser
) {
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
        itemModifier: (ItemBuilder.() -> ItemBuilder)? = null
    ): Button {
        val builder = ItemBuilder(material)
            .name(parser.parse(name))
            .lore(lore.map(parser::parse))
        if (itemModifier != null) {
            builder.itemModifier()
        }
        return Button.simple(builder.build()).action { }.build()
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
        clearAttributes()
    }

    fun hideAdditionalTooltip(): ItemBuilder.() -> ItemBuilder = {
        flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
    }

    fun zeroFoodPreview(): ItemBuilder.() -> ItemBuilder = {
        edit { item ->
            if (item.getData(DataComponentTypes.CONSUMABLE) == null) {
                item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().build())
            }
            item.setData(
                DataComponentTypes.FOOD,
                FoodProperties.food().nutrition(0).saturation(0f).build()
            )
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
        clearAttributes()
    }
}
