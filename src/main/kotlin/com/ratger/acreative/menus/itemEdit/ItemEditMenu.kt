package com.ratger.acreative.menus.itemEdit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.AdvancedEditPageOne
import com.ratger.acreative.menus.itemEdit.pages.AdvancedEditPageTwo
import com.ratger.acreative.menus.itemEdit.pages.AttributeEditPage
import com.ratger.acreative.menus.itemEdit.pages.EnchantmentsActivePage
import com.ratger.acreative.menus.itemEdit.pages.EnchantmentsEditPage
import com.ratger.acreative.menus.itemEdit.pages.EquippableEditPage
import com.ratger.acreative.menus.itemEdit.pages.RootEditMenu
import com.ratger.acreative.menus.itemEdit.pages.RestrictionsListPage
import com.ratger.acreative.menus.itemEdit.pages.RestrictionsRootPage
import com.ratger.acreative.menus.itemEdit.pages.SimpleEditMenu
import com.ratger.acreative.menus.itemEdit.pages.ToolEditPage
import com.ratger.acreative.menus.itemEdit.pages.UseCooldownEditPage
import com.ratger.acreative.menus.itemEdit.pages.UseRemainderEditPage
import com.ratger.acreative.menus.itemEdit.pages.DecoratedPotPartDescriptor
import com.ratger.acreative.menus.itemEdit.pages.PotEditPage
import com.ratger.acreative.menus.itemEdit.pages.PotPatternSelectPage
import com.ratger.acreative.menus.itemEdit.pages.HeadTextureEditPage
import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.restrictions.RestrictionMode
import org.bukkit.Material
import org.bukkit.entity.Player

