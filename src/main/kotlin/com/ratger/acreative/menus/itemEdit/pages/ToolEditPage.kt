package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.tool.ToolComponentSupport
import com.ratger.acreative.itemedit.tool.ToolDamageSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.layout.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.Damageable
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class ToolEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Инструмент",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 29, 30, 32, 33),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))

        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: ru.violence.coreapi.bukkit.api.menu.Menu, player: Player, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(29, buildMaxDurabilityButton(player, session))
        menu.setButton(30, buildDamageButton(player, session))
        menu.setButton(32, buildMiningSpeedButton(player, session))
        menu.setButton(33, buildDamagePerBlockButton(player, session))
    }

    private fun buildMaxDurabilityButton(player: Player, session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val active = ToolDamageSupport.hasCustomMaxDamage(session.editableItem)
        val commonLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
        return buttonFactory.applyResetButton(
            material = Material.IRON_INGOT,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Максимальная прочность: <#00FF40>${ToolDamageSupport.customMaxDamage(session.editableItem)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Максимальная прочность: <#FF1500>Обычная",
            activeLore = commonLore,
            inactiveLore = commonLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.MAX_DURABILITY) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = { event ->
                val meta = session.editableItem.itemMeta as? Damageable ?: return@applyResetButton
                meta.setMaxDamage(null)
                session.editableItem.itemMeta = meta
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun buildDamageButton(player: Player, session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val active = !ToolDamageSupport.isDamageOrdinary(session.editableItem)
        val commonLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
            ""
        )
        return buttonFactory.applyResetButton(
            material = Material.GOLD_INGOT,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Повреждённость: <#00FF40>${ToolDamageSupport.currentDamage(session.editableItem)}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Повреждённость: <#FF1500>0",
            activeLore = commonLore,
            inactiveLore = commonLore,
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.DAMAGE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = { event ->
                val meta = session.editableItem.itemMeta as? Damageable ?: return@applyResetButton
                meta.resetDamage()
                session.editableItem.itemMeta = meta
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun buildMiningSpeedButton(player: Player, session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val active = !ToolComponentSupport.isMiningSpeedOrdinary(session.editableItem)
        val commonLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
        return buttonFactory.applyResetButton(
            material = Material.GOLDEN_PICKAXE,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Скорость копания: <#00FF40>${formatFloat(ToolComponentSupport.effectiveMiningSpeed(session.editableItem))}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Скорость копания: <#FF1500>Обычная",
            activeLore = commonLore,
            inactiveLore = commonLore,
            itemModifier = buttonFactory.hideAttributes(),
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.MINING_SPEED) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = { event ->
                ToolComponentSupport.resetMiningSpeed(session.editableItem)
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun buildDamagePerBlockButton(player: Player, session: ItemEditSession): ru.violence.coreapi.bukkit.api.menu.button.Button {
        val active = !ToolComponentSupport.isDamagePerBlockOrdinary(session.editableItem)
        val commonLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Количество единиц прочности ",
            "<!i><#C7A300> ● <#FFE68A>за поломку <#FFF3E0>1-го <#FFE68A>блока. ",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум ",
            ""
        )
        return buttonFactory.applyResetButton(
            material = Material.STONE_PICKAXE,
            active = active,
            activeName = "<!i><#C7A300>◎ <#FFD700>Повреждение за блок: <#00FF40>${ToolComponentSupport.effectiveDamagePerBlock(session.editableItem) ?: 0}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Повреждение за блок: <#FF1500>Обычное",
            activeLore = commonLore,
            inactiveLore = commonLore,
            itemModifier = buttonFactory.hideAttributes(),
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.DAMAGE_PER_BLOCK) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = { event ->
                ToolComponentSupport.resetDamagePerBlock(session.editableItem)
                refreshButtons(event.menu, player, session)
            }
        )
    }

    private fun formatFloat(value: Float?): String {
        val number = value ?: 0f
        return if (number % 1f == 0f) number.toInt().toString() else number.toString()
    }

}
