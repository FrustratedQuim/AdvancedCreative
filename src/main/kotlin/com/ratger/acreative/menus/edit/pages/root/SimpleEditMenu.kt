package com.ratger.acreative.menus.edit.pages.root

import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.simple.SimpleEdibleToggleSupport
import com.ratger.acreative.menus.edit.simple.SimpleHeadEquippableToggleSupport
import com.ratger.acreative.menus.edit.simple.SimpleThrowableToggleSupport
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class SimpleEditMenu(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val textStyleService: ItemTextStyleService,
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

    private fun buildThrowableButton(player: Player, session: ItemEditSession) = buttonFactory.toggleButton(
        material = Material.SNOWBALL,
        enabled = throwableConfigured(session),
        enabledName = "<!i><#C7A300>◎ <#FFD700>Кидающийся предмет: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Кидающийся предмет: <#FF1500>Выкл",
        lore = TOGGLE_LORE,
        soundProfile = MenuSoundSupport.ButtonSoundProfile.NONE,
        action = { event ->
            SimpleThrowableToggleSupport.toggle(session, textStyleService)
            MenuSoundSupport.success(player)
            refreshButtons(event.menu, player, session)
        }
    )

    private fun buildEdibleButton(player: Player, session: ItemEditSession) = buttonFactory.toggleButton(
        material = Material.APPLE,
        enabled = edibleConfigured(session),
        enabledName = "<!i><#C7A300>◎ <#FFD700>Съедобность: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Съедобность: <#FF1500>Выкл",
        lore = TOGGLE_LORE,
        soundProfile = MenuSoundSupport.ButtonSoundProfile.NONE,
        itemModifier = {
            buttonFactory.zeroFoodPreview().invoke(this)
            this
        },
        action = { event ->
            SimpleEdibleToggleSupport.toggle(session)
            MenuSoundSupport.success(player)
            refreshButtons(event.menu, player, session)
        }
    )

    private fun buildHeadEquippableButton(player: Player, session: ItemEditSession) = buttonFactory.toggleButton(
        material = Material.IRON_HELMET,
        enabled = headEquippableConfigured(session),
        enabledName = "<!i><#C7A300>◎ <#FFD700>Надевание на голову: <#00FF40>Вкл",
        disabledName = "<!i><#C7A300>⭘ <#FFD700>Надевание на голову: <#FF1500>Выкл",
        lore = TOGGLE_LORE,
        soundProfile = MenuSoundSupport.ButtonSoundProfile.NONE,
        itemModifier = {
            buttonFactory.hideAttributes().invoke(this)
            this
        },
        action = { event ->
            SimpleHeadEquippableToggleSupport.toggle(session)
            MenuSoundSupport.success(player)
            refreshButtons(event.menu, player, session)
        }
    )

    private fun throwableConfigured(session: ItemEditSession): Boolean = SimpleThrowableToggleSupport.isEnabled(session)

    private fun edibleConfigured(session: ItemEditSession): Boolean = SimpleEdibleToggleSupport.isEnabled(session)

    private fun headEquippableConfigured(session: ItemEditSession): Boolean = SimpleHeadEquippableToggleSupport.isEnabled(session)

    private companion object {
        private val TOGGLE_LORE = listOf("<!i><#FFD700>Нажмите, <#FFE68A>чтобы изменить")
    }
}
