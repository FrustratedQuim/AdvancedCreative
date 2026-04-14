package com.ratger.acreative.menus.itemEdit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.itemedit.container.LockItemSupport
import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import com.ratger.acreative.itemedit.head.HeadTextureValueBookSupport
import com.ratger.acreative.itemedit.invisibility.FrameInvisibilitySupport
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.itemedit.text.ItemTextStyleService
import com.ratger.acreative.itemedit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.itemEdit.pages.advanced.AdvancedEditDetailsPage
import com.ratger.acreative.menus.itemEdit.pages.advanced.AdvancedEditMainPage
import com.ratger.acreative.menus.itemEdit.pages.appearance.TextAppearanceContentPage
import com.ratger.acreative.menus.itemEdit.pages.appearance.TextAppearanceStylePage
import com.ratger.acreative.menus.itemEdit.pages.attributes.AttributeEditPage
import com.ratger.acreative.menus.itemEdit.pages.attributes.EquippableEditPage
import com.ratger.acreative.menus.itemEdit.pages.effects.DeathProtectionApplyEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.effects.DeathProtectionEditPage
import com.ratger.acreative.menus.itemEdit.pages.effects.DeathProtectionRemoveEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.effects.FoodApplyEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.effects.FoodEditPage
import com.ratger.acreative.menus.itemEdit.pages.effects.FoodRemoveEffectsListPage
import com.ratger.acreative.menus.itemEdit.pages.enchantments.EnchantmentsActivePage
import com.ratger.acreative.menus.itemEdit.pages.enchantments.EnchantmentsEditPage
import com.ratger.acreative.menus.itemEdit.pages.head.HeadTextureEditPage
import com.ratger.acreative.menus.itemEdit.pages.map.MapEditPage
import com.ratger.acreative.menus.itemEdit.pages.pot.DecoratedPotPartDescriptor
import com.ratger.acreative.menus.itemEdit.pages.pot.PotEditPage
import com.ratger.acreative.menus.itemEdit.pages.pot.PotPatternSelectPage
import com.ratger.acreative.menus.itemEdit.pages.potion.PotionEditPage
import com.ratger.acreative.menus.itemEdit.pages.potion.PotionEffectsActivePage
import com.ratger.acreative.menus.itemEdit.pages.restrictions.RestrictionsListPage
import com.ratger.acreative.menus.itemEdit.pages.restrictions.RestrictionsRootPage
import com.ratger.acreative.menus.itemEdit.pages.root.RootEditMenu
import com.ratger.acreative.menus.itemEdit.pages.root.SimpleEditMenu
import com.ratger.acreative.menus.itemEdit.pages.tooling.LockEditPage
import com.ratger.acreative.menus.itemEdit.pages.tooling.ToolEditPage
import com.ratger.acreative.menus.itemEdit.pages.tooling.UseCooldownEditPage
import com.ratger.acreative.menus.itemEdit.pages.tooling.UseRemainderEditPage
import com.ratger.acreative.menus.itemEdit.pages.trim.ArmorTrimEditPage
import com.ratger.acreative.menus.itemEdit.pages.trim.ArmorTrimMaterialSelectPage
import com.ratger.acreative.menus.itemEdit.pages.trim.ArmorTrimPatternSelectPage
import org.bukkit.Material
import org.bukkit.entity.Player

