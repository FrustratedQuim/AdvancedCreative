package com.ratger.acreative.menus.edit

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.container.LockItemSupport
import com.ratger.acreative.menus.edit.head.HeadTextureMutationSupport
import com.ratger.acreative.menus.edit.invisibility.FrameInvisibilitySupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectContextKey
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectFlowService
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import com.ratger.acreative.menus.edit.trim.ArmorTrimSupport
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.pages.pot.DecoratedPotPartDescriptor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

class ItemEditMenu(
    hooker: FunctionHooker,
    sessionManager: ItemEditSessionManager,
    buttonFactory: MenuButtonFactory,
    private val parser: MiniMessageParser,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val requestSignInput: (Player, Array<String>, (Player, String?) -> Unit, (Player) -> Unit) -> Unit,
    headMutationSupport: HeadTextureMutationSupport,
    textStyleService: ItemTextStyleService,
    private val visualEffectFlowService: VisualEffectFlowService
) {
    enum class LastEditorCategory {
        ROOT,
        SIMPLE,
        ADVANCED
    }

    private val support = ItemEditMenuSupport(hooker, sessionManager, buttonFactory, parser)
    private val lastCategoryByPlayer = mutableMapOf<java.util.UUID, LastEditorCategory>()
    private val lastAdvancedPageByPlayer = mutableMapOf<java.util.UUID, Int>()

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

    private val pages: ItemEditPages = ItemEditPageFactory(
        support = support,
        buttonFactory = buttonFactory,
        requestApplyInput = requestApplyInput,
        requestSignInput = requestSignInput,
        headMutationSupport = headMutationSupport,
        textStyleService = textStyleService,
        visualEffectFlowService = visualEffectFlowService
    ).create(
        ItemEditNavigationHandlers(
            openRoot = openRootHandler,
            openSimple = openSimpleHandler,
            openAdvancedPageOne = openAdvancedPageOneHandler,
            openAdvancedPageTwo = openAdvancedPageTwoHandler,
            openAttributePage = openAttributePageHandler,
            openEquippablePage = openEquippablePageHandler,
            openToolPage = openToolPageHandler,
            openEnchantmentsFromSimple = openEnchantmentsFromSimpleHandler,
            openEnchantmentsFromAdvanced = openEnchantmentsFromAdvancedHandler,
            openUseRemainderPage = openUseRemainderPageHandler,
            openUseCooldownPage = openUseCooldownPageHandler,
            openLockPage = openLockPageHandler,
            openRestrictionsRoot = openRestrictionsRootHandler,
            openDeathProtectionPage = openDeathProtectionPageHandler,
            openFoodPageFromAdvanced = openFoodPageFromAdvancedHandler,
            openSpecialParametersFromAdvanced = openSpecialParametersFromAdvancedHandler,
            openPotionPageFromAdvanced = openPotionPageFromAdvancedHandler,
            openMapPageFromAdvanced = openMapPageFromAdvancedHandler,
            openArmorTrimPage = openArmorTrimPageHandler,
            openTextAppearanceFromSimple = openTextAppearanceFromSimpleHandler,
            openTextAppearanceFromAdvanced = openTextAppearanceFromAdvancedHandler,
            openTextAppearancePageOne = this::openTextAppearancePageOne,
            openTextAppearancePageTwo = this::openTextAppearancePageTwo,
            openFoodPage = this::openFoodPage,
            openFoodRemoveEffectsPage = this::openFoodRemoveEffectsPage,
            openFoodApplyEffectsPage = this::openFoodApplyEffectsPage,
            openDeathProtectionRemoveEffectsPage = this::openDeathProtectionRemoveEffectsPage,
            openDeathProtectionApplyEffectsPage = this::openDeathProtectionApplyEffectsPage,
            openDecoratedPotPattern = this::openDecoratedPotPattern,
            openPotionEffectsPage = this::openPotionEffectsPage,
            openPotionPageWithBack = this::openPotionPage,
            openVisualEffectTypePage = this::openVisualEffectTypePage,
            openVisualEffectTypeOnlyPage = this::openVisualEffectTypeOnlyPage,
            openVisualEffectParametersPage = this::openVisualEffectParametersPage,
            openArmorTrimPatternPage = this::openArmorTrimPatternPage,
            openArmorTrimMaterialPage = this::openArmorTrimMaterialPage
        )
    )

    fun openRoot(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.ROOT)
            pages.root.open(player, session)
        }
    }

    fun openSimple(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.SIMPLE)
            pages.simple.open(player, session)
        }
    }

    fun openAdvancedPageOne(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.ADVANCED)
            rememberAdvancedPage(player, 1)
            pages.advancedMain.open(player, session)
        }
    }

    fun openAdvancedPageTwo(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            rememberCategory(player, LastEditorCategory.ADVANCED)
            rememberAdvancedPage(player, 2)
            pages.advancedDetails.open(player, session)
        }
    }

    fun openLastCategoryOrDefault(player: Player, session: ItemEditSession) {
        when (lastCategoryByPlayer[player.uniqueId] ?: LastEditorCategory.ROOT) {
            LastEditorCategory.ROOT -> openRoot(player, session)
            LastEditorCategory.SIMPLE -> openSimple(player, session)
            LastEditorCategory.ADVANCED -> {
                when (lastAdvancedPageByPlayer[player.uniqueId] ?: 1) {
                    2 -> openAdvancedPageTwo(player, session)
                    else -> openAdvancedPageOne(player, session)
                }
            }
        }
    }

    fun openAttributePage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.attribute.open(player, session) }
    }

    fun openEquippablePage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.equippable.open(player, session) }
    }

    fun openEnchantmentsFromSimple(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.enchantments.open(player, session, openSimpleHandler) }
    }

    fun openEnchantmentsFromAdvanced(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.enchantments.open(player, session, openAdvancedPageTwoHandler) }
    }

    fun openUseRemainderPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.useRemainder.open(player, session) }
    }

    fun openUseCooldownPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.useCooldown.open(player, session) }
    }

    private fun openLockPageInternal(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.lock.open(player, session) }
    }

    fun openToolPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.tool.open(player, session) }
    }

    fun openRestrictionsRoot(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.restrictionsRoot.open(player, session) }
    }

    fun openFoodPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { pages.food.open(player, session, openBack) }
    }

    fun openFoodPageFromAdvanced(player: Player, session: ItemEditSession) {
        openFoodPage(player, session, openAdvancedPageTwoHandler)
    }

    fun openFoodRemoveEffectsPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int) {
        openPageSafely(player) { pages.foodRemoveEffects.open(player, session, openBack, page) }
    }

    fun openFoodApplyEffectsPage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit, page: Int) {
        openPageSafely(player) { pages.foodApplyEffects.open(player, session, openBack, page) }
    }

    fun openDeathProtectionPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.deathProtection.open(player, session) }
    }

    fun openDeathProtectionRemoveEffectsPage(player: Player, session: ItemEditSession, page: Int) {
        openPageSafely(player) { pages.deathProtectionRemoveEffects.open(player, session, page) }
    }

    fun openDeathProtectionApplyEffectsPage(player: Player, session: ItemEditSession, page: Int) {
        openPageSafely(player) { pages.deathProtectionApplyEffects.open(player, session, page) }
    }

    fun openHeadTexturePage(player: Player, session: ItemEditSession) {
        openPageSafely(player) {
            support.transition(session) {
                pages.headTexture.open(player, session)
            }
        }
    }

    fun openPotionPage(player: Player, session: ItemEditSession, back: (Player, ItemEditSession) -> Unit = openAdvancedPageOneHandler) {
        openPageSafely(player) { pages.potionEdit.open(player, session, back) }
    }

    fun openPotionEffectsPage(player: Player, session: ItemEditSession, page: Int) {
        openPageSafely(player) { pages.potionEffects.open(player, session, page) }
    }

    fun openMapPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.mapEdit.open(player, session) }
    }

    fun openVisualEffectTypePage(
        player: Player,
        session: ItemEditSession,
        contextKey: VisualEffectContextKey,
        page: Int,
        openParent: (Player, ItemEditSession) -> Unit
    ) {
        openPageSafely(player) {
            pages.visualEffectTypeSelect.open(
                player = player,
                session = session,
                contextKey = contextKey,
                page = page,
                openParent = openParent,
                openParams = { paramsPlayer, paramsSession ->
                    openVisualEffectParametersPage(
                        paramsPlayer,
                        paramsSession,
                        openParent
                    ) { backPlayer, backSession, targetPage ->
                        openVisualEffectTypePage(backPlayer, backSession, contextKey, targetPage, openParent)
                    }
                }
            )
        }
    }

    fun openVisualEffectParametersPage(
        player: Player,
        session: ItemEditSession,
        openParent: (Player, ItemEditSession) -> Unit,
        openTypePage: (Player, ItemEditSession, Int) -> Unit
    ) {
        openPageSafely(player) {
            pages.visualEffectParameters.open(player, session, openParent, openTypePage)
        }
    }

    fun openVisualEffectTypeOnlyPage(
        player: Player,
        session: ItemEditSession,
        contextKey: VisualEffectContextKey,
        page: Int,
        openParent: (Player, ItemEditSession) -> Unit,
        multiSelect: Boolean,
        selectedTypesProvider: (ItemEditSession) -> Set<PotionEffectType>,
        onTypeSelected: (Player, ItemEditSession, PotionEffectType) -> Unit
    ) {
        openPageSafely(player) {
            pages.visualEffectTypeSelect.open(
                player = player,
                session = session,
                contextKey = contextKey,
                page = page,
                openParent = openParent,
                openParams = openParent,
                multiSelect = multiSelect,
                selectedTypesProvider = selectedTypesProvider,
                onTypeSelected = onTypeSelected
            )
        }
    }

    fun openTextAppearancePage(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openTextAppearancePageOne(player, session, openBack)
    }

    fun openTextAppearancePageOne(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { pages.textAppearanceContent.open(player, session, openBack) }
    }

    fun openTextAppearancePageTwo(player: Player, session: ItemEditSession, openBack: (Player, ItemEditSession) -> Unit) {
        openPageSafely(player) { pages.textAppearanceStyle.open(player, session, openBack) }
    }

    fun openTextAppearanceFromSimple(player: Player, session: ItemEditSession) {
        openTextAppearancePage(player, session, openSimpleHandler)
    }

    fun openTextAppearanceFromAdvanced(player: Player, session: ItemEditSession) {
        openTextAppearancePage(player, session, openAdvancedPageOneHandler)
    }

    fun openArmorTrimPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.armorTrimEdit.open(player, session, openAdvancedPageOneHandler) }
    }

    fun openArmorTrimPatternPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.armorTrimPatternSelect.open(player, session, this::openArmorTrimPage) }
    }

    fun openArmorTrimMaterialPage(player: Player, session: ItemEditSession) {
        openPageSafely(player) { pages.armorTrimMaterialSelect.open(player, session, this::openArmorTrimPage) }
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
        openPageSafely(player) { pages.potEdit.open(player, session, openBack) }
    }

    fun openDecoratedPotPattern(
        player: Player,
        session: ItemEditSession,
        part: DecoratedPotPartDescriptor,
        openBack: (Player, ItemEditSession) -> Unit
    ) {
        openPageSafely(player) { pages.potPatternSelect.open(player, session, part, openBack) }
    }

    private fun rememberCategory(player: Player, category: LastEditorCategory) {
        lastCategoryByPlayer[player.uniqueId] = category
    }

    private fun rememberAdvancedPage(player: Player, page: Int) {
        lastAdvancedPageByPlayer[player.uniqueId] = page
    }

    private fun openPageSafely(player: Player, openAction: () -> Unit) {
        runCatching { openAction() }
            .onFailure {
                player.closeInventory()
                player.sendMessage(parser.parse("<!i><dark_red>▍ <#FF1500>Предмет повреждён.."))
            }
    }
}
