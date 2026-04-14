package com.ratger.acreative.menus.itemEdit.pages.trim

import com.ratger.acreative.itemedit.trim.ArmorTrimCatalog
import com.ratger.acreative.itemedit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.ItemEditMenuSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.pages.common.ItemEditPageLayouts
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.MenuRows

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

        menu.setButton(30, buttonFactory.armorTrimRootPatternButton(patternName) {
            support.transition(session) {
                openPatternSelect(player, session)
            }
        })

        menu.setButton(32, buttonFactory.armorTrimRootMaterialButton(materialName) {
            support.transition(session) {
                openMaterialSelect(player, session)
            }
        })

        menu.open(player)
    }
}
