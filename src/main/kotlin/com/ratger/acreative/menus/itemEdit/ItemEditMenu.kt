package com.ratger.acreative.menus.itemEdit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.AdvancedEditPageOne
import com.ratger.acreative.menus.itemEdit.pages.AdvancedEditPageTwo
import com.ratger.acreative.menus.itemEdit.pages.RootEditMenu
import com.ratger.acreative.menus.itemEdit.pages.SimpleEditMenu
import org.bukkit.entity.Player

class ItemEditMenu(
    hooker: FunctionHooker,
    sessionManager: ItemEditSessionManager,
    buttonFactory: MenuButtonFactory,
    parser: MiniMessageParser,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit
) {
    private val support = ItemEditMenuSupport(hooker, sessionManager, buttonFactory, parser)

    private val openRootHandler: (Player, ItemEditSession) -> Unit = { player, session -> openRoot(player, session) }
    private val openSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openSimple(player, session) }
    private val openAdvancedPageOneHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAdvancedPageOne(player, session) }
    private val openAdvancedPageTwoHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAdvancedPageTwo(player, session) }

    private val rootPage: RootEditMenu = RootEditMenu(support, buttonFactory, openSimpleHandler, openAdvancedPageOneHandler)
    private val simplePage: SimpleEditMenu = SimpleEditMenu(support, buttonFactory, openRootHandler)
    private val advancedEditPageOne: AdvancedEditPageOne = AdvancedEditPageOne(
        support = support,
        buttonFactory = buttonFactory,
        openRoot = openRootHandler,
        openAdvancedPageTwo = openAdvancedPageTwoHandler,
        requestApplyInput = requestApplyInput
    )
    private val advancedPageTwo: AdvancedEditPageTwo =
        AdvancedEditPageTwo(support, buttonFactory, openAdvancedPageOneHandler)

    fun openRoot(player: Player, session: ItemEditSession) {
        rootPage.open(player, session)
    }

    fun openSimple(player: Player, session: ItemEditSession) {
        simplePage.open(player, session)
    }

    fun openAdvancedPageOne(player: Player, session: ItemEditSession) {
        advancedEditPageOne.open(player, session)
    }

    fun openAdvancedPageTwo(player: Player, session: ItemEditSession) {
        advancedPageTwo.open(player, session)
    }
}
