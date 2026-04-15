package com.ratger.acreative.menus.edit.pages.trim

import com.ratger.acreative.menus.edit.trim.ArmorTrimCatalog
import com.ratger.acreative.menus.edit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.ItemEditMenuSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.pages.common.ItemEditPageLayouts
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.event.ClickEvent

class ArmorTrimEditPage(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val openPatternSelect: (Player, ItemEditSession) -> Unit,
    private val openMaterialSelect: (Player, ItemEditSession) -> Unit
) {
    fun open(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        val menu = support.buildMenu(
            title = "<!i>▍ Редактор → Отделка брони",
            menuSize = 45,
            rows = MenuRows.FIVE,
            interactiveTopSlots = setOf(18, 30, 32),
            session = session
        )

        support.fillBase(menu, 45, ItemEditPageLayouts.standardEditorBlackSlots)
        menu.setButton(18, buttonFactory.backButton { support.transition(session) { openBack(player, session) } })
        menu.setButton(13, buttonFactory.editablePreviewButton(session.editableItem))

        val currentPattern = ArmorTrimSupport.currentPattern(session.editableItem)
        val currentMaterial = ArmorTrimSupport.currentMaterial(session.editableItem)
        val patternName = currentPattern?.let { ArmorTrimCatalog.patternByKey[it]?.displayName }
        val materialName = currentMaterial?.let { ArmorTrimCatalog.materialByKey[it]?.displayName }

        menu.setButton(30, buildPatternButton(patternName) {
            support.transition(session) {
                openPatternSelect(player, session)
            }
        })

        val materialIcon = currentMaterial
            ?.let { ArmorTrimCatalog.materialByKey[it]?.icon }
            ?: Material.STRUCTURE_VOID
        menu.setButton(32, buildMaterialButton(materialName, materialIcon) {
            support.transition(session) {
                openMaterialSelect(player, session)
            }
        })

        menu.open(player)
    }

    private fun buildPatternButton(patternDisplayName: String?, action: (ClickEvent) -> Unit) =
        buttonFactory.actionButton(
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
                    buttonFactory.hideAdditionalTooltip().invoke(this)
                }
                this
            },
            action = action
        )

    private fun buildMaterialButton(materialDisplayName: String?, icon: Material, action: (ClickEvent) -> Unit) =
        buttonFactory.actionButton(
            material = icon,
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
}