class ItemEditMenu(
    hooker: FunctionHooker,
    sessionManager: ItemEditSessionManager,
    buttonFactory: MenuButtonFactory,
    parser: MiniMessageParser,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val headMutationSupport: HeadTextureMutationSupport
) {
    private val support = ItemEditMenuSupport(hooker, sessionManager, buttonFactory, parser)

    private val openRootHandler: (Player, ItemEditSession) -> Unit = { player, session -> openRoot(player, session) }
    private val openSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openSimple(player, session) }
    private val openAdvancedPageOneHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAdvancedPageOne(player, session) }
    private val openAdvancedPageTwoHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAdvancedPageTwo(player, session) }
    private val openAttributePageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openAttributePage(player, session) }
    private val openEquippablePageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openEquippablePage(player, session) }
    private val openToolPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openToolPage(player, session) }
    private val openEnchantmentsFromSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openEnchantmentsFromSimple(player, session) }
    private val openEnchantmentsFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openEnchantmentsFromAdvanced(player, session) }
    private val openUseRemainderPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openUseRemainderPage(player, session) }
    private val openUseCooldownPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openUseCooldownPage(player, session) }
    private val openRestrictionsRootHandler: (Player, ItemEditSession) -> Unit = { player, session -> openRestrictionsRoot(player, session) }
    private val openSpecialParametersFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openSpecialParametersFromAdvanced(player, session) }

    private val rootPage: RootEditMenu = RootEditMenu(support, buttonFactory, openSimpleHandler, openAdvancedPageOneHandler)
    private val simplePage: SimpleEditMenu = SimpleEditMenu(support, buttonFactory, openRootHandler, openEnchantmentsFromSimpleHandler)
    private val advancedEditPageOne: AdvancedEditPageOne = AdvancedEditPageOne(
        support = support,
        buttonFactory = buttonFactory,
        openRoot = openRootHandler,
        openAdvancedPageTwo = openAdvancedPageTwoHandler,
        openSpecialParameters = openSpecialParametersFromAdvancedHandler,
        requestApplyInput = requestApplyInput
    )
    private val advancedPageTwo: AdvancedEditPageTwo =
        AdvancedEditPageTwo(
            support,
            buttonFactory,
            openAdvancedPageOneHandler,
            openAttributePageHandler,
            openEquippablePageHandler,
            openToolPageHandler,
            openEnchantmentsFromAdvancedHandler,
            openUseRemainderPageHandler,
            openUseCooldownPageHandler,
            openRestrictionsRootHandler
        )
    private val attributePage: AttributeEditPage =
        AttributeEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val equippablePage: EquippableEditPage =
        EquippableEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val useRemainderPage: UseRemainderEditPage =
        UseRemainderEditPage(support, buttonFactory, openAdvancedPageTwoHandler)
    private val useCooldownPage: UseCooldownEditPage =
        UseCooldownEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val toolPage: ToolEditPage =
        ToolEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val enchantmentsActivePage: EnchantmentsActivePage =
        EnchantmentsActivePage(support, buttonFactory, requestApplyInput)
    private val enchantmentsPage: EnchantmentsEditPage =
        EnchantmentsEditPage(support, buttonFactory, enchantmentsActivePage::open)
    private val restrictionsListPage: RestrictionsListPage =
        RestrictionsListPage(support, buttonFactory, openRestrictionsRootHandler, requestApplyInput)
    private val restrictionsRootPage: RestrictionsRootPage =
        RestrictionsRootPage(support, buttonFactory, openAdvancedPageTwoHandler, restrictionsListPage::open)
    private val potPatternSelectPage: PotPatternSelectPage =
        PotPatternSelectPage(support, buttonFactory)
    private val potEditPage: PotEditPage =
        PotEditPage(support, buttonFactory, this::openDecoratedPotPattern)
    private val headTextureEditPage: HeadTextureEditPage =
        HeadTextureEditPage(support, buttonFactory, headMutationSupport, openAdvancedPageOneHandler, requestApplyInput)

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

    fun openAttributePage(player: Player, session: ItemEditSession) {
        attributePage.open(player, session)
    }

    fun openEquippablePage(player: Player, session: ItemEditSession) {
        equippablePage.open(player, session)
    }

    fun openEnchantmentsFromSimple(player: Player, session: ItemEditSession) {
        enchantmentsPage.open(player, session, openSimpleHandler)
    }

    fun openEnchantmentsFromAdvanced(player: Player, session: ItemEditSession) {
        enchantmentsPage.open(player, session, openAdvancedPageTwoHandler)
    }

    fun openUseRemainderPage(player: Player, session: ItemEditSession) {
        useRemainderPage.open(player, session)
    }

    fun openUseCooldownPage(player: Player, session: ItemEditSession) {
        useCooldownPage.open(player, session)
    }

    fun openToolPage(player: Player, session: ItemEditSession) {
        toolPage.open(player, session)
    }

    fun openRestrictionsRoot(player: Player, session: ItemEditSession) {
        restrictionsRootPage.open(player, session)
    }

    fun openRestrictionsList(player: Player, session: ItemEditSession, mode: RestrictionMode, page: Int = 0) {
        restrictionsListPage.open(player, session, mode, page)
    }


    fun openHeadTexturePage(player: Player, session: ItemEditSession) {
        support.transition(session) {
            headTextureEditPage.open(player, session)
        }
    }

    fun openSpecialParametersFromAdvanced(player: Player, session: ItemEditSession) {
        if (session.editableItem.type == Material.DECORATED_POT) {
            openDecoratedPotRoot(player, session, openAdvancedPageOneHandler)
            return
        }
        if (session.editableItem.type == Material.PLAYER_HEAD) {
            headTextureEditPage.open(player, session)
            return
        }
        openAdvancedPageOne(player, session)
    }

    fun openDecoratedPotRoot(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        potEditPage.open(player, session, openBack)
    }

    fun openDecoratedPotPattern(
        player: Player,
        session: ItemEditSession,
        part: DecoratedPotPartDescriptor,
        openBack: (Player, ItemEditSession) -> Unit
    ) {
        potPatternSelectPage.open(player, session, part, openBack)
    }
}
