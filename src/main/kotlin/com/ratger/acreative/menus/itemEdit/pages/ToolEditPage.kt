package com.ratger.acreative.menus.itemEdit.pages

import com.ratger.acreative.itemedit.tool.ToolComponentSupport
import com.ratger.acreative.itemedit.tool.ToolDamageSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
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
    private val blackSlots = setOf(0, 8, 9, 17, 18, 26, 27, 35, 36, 44, 12, 14)

    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Инструмент",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 29, 30, 32, 33),
            session = session
        )

        support.fillBase(menu, 45, blackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openAdvancedPageTwo(player, session) } })
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))

        refreshButtons(menu, player, session)
        menu.open(player)
    }

    private fun refreshButtons(menu: ru.violence.coreapi.bukkit.api.menu.Menu, player: Player, session: ItemEditSession) {
        menu.setButton(support.editableSlot, buttonFactory.editablePreviewButton(session.editableItem))
        menu.setButton(29, buildMaxDurabilityButton(player, session))
        menu.setButton(30, buildDamageButton(player, session))
        if (!ToolComponentSupport.supportsToolEditing(session.editableItem)) {
            menu.setButton(32, buildUnsupportedToolButton())
            menu.setButton(33, buildUnsupportedToolButton())
        } else {
            menu.setButton(32, buildMiningSpeedButton(player, session))
            menu.setButton(33, buildDamagePerBlockButton(player, session))
        }
    }

    private fun buildMaxDurabilityButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.IRON_INGOT,
        name = if (ToolDamageSupport.hasCustomMaxDamage(session.editableItem)) {
            "<!i><#C7A300>◎ <#FFD700>Максимальная прочность: <#00FF40>${ToolDamageSupport.customMaxDamage(session.editableItem)}"
        } else {
            "<!i><#C7A300>⭘ <#FFD700>Максимальная прочность: <#FF1500>Обычная"
        },
        lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            ""
        ),
        itemModifier = {
            if (ToolDamageSupport.hasCustomMaxDamage(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (event.isRight || event.isShiftRight) {
                val meta = session.editableItem.itemMeta as? Damageable ?: return@actionButton
                meta.setMaxDamage(null)
                session.editableItem.itemMeta = meta
                refreshButtons(event.menu, player, session)
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.MAX_DURABILITY) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }
    )

    private fun buildDamageButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.GOLD_INGOT,
        name = if (ToolDamageSupport.isDamageOrdinary(session.editableItem)) {
            "<!i><#C7A300>⭘ <#FFD700>Повреждённость: <#FF1500>0"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Повреждённость: <#00FF40>${ToolDamageSupport.currentDamage(session.editableItem)}"
        },
        lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум",
            ""
        ),
        itemModifier = {
            if (!ToolDamageSupport.isDamageOrdinary(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (event.isRight || event.isShiftRight) {
                val meta = session.editableItem.itemMeta as? Damageable ?: return@actionButton
                meta.resetDamage()
                session.editableItem.itemMeta = meta
                refreshButtons(event.menu, player, session)
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.DAMAGE) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }
    )

    private fun buildMiningSpeedButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.GOLDEN_PICKAXE,
        name = if (ToolComponentSupport.isMiningSpeedOrdinary(session.editableItem)) {
            "<!i><#C7A300>⭘ <#FFD700>Скорость копания: <#FF1500>Обычная"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Скорость копания: <#00FF40>${formatFloat(ToolComponentSupport.effectiveMiningSpeed(session.editableItem))}"
        },
        lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена",
            ""
        ),
        itemModifier = {
            buttonFactory.hideAttributes().invoke(this)
            if (!ToolComponentSupport.isMiningSpeedOrdinary(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (event.isRight || event.isShiftRight) {
                ToolComponentSupport.resetMiningSpeed(session.editableItem)
                refreshButtons(event.menu, player, session)
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.MINING_SPEED) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }
    )

    private fun buildDamagePerBlockButton(player: Player, session: ItemEditSession) = buttonFactory.actionButton(
        material = Material.STONE_PICKAXE,
        name = if (ToolComponentSupport.isDamagePerBlockOrdinary(session.editableItem)) {
            "<!i><#C7A300>⭘ <#FFD700>Повреждение за блок: <#FF1500>Обычное"
        } else {
            "<!i><#C7A300>◎ <#FFD700>Повреждение за блок: <#00FF40>${ToolComponentSupport.effectiveDamagePerBlock(session.editableItem) ?: 0}"
        },
        lore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>Назначение:",
            "<!i><#C7A300> ● <#FFE68A>Количество единиц прочности",
            "<!i><#C7A300> ● <#FFE68A>за поломку <#FFF3E0>1-го <#FFE68A>блока.",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать",
            "<!i><#C7A300> ● <#FFF3E0>/apply max <#C7A300>- <#FFE68A>максимум",
            ""
        ),
        itemModifier = {
            buttonFactory.hideAttributes().invoke(this)
            if (!ToolComponentSupport.isDamagePerBlockOrdinary(session.editableItem)) {
                glint(true)
            }
            this
        },
        action = { event ->
            if (event.isRight || event.isShiftRight) {
                ToolComponentSupport.resetDamagePerBlock(session.editableItem)
                refreshButtons(event.menu, player, session)
            } else if (event.isLeft || event.isShiftLeft) {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.DAMAGE_PER_BLOCK) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            }
        }
    )

    private fun formatFloat(value: Float?): String {
        val number = value ?: 0f
        return if (number % 1f == 0f) number.toInt().toString() else number.toString()
    }

    private fun buildUnsupportedToolButton() = buttonFactory.actionButton(
        material = Material.BARRIER,
        name = "<!i><#FF1500>⚠ Доступно только для инструмента",
        lore = emptyList()
    )
}
