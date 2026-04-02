package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import org.bukkit.entity.Player

class ItemEditMenu(
    hooker: FunctionHooker,
    sessionManager: ItemEditSessionManager,
    buttonFactory: MenuButtonFactory,
    parser: MiniMessageParser
) {
    private val support = ItemEditMenuSupport(hooker, sessionManager, buttonFactory, parser)

    private val openRootHandler: (Player, ItemEditSession) -> Unit = { player, session -> openRoot(player, session) }
    private val openSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openSimple(player, session) }
    private val openAdvancedPageOneHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAdvancedPageOne(player, session) }
    private val openAdvancedPageTwoHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAdvancedPageTwo(player, session) }

    private val rootPage: RootItemEditMenuPage = RootItemEditMenuPage(support, buttonFactory, openSimpleHandler, openAdvancedPageOneHandler)
    private val simplePage: SimpleItemEditMenuPage = SimpleItemEditMenuPage(support, buttonFactory, openRootHandler)
    private val advancedPageOne: AdvancedItemEditMenuPageOne = AdvancedItemEditMenuPageOne(support, buttonFactory, openRootHandler, openAdvancedPageTwoHandler)
    private val advancedPageTwo: AdvancedItemEditMenuPageTwo = AdvancedItemEditMenuPageTwo(support, buttonFactory, openAdvancedPageOneHandler)

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
