package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.apply.EditorApplyKind
import com.ratger.acreative.menus.pages.AdvancedItemEditMenuPageOne
import com.ratger.acreative.menus.pages.AdvancedItemEditMenuPageTwo
import com.ratger.acreative.menus.pages.RootItemEditMenu
import com.ratger.acreative.menus.pages.SimpleItemEditMenu
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

    private val rootPage: RootItemEditMenu = RootItemEditMenu(support, buttonFactory, openSimpleHandler, openAdvancedPageOneHandler)
    private val simplePage: SimpleItemEditMenu = SimpleItemEditMenu(support, buttonFactory, openRootHandler)
    private val advancedPageOne: AdvancedItemEditMenuPageOne = AdvancedItemEditMenuPageOne(
        support = support,
        buttonFactory = buttonFactory,
        openRoot = openRootHandler,
        openAdvancedPageTwo = openAdvancedPageTwoHandler,
        requestApplyInput = requestApplyInput
    )
    private val advancedPageTwo: AdvancedItemEditMenuPageTwo =
        AdvancedItemEditMenuPageTwo(support, buttonFactory, openAdvancedPageOneHandler)

    fun openRoot(player: Player, session: ItemEditSession) {
        rootPage.open(player, session)
    }

    fun openSimple(player: Player, session: ItemEditSession) {
        simplePage.open(player, session)
    }

    fun openAdvancedPageOne(player: Player, session: ItemEditSession) {
        advancedPageOne.open(player, session)
    }

    fun openAdvancedPageTwo(player: Player, session: ItemEditSession) {
        advancedPageTwo.open(player, session)
    }
}
