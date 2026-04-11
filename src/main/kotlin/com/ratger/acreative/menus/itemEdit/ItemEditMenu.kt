package com.ratger.acreative.menus.itemEdit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.itemedit.container.LockItemSupport
import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.itemedit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.itemEdit.apply.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.AdvancedEditPageOne
import com.ratger.acreative.menus.itemEdit.pages.AdvancedEditPageTwo
import com.ratger.acreative.menus.itemEdit.pages.AttributeEditPage
import com.ratger.acreative.menus.itemEdit.pages.ArmorTrimEditPage
import com.ratger.acreative.menus.itemEdit.pages.ArmorTrimMaterialSelectPage
import com.ratger.acreative.menus.itemEdit.pages.ArmorTrimPatternSelectPage
import com.ratger.acreative.menus.itemEdit.pages.EnchantmentsActivePage
import com.ratger.acreative.menus.itemEdit.pages.EnchantmentsEditPage
import com.ratger.acreative.menus.itemEdit.pages.EquippableEditPage
import com.ratger.acreative.menus.itemEdit.pages.FoodApplyEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.FoodRemoveEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.FoodEditPage
import com.ratger.acreative.menus.itemEdit.pages.RootEditMenu
import com.ratger.acreative.menus.itemEdit.pages.RestrictionsListPage
import com.ratger.acreative.menus.itemEdit.pages.RestrictionsRootPage
import com.ratger.acreative.menus.itemEdit.pages.SimpleEditMenu
import com.ratger.acreative.menus.itemEdit.pages.TextAppearanceEditPageOne
import com.ratger.acreative.menus.itemEdit.pages.ToolEditPage
import com.ratger.acreative.menus.itemEdit.pages.UseCooldownEditPage
import com.ratger.acreative.menus.itemEdit.pages.UseRemainderEditPage
import com.ratger.acreative.menus.itemEdit.pages.DecoratedPotPartDescriptor
import com.ratger.acreative.menus.itemEdit.pages.DeathProtectionApplyEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.DeathProtectionEditPage
import com.ratger.acreative.menus.itemEdit.pages.DeathProtectionRemoveEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.PotEditPage
import com.ratger.acreative.menus.itemEdit.pages.PotPatternSelectPage
import com.ratger.acreative.menus.itemEdit.pages.HeadTextureEditPage
import com.ratger.acreative.menus.itemEdit.pages.LockEditPage
import com.ratger.acreative.menus.itemEdit.pages.PotionEditPage
import com.ratger.acreative.menus.itemEdit.pages.PotionEffectsActivePage
import com.ratger.acreative.menus.itemEdit.pages.MapEditPage
import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.head.HeadTextureValueBookSupport
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
    enum class LastEditorCategory {
        ROOT,
        SIMPLE,
        ADVANCED
    }

    private val support = ItemEditMenuSupport(hooker, sessionManager, buttonFactory, parser)
    private val headTextureValueBookSupport = HeadTextureValueBookSupport()
    private val textStyleService = ItemTextStyleService()
    private val lastCategoryByPlayer = mutableMapOf<java.util.UUID, LastEditorCategory>()

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
    private val openLockPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openLockPageInternal(player, session) }
    private val openRestrictionsRootHandler: (Player, ItemEditSession) -> Unit = { player, session -> openRestrictionsRoot(player, session) }
    private val openDeathProtectionPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openDeathProtectionPage(player, session) }
    private val openFoodPageFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openFoodPageFromAdvanced(player, session) }
    private val openFoodPageFromSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openFoodPageFromSimple(player, session) }
    private val openSpecialParametersFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openSpecialParametersFromAdvanced(player, session) }
    private val openPotionPageFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session ->
        openPotionPage(player, session, openAdvancedPageOneHandler)
    }
    private val openMapPageFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openMapPage(player, session) }
    private val openArmorTrimPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openArmorTrimPage(player, session) }
    private val openTextAppearanceFromSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openTextAppearanceFromSimple(player, session) }
    private val openTextAppearanceFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openTextAppearanceFromAdvanced(player, session) }

    private val rootPage: RootEditMenu = RootEditMenu(support, buttonFactory, openSimpleHandler, openAdvancedPageOneHandler)
    private val simplePage: SimpleEditMenu = SimpleEditMenu(
        support,
        buttonFactory,
        openRootHandler,
        openEnchantmentsFromSimpleHandler,
        openFoodPageFromSimpleHandler,
        openTextAppearanceFromSimpleHandler
    )
    private val advancedEditPageOne: AdvancedEditPageOne = AdvancedEditPageOne(
        support = support,
        buttonFactory = buttonFactory,
        openRoot = openRootHandler,
        openAdvancedPageTwo = openAdvancedPageTwoHandler,
        openSpecialParameters = openSpecialParametersFromAdvancedHandler,
        openTextAppearance = openTextAppearanceFromAdvancedHandler,
        requestApplyInput = requestApplyInput
    )
    private val textAppearancePageOne: TextAppearanceEditPageOne =
        TextAppearanceEditPageOne(support, buttonFactory, textStyleService, requestApplyInput)
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
            openRestrictionsRootHandler,
            openDeathProtectionPageHandler,
            openFoodPageFromAdvancedHandler
        )
    private val foodRemoveEffectsPage: FoodRemoveEffectsListPage =
        FoodRemoveEffectsListPage(support, buttonFactory, requestApplyInput, this::openFoodPage)
    private val foodApplyEffectsPage: FoodApplyEffectsListPage =
        FoodApplyEffectsListPage(support, buttonFactory, requestApplyInput, this::openFoodPage)
    private val foodPage: FoodEditPage =
        FoodEditPage(
            support = support,
            buttonFactory = buttonFactory,
            openRemoveEffectsPage = { player, session, openBack -> openFoodRemoveEffectsPage(player, session, openBack, 0) },
            openApplyEffectsPage = { player, session, openBack -> openFoodApplyEffectsPage(player, session, openBack, 0) },
            requestApplyInput = requestApplyInput
        )
    private val deathProtectionRemoveEffectsPage: DeathProtectionRemoveEffectsListPage =
        DeathProtectionRemoveEffectsListPage(support, buttonFactory, requestApplyInput, openDeathProtectionPageHandler)
    private val deathProtectionApplyEffectsPage: DeathProtectionApplyEffectsListPage =
        DeathProtectionApplyEffectsListPage(support, buttonFactory, requestApplyInput, openDeathProtectionPageHandler)
    private val deathProtectionPage: DeathProtectionEditPage =
        DeathProtectionEditPage(
            support = support,
            buttonFactory = buttonFactory,
            openAdvancedPageTwo = openAdvancedPageTwoHandler,
            openRemoveEffectsPage = { player, session -> openDeathProtectionRemoveEffectsPage(player, session, 0) },
            openApplyEffectsPage = { player, session -> openDeathProtectionApplyEffectsPage(player, session, 0) },
            requestApplyInput = requestApplyInput
        )
    private val attributePage: AttributeEditPage =
        AttributeEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val equippablePage: EquippableEditPage =
        EquippableEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val useRemainderPage: UseRemainderEditPage =
        UseRemainderEditPage(support, buttonFactory, openAdvancedPageTwoHandler)
    private val useCooldownPage: UseCooldownEditPage =
        UseCooldownEditPage(support, buttonFactory, openAdvancedPageTwoHandler, requestApplyInput)
    private val lockPage: LockEditPage =
        LockEditPage(support, buttonFactory, openAdvancedPageOneHandler)
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
        HeadTextureEditPage(support, buttonFactory, headMutationSupport, headTextureValueBookSupport, openAdvancedPageOneHandler, requestApplyInput)
    private val potionEditPage: PotionEditPage =
        PotionEditPage(support, buttonFactory, requestApplyInput, this::openPotionEffectsPage)
    private val potionEffectsPage: PotionEffectsActivePage =
        PotionEffectsActivePage(support, buttonFactory, requestApplyInput) { player, session ->
            openPotionPage(player, session, openAdvancedPageOneHandler)
        }
    private val mapEditPage: MapEditPage =
        MapEditPage(support, buttonFactory, requestApplyInput, openAdvancedPageOneHandler)

    private val armorTrimPatternSelectPage: ArmorTrimPatternSelectPage =
        ArmorTrimPatternSelectPage(support, buttonFactory)
    private val armorTrimMaterialSelectPage: ArmorTrimMaterialSelectPage =
        ArmorTrimMaterialSelectPage(support, buttonFactory)
    private val armorTrimEditPage: ArmorTrimEditPage =
        ArmorTrimEditPage(support, buttonFactory, this::openArmorTrimPatternPage, this::openArmorTrimMaterialPage)

    fun openRoot(player: Player, session: ItemEditSession) {
        rememberCategory(player, LastEditorCategory.ROOT)
        rootPage.open(player, session)
    }

    fun openSimple(player: Player, session: ItemEditSession) {
        rememberCategory(player, LastEditorCategory.SIMPLE)
        simplePage.open(player, session)
    }

    fun openAdvancedPageOne(player: Player, session: ItemEditSession) {
        rememberCategory(player, LastEditorCategory.ADVANCED)
        advancedEditPageOne.open(player, session)
    }

    fun openAdvancedPageTwo(player: Player, session: ItemEditSession) {
        rememberCategory(player, LastEditorCategory.ADVANCED)
        advancedPageTwo.open(player, session)
    }

    fun openLastCategoryOrDefault(player: Player, session: ItemEditSession) {
        when (lastCategoryByPlayer[player.uniqueId] ?: LastEditorCategory.ROOT) {
            LastEditorCategory.ROOT -> openRoot(player, session)
            LastEditorCategory.SIMPLE -> openSimple(player, session)
            LastEditorCategory.ADVANCED -> openAdvancedPageOne(player, session)
        }
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

    fun openLockPage(player: Player, session: ItemEditSession) {
        lockPage.open(player, session)
    }

    private fun openLockPageInternal(player: Player, session: ItemEditSession) {
        lockPage.open(player, session)
    }

    fun openToolPage(player: Player, session: ItemEditSession) {
        toolPage.open(player, session)
    }

    fun openRestrictionsRoot(player: Player, session: ItemEditSession) {
        restrictionsRootPage.open(player, session)
    }

    fun openFoodPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        foodPage.open(player, session, openBack)
    }

    fun openFoodPageFromAdvanced(player: Player, session: ItemEditSession) {
        openFoodPage(player, session, openAdvancedPageTwoHandler)
    }

    fun openFoodPageFromSimple(player: Player, session: ItemEditSession) {
        openFoodPage(player, session, openSimpleHandler)
    }

    fun openFoodRemoveEffectsPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int = 0) {
        foodRemoveEffectsPage.open(player, session, openBack, page)
    }

    fun openFoodApplyEffectsPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int = 0) {
        foodApplyEffectsPage.open(player, session, openBack, page)
    }

    fun openDeathProtectionPage(player: Player, session: ItemEditSession) {
        deathProtectionPage.open(player, session)
    }

    fun openDeathProtectionRemoveEffectsPage(player: Player, session: ItemEditSession, page: Int = 0) {
        deathProtectionRemoveEffectsPage.open(player, session, page)
    }

    fun openDeathProtectionApplyEffectsPage(player: Player, session: ItemEditSession, page: Int = 0) {
        deathProtectionApplyEffectsPage.open(player, session, page)
    }

    fun openRestrictionsList(player: Player, session: ItemEditSession, mode: RestrictionMode, page: Int = 0) {
        restrictionsListPage.open(player, session, mode, page)
    }


    fun openHeadTexturePage(player: Player, session: ItemEditSession) {
        support.transition(session) {
            headTextureEditPage.open(player, session)
        }
    }

    fun openPotionPage(player: Player, session: ItemEditSession, back: (Player, ItemEditSession) -> Unit = openAdvancedPageOneHandler) {
        potionEditPage.open(player, session, back)
    }

    fun openPotionEffectsPage(player: Player, session: ItemEditSession, page: Int = 0) {
        potionEffectsPage.open(player, session, page)
    }

    fun openMapPage(player: Player, session: ItemEditSession) {
        mapEditPage.open(player, session)
    }

    fun openTextAppearancePage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        textAppearancePageOne.open(player, session, openBack)
    }

    fun openTextAppearanceFromSimple(player: Player, session: ItemEditSession) {
        openTextAppearancePage(player, session, openSimpleHandler)
    }

    fun openTextAppearanceFromAdvanced(player: Player, session: ItemEditSession) {
        openTextAppearancePage(player, session, openAdvancedPageOneHandler)
    }


    fun openArmorTrimPage(player: Player, session: ItemEditSession) {
        armorTrimEditPage.open(player, session, openAdvancedPageOneHandler)
    }

    fun openArmorTrimPatternPage(player: Player, session: ItemEditSession) {
        armorTrimPatternSelectPage.open(player, session, this::openArmorTrimPage)
    }

    fun openArmorTrimMaterialPage(player: Player, session: ItemEditSession) {
        armorTrimMaterialSelectPage.open(player, session, this::openArmorTrimPage)
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
        if (ArmorTrimSupport.supports(session.editableItem)) {
            openArmorTrimPageHandler(player, session)
            return
        }
        if (LockItemSupport.supports(session.editableItem)) {
            openLockPageHandler(player, session)
            return
        }
        if (
            session.editableItem.type == Material.POTION ||
            session.editableItem.type == Material.SPLASH_POTION ||
            session.editableItem.type == Material.LINGERING_POTION ||
            session.editableItem.type == Material.TIPPED_ARROW
        ) {
            openPotionPageFromAdvancedHandler(player, session)
            return
        }
        if (session.editableItem.type == Material.FILLED_MAP) {
            openMapPageFromAdvancedHandler(player, session)
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

    private fun rememberCategory(player: Player, category: LastEditorCategory) {
        lastCategoryByPlayer[player.uniqueId] = category
    }
}