class ItemEditMenu(
    hooker: FunctionHooker,
    sessionManager: ItemEditSessionManager,
    buttonFactory: MenuButtonFactory,
    private val parser: MiniMessageParser,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    headMutationSupport: HeadTextureMutationSupport,
    textStyleService: ItemTextStyleService
) {
    enum class LastEditorCategory {
        ROOT,
        SIMPLE,
        ADVANCED
    }

    private val support = ItemEditMenuSupport(hooker, sessionManager, buttonFactory, parser)
    private val headTextureValueBookSupport = HeadTextureValueBookSupport()
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
    private val openSpecialParametersFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openSpecialParametersFromAdvanced(player, session) }
    private val openPotionPageFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session ->
        openPotionPage(player, session, openAdvancedPageOneHandler)
    }
    private val openMapPageFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openMapPage(player, session) }
    private val openArmorTrimPageHandler: (Player, ItemEditSession) -> Unit = { player, session -> openArmorTrimPage(player, session) }
    private val openTextAppearanceFromSimpleHandler: (Player, ItemEditSession) -> Unit = { player, session -> openTextAppearanceFromSimple(player, session) }
    private val openTextAppearanceFromAdvancedHandler: (Player, ItemEditSession) -> Unit = { player, session -> openTextAppearanceFromAdvanced(player, session) }

    private val rootPage: RootEditMenu =
        RootEditMenu(support, buttonFactory, openSimpleHandler, openAdvancedPageOneHandler)
    private val simplePage: SimpleEditMenu = SimpleEditMenu(
        support,
        buttonFactory,
        openRootHandler,
        openEnchantmentsFromSimpleHandler,
        openTextAppearanceFromSimpleHandler
    )
    private val advancedEditPageOne: AdvancedEditMainPage = AdvancedEditMainPage(
        support = support,
        buttonFactory = buttonFactory,
        openRoot = openRootHandler,
        openAdvancedPageTwo = openAdvancedPageTwoHandler,
        openSpecialParameters = openSpecialParametersFromAdvancedHandler,
        openTextAppearance = openTextAppearanceFromAdvancedHandler,
        requestApplyInput = requestApplyInput
    )
    private val textAppearancePageTwo: TextAppearanceStylePage =
        TextAppearanceStylePage(
            support,
            buttonFactory,
            textStyleService,
            requestApplyInput,
            this::openTextAppearancePageOne
        )
    private val textAppearancePageOne: TextAppearanceContentPage =
        TextAppearanceContentPage(
            support,
            buttonFactory,
            textStyleService,
            requestApplyInput,
            this::openTextAppearancePageTwo
        )
    private val advancedPageTwo: AdvancedEditDetailsPage =
        AdvancedEditDetailsPage(
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
            openRemoveEffectsPage = { player, session, openBack ->
                openFoodRemoveEffectsPage(
                    player,
                    session,
                    openBack,
                    0
                )
            },
            openApplyEffectsPage = { player, session, openBack ->
                openFoodApplyEffectsPage(
                    player,
                    session,
                    openBack,
                    0
                )
            },
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
        HeadTextureEditPage(
            support,
            buttonFactory,
            headMutationSupport,
            headTextureValueBookSupport,
            openAdvancedPageOneHandler,
            requestApplyInput
        )
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
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.ROOT)
            rootPage.open(player, session)
        }
    }

    fun openSimple(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.SIMPLE)
            simplePage.open(player, session)
        }
    }

    fun openAdvancedPageOne(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.ADVANCED)
            advancedEditPageOne.open(player, session)
        }
    }

    fun openAdvancedPageTwo(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.ADVANCED)
            advancedPageTwo.open(player, session)
        }
    }

    fun openLastCategoryOrDefault(player: Player, session: ItemEditSession) {
        when (lastCategoryByPlayer[player.uniqueId] ?: LastEditorCategory.ROOT) {
            LastEditorCategory.ROOT -> openRoot(player, session)
            LastEditorCategory.SIMPLE -> openSimple(player, session)
            LastEditorCategory.ADVANCED -> openAdvancedPageOne(player, session)
        }
    }

    fun openAttributePage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { attributePage.open(player, session) }
    }

    fun openEquippablePage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { equippablePage.open(player, session) }
    }

    fun openEnchantmentsFromSimple(player: Player, session: ItemEditSession) {
        openPageSafely(player) { enchantmentsPage.open(player, session, openSimpleHandler) }
    }

    fun openEnchantmentsFromAdvanced(player: Player, session: ItemEditSession) {
        openPageSafely(player) { enchantmentsPage.open(player, session, openAdvancedPageTwoHandler) }
    }

    fun openUseRemainderPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { useRemainderPage.open(player, session) }
    }

    fun openUseCooldownPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { useCooldownPage.open(player, session) }
    }

    private fun openLockPageInternal(player: Player, session: ItemEditSession) {
        openPageSafely(player) { lockPage.open(player, session) }
    }

    fun openToolPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { toolPage.open(player, session) }
    }

    fun openRestrictionsRoot(player: Player, session: ItemEditSession) {
        openPageSafely(player) { restrictionsRootPage.open(player, session) }
    }

    fun openFoodPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { foodPage.open(player, session, openBack) }
    }

    fun openFoodPageFromAdvanced(player: Player, session: ItemEditSession) {
        openFoodPage(player, session, openAdvancedPageTwoHandler)
    }

    fun openFoodRemoveEffectsPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int = 0) {
        openPageSafely(player) { foodRemoveEffectsPage.open(player, session, openBack, page) }
    }

    fun openFoodApplyEffectsPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int = 0) {
        openPageSafely(player) { foodApplyEffectsPage.open(player, session, openBack, page) }
    }

    fun openDeathProtectionPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { deathProtectionPage.open(player, session) }
    }

    fun openDeathProtectionRemoveEffectsPage(player: Player, session: ItemEditSession, page: Int = 0) {
        openPageSafely(player) { deathProtectionRemoveEffectsPage.open(player, session, page) }
    }

    fun openDeathProtectionApplyEffectsPage(player: Player, session: ItemEditSession, page: Int = 0) {
        openPageSafely(player) { deathProtectionApplyEffectsPage.open(player, session, page) }
    }

    fun openHeadTexturePage(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            support.transition(session) {
                headTextureEditPage.open(player, session)
            }
        }
    }

    fun openPotionPage(player: Player, session: ItemEditSession, back: (Player, ItemEditSession) -> Unit = openAdvancedPageOneHandler) {
        openPageSafely(player) { potionEditPage.open(player, session, back) }
    }

    fun openPotionEffectsPage(player: Player, session: ItemEditSession, page: Int = 0) {
        openPageSafely(player) { potionEffectsPage.open(player, session, page) }
    }

    fun openMapPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { mapEditPage.open(player, session) }
    }

    fun openTextAppearancePage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openTextAppearancePageOne(player, session, openBack)
    }

    fun openTextAppearancePageOne(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { textAppearancePageOne.open(player, session, openBack) }
    }

    fun openTextAppearancePageTwo(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { textAppearancePageTwo.open(player, session, openBack) }
    }

    fun openTextAppearanceFromSimple(player: Player, session: ItemEditSession) {
        openTextAppearancePage(player, session, openSimpleHandler)
    }

    fun openTextAppearanceFromAdvanced(player: Player, session: ItemEditSession) {
        openTextAppearancePage(player, session, openAdvancedPageOneHandler)
    }


    fun openArmorTrimPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { armorTrimEditPage.open(player, session, openAdvancedPageOneHandler) }
    }

    fun openArmorTrimPatternPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { armorTrimPatternSelectPage.open(player, session, this::openArmorTrimPage) }
    }

    fun openArmorTrimMaterialPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { armorTrimMaterialSelectPage.open(player, session, this::openArmorTrimPage) }
    }

    fun openSpecialParametersFromAdvanced(player: Player, session: ItemEditSession) {
        if (session.editableItem.type == Material.DECORATED_POT) {
            openDecoratedPotRoot(player, session, openAdvancedPageOneHandler)
            return
        }
        if (session.editableItem.type == Material.ITEM_FRAME || session.editableItem.type == Material.GLOW_ITEM_FRAME) {
            toggleFrameInvisibilityAndReturn(player, session)
            return
        }
        if (session.editableItem.type == Material.PLAYER_HEAD) {
            openHeadTexturePage(player, session)
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

    private fun toggleFrameInvisibilityAndReturn(player: Player, session: ItemEditSession) {
        val toggledItem = FrameInvisibilitySupport.toggle(session.editableItem) ?: run {
            openAdvancedPageOne(player, session)
            return
        }

        session.editableItem = toggledItem
        openAdvancedPageOne(player, session)
    }

    fun openDecoratedPotRoot(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { potEditPage.open(player, session, openBack) }
    }

    fun openDecoratedPotPattern(
        player: Player,
        session: ItemEditSession,
        part: DecoratedPotPartDescriptor,
        openBack: (Player, ItemEditSession) -> Unit
    ) {
        openPageSafely(player) { potPatternSelectPage.open(player, session, part, openBack) }
    }

    private fun rememberCategory(player: Player, category: LastEditorCategory) {
        lastCategoryByPlayer[player.uniqueId] = category
    }

    private fun openPageSafely(player: Player, openAction: () -> Unit) {
        runCatching { openAction() }
            .onFailure {
                player.closeInventory()
                player.sendMessage(parser.parse("<!i><dark_red>▍ <#FF1500>Предмет повреждён.."))
            }
    }
}
