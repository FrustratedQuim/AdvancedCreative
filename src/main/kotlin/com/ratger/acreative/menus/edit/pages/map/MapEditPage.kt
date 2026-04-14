package com.ratger.acreative.menus.edit.pages.map

import com.ratger.acreative.itemedit.color.ColorInputSupport
import com.ratger.acreative.itemedit.map.MapItemSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.pages.common.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

class MapEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val openBack: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Карта",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        val mapColor = MapItemSupport.color(session.editableItem)
        val hexColor = mapColor?.let(ColorInputSupport::normalizeHex)
        val activeColorLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <#hex> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
        val inactiveColorLore = activeColorLore.toMutableList().also {
            it[it.lastIndex - 1] = "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отменить "
        }
        menu.setButton(30, buttonFactory.applyResetButton(
            material = Material.BRUSH,
            active = hexColor != null,
            activeName = "<!i><#C7A300>◎ <#FFD700>Цвет: <$hexColor>$hexColor",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>Цвет: <#FF1500>Обычный",
            activeLore = activeColorLore,
            inactiveLore = inactiveColorLore,
            itemModifier = buttonFactory.hideAdditionalTooltip(),
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.MAP_COLOR) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = {
                MapItemSupport.setColor(session.editableItem, null)
                support.transition(session) { open(player, session) }
            }
        ))

        val currentMapId = MapItemSupport.mapId(session.editableItem)
        val activeIdLore = listOf(
            "<!i><#FFD700>ЛКМ, <#FFE68A>чтобы задать",
            "<!i><#FFD700>ПКМ, <#FFE68A>чтобы сбросить",
            "",
            "<!i><#FFD700>После нажатия:",
            "<!i><#C7A300> ● <#FFF3E0>/apply <число> <#C7A300>- <#FFE68A>задать ",
            "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отмена ",
            ""
        )
        val inactiveIdLore = activeIdLore.toMutableList().also {
            it[it.lastIndex - 1] = "<!i><#C7A300> ● <#FFF3E0>/apply cancel <#C7A300>- <#FFE68A>отменить "
        }
        menu.setButton(32, buttonFactory.applyResetButton(
            material = Material.FILLED_MAP,
            active = currentMapId != null,
            activeName = "<!i><#C7A300>◎ <#FFD700>ID карты: <#00FF40>${currentMapId ?: 0}",
            inactiveName = "<!i><#C7A300>⭘ <#FFD700>ID карты: <#FF1500>Нет",
            activeLore = activeIdLore,
            inactiveLore = inactiveIdLore,
            itemModifier = buttonFactory.hideAdditionalTooltip(),
            onApply = {
                support.transition(session) {
                    requestApplyInput(player, session, EditorApplyKind.MAP_ID) { reopenPlayer, reopenSession ->
                        open(reopenPlayer, reopenSession)
                    }
                }
            },
            onReset = {
                MapItemSupport.clearMapId(session.editableItem)
                support.transition(session) { open(player, session) }
            }
        ))

        menu.open(player)
    }
}
