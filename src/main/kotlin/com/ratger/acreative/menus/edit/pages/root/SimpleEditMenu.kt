package com.ratger.acreative.menus.edit.pages.root

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.itemedit.effects.EdibleMenuSupport
import com.ratger.acreative.itemedit.effects.FoodComponentSupport
import com.ratger.acreative.itemedit.equippable.EquippableSupport
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class SimpleEditMenu(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openRoot: (Player, ItemEditSession) -> Unit,
    private val openEnchantments: (Player, ItemEditSession) -> Unit,
    private val openTextAppearance: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menuSize = 45
        val menu = support.buildMenu(
            title = "<!i>▍ Простой редактор",
            menuSize = menuSize,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 29, 30, 31, 32, 33),
            session = session
        )

        support.fillBase(menu, menuSize, support.rootBlackSlots)
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openRoot(player, session) } })
        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: Menu, player: Player, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(29, buildThrowableButton(player, session))
        menu.setButton(30, buildEdibleButton(player, session))
        menu.setButton(31, buildHeadEquippableButton(player, session))
        menu.setButton(32, buttonFactory.actionButton(
            Material.NAME_TAG,
            "<!i><#C7A300>✎ <#FFD700>Изменить название и описание",
            listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openTextAppearance(player, session) } }
        ))
        menu.setButton(33, buttonFactory.actionButton(
            material = Material.LAPIS_LAZULI,
            name = "<!i><#C7A300>⭐ <#FFD700>Параметры зачарований",
            lore = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы открыть"),
            action = { support.transition(session) { openEnchantments(player, session) } }
        ))
    }

    private fun buildThrowableButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.SNOWBALL,
        name = "<!i><#C7A300>☄ <#FFD700>Сделать предмет кидающимся",
        lore = actionLore(throwableConfigured(session)),
        itemModifier = {
            if (throwableConfigured(session)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (session.simpleThrowableApplied) return@actionButton
            val previousType = session.editableItem.type
            val converted = session.editableItem.withType(Material.SNOWBALL)
            val convertedMeta = converted.itemMeta ?: return@actionButton
            convertedMeta.itemModel = NamespacedKey.minecraft(previousType.key.key)
            converted.itemMeta = convertedMeta
            session.editableItem = converted
            session.simpleThrowableApplied = true
            refreshButtons(event.menu, player, session)
        }
    )

    private fun buildEdibleButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.APPLE,
        name = "<!i><#C7A300>🍖 <#FFD700>Сделать предмет съедобным",
        lore = actionLore(edibleConfigured(session)),
        itemModifier = {
            buttonFactory.zeroFoodPreview().invoke(this)
            if (edibleConfigured(session)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (session.simpleEdibleApplied) return@actionButton
            EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
            FoodComponentSupport.setCanAlwaysEat(session.editableItem, true)
            FoodComponentSupport.setSaturation(session.editableItem, 6f)
            FoodComponentSupport.setNutrition(session.editableItem, 5)
            session.simpleEdibleApplied = true
            refreshButtons(event.menu, player, session)
        }
    )

    private fun buildHeadEquippableButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.IRON_HELMET,
        name = "<!i><#C7A300>🔔 <#FFD700>Позволить надевать на голову",
        lore = actionLore(headEquippableConfigured(session)),
        itemModifier = {
            buttonFactory.hideAttributes().invoke(this)
            if (headEquippableConfigured(session)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (session.simpleHeadEquippableApplied) return@actionButton
            EquippableSupport.setSlot(session.editableItem, EquipmentSlot.HEAD)
            session.simpleHeadEquippableApplied = true
            refreshButtons(event.menu, player, session)
        }
    )

    private fun actionLore(applied: Boolean): List<String> {
        if (!applied) return listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить")
        return listOf(
            "<!i><#FFD700>Нажмите, <#FFE68A>чтобы совершить",
            "",
            "<!i><dark_green>▍ <#00FF40>Выполнено"
        )
    }

    private fun throwableConfigured(session: ItemEditSession): Boolean = session.simpleThrowableApplied

    private fun edibleConfigured(session: ItemEditSession): Boolean = session.simpleEdibleApplied

    private fun headEquippableConfigured(session: ItemEditSession): Boolean = session.simpleHeadEquippableApplied
}
