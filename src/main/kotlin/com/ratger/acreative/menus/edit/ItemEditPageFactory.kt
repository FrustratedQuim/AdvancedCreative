package com.ratger.acreative.menus.edit

import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.head.HeadTextureMutationSupport
import com.ratger.acreative.menus.edit.head.HeadTextureValueBookSupport
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import com.ratger.acreative.menus.MenuButtonFactory
import com.ratger.acreative.menus.edit.pages.advanced.AdvancedEditDetailsPage
import com.ratger.acreative.menus.edit.pages.advanced.AdvancedEditMainPage
import com.ratger.acreative.menus.edit.pages.appearance.TextAppearanceContentPage
import com.ratger.acreative.menus.edit.pages.appearance.TextAppearanceStylePage
import com.ratger.acreative.menus.edit.pages.attributes.AttributeEditPage
import com.ratger.acreative.menus.edit.pages.attributes.EquippableEditPage
import com.ratger.acreative.menus.edit.pages.effects.DeathProtectionApplyEffectsListPage
import com.ratger.acreative.menus.edit.pages.effects.DeathProtectionEditPage
import com.ratger.acreative.menus.edit.pages.effects.DeathProtectionRemoveEffectsListPage
import com.ratger.acreative.menus.edit.pages.effects.FoodApplyEffectsListPage
import com.ratger.acreative.menus.edit.pages.effects.FoodEditPage
import com.ratger.acreative.menus.edit.pages.effects.FoodRemoveEffectsListPage
import com.ratger.acreative.menus.edit.pages.enchantments.EnchantmentsActivePage
import com.ratger.acreative.menus.edit.pages.enchantments.EnchantmentsEditPage
import com.ratger.acreative.menus.edit.pages.head.HeadTextureEditPage
import com.ratger.acreative.menus.edit.pages.map.MapEditPage
import com.ratger.acreative.menus.edit.pages.pot.PotEditPage
import com.ratger.acreative.menus.edit.pages.pot.DecoratedPotPartDescriptor
import com.ratger.acreative.menus.edit.pages.pot.PotPatternSelectPage
import com.ratger.acreative.menus.edit.pages.potion.PotionEditPage
import com.ratger.acreative.menus.edit.pages.potion.PotionEffectsActivePage
import com.ratger.acreative.menus.edit.pages.restrictions.RestrictionsListPage
import com.ratger.acreative.menus.edit.pages.restrictions.RestrictionsRootPage
import com.ratger.acreative.menus.edit.pages.root.RootEditMenu
import com.ratger.acreative.menus.edit.pages.root.SimpleEditMenu
import com.ratger.acreative.menus.edit.pages.tooling.LockEditPage
import com.ratger.acreative.menus.edit.pages.tooling.ToolEditPage
import com.ratger.acreative.menus.edit.pages.tooling.UseCooldownEditPage
import com.ratger.acreative.menus.edit.pages.tooling.UseRemainderEditPage
import com.ratger.acreative.menus.edit.pages.trim.ArmorTrimEditPage
import com.ratger.acreative.menus.edit.pages.trim.ArmorTrimMaterialSelectPage
import com.ratger.acreative.menus.edit.pages.trim.ArmorTrimPatternSelectPage
import org.bukkit.entity.Player

internal data class ItemEditPages(
    val root: RootEditMenu,
    val simple: SimpleEditMenu,
    val advancedMain: AdvancedEditMainPage,
    val advancedDetails: AdvancedEditDetailsPage,
    val textAppearanceContent: TextAppearanceContentPage,
    val textAppearanceStyle: TextAppearanceStylePage,
    val food: FoodEditPage,
    val foodRemoveEffects: FoodRemoveEffectsListPage,
    val foodApplyEffects: FoodApplyEffectsListPage,
    val deathProtection: DeathProtectionEditPage,
    val deathProtectionRemoveEffects: DeathProtectionRemoveEffectsListPage,
    val deathProtectionApplyEffects: DeathProtectionApplyEffectsListPage,
    val attribute: AttributeEditPage,
    val equippable: EquippableEditPage,
    val useRemainder: UseRemainderEditPage,
    val useCooldown: UseCooldownEditPage,
    val lock: LockEditPage,
    val tool: ToolEditPage,
    val enchantments: EnchantmentsEditPage,
    val restrictionsRoot: RestrictionsRootPage,
    val potPatternSelect: PotPatternSelectPage,
    val potEdit: PotEditPage,
    val headTexture: HeadTextureEditPage,
    val potionEdit: PotionEditPage,
    val potionEffects: PotionEffectsActivePage,
    val mapEdit: MapEditPage,
    val armorTrimPatternSelect: ArmorTrimPatternSelectPage,
    val armorTrimMaterialSelect: ArmorTrimMaterialSelectPage,
    val armorTrimEdit: ArmorTrimEditPage
)

internal data class ItemEditNavigationHandlers(
    val openRoot: (Player, ItemEditSession) -> Unit,
    val openSimple: (Player, ItemEditSession) -> Unit,
    val openAdvancedPageOne: (Player, ItemEditSession) -> Unit,
    val openAdvancedPageTwo: (Player, ItemEditSession) -> Unit,
    val openAttributePage: (Player, ItemEditSession) -> Unit,
    val openEquippablePage: (Player, ItemEditSession) -> Unit,
    val openToolPage: (Player, ItemEditSession) -> Unit,
    val openEnchantmentsFromSimple: (Player, ItemEditSession) -> Unit,
    val openEnchantmentsFromAdvanced: (Player, ItemEditSession) -> Unit,
    val openUseRemainderPage: (Player, ItemEditSession) -> Unit,
    val openUseCooldownPage: (Player, ItemEditSession) -> Unit,
    val openLockPage: (Player, ItemEditSession) -> Unit,
    val openRestrictionsRoot: (Player, ItemEditSession) -> Unit,
    val openDeathProtectionPage: (Player, ItemEditSession) -> Unit,
    val openFoodPageFromAdvanced: (Player, ItemEditSession) -> Unit,
    val openSpecialParametersFromAdvanced: (Player, ItemEditSession) -> Unit,
    val openPotionPageFromAdvanced: (Player, ItemEditSession) -> Unit,
    val openMapPageFromAdvanced: (Player, ItemEditSession) -> Unit,
    val openArmorTrimPage: (Player, ItemEditSession) -> Unit,
    val openTextAppearanceFromSimple: (Player, ItemEditSession) -> Unit,
    val openTextAppearanceFromAdvanced: (Player, ItemEditSession) -> Unit,
    val openTextAppearancePageOne: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit,
    val openTextAppearancePageTwo: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit,
    val openFoodPage: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit,
    val openFoodRemoveEffectsPage: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit, Int) -> Unit,
    val openFoodApplyEffectsPage: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit, Int) -> Unit,
    val openDeathProtectionRemoveEffectsPage: (Player, ItemEditSession, Int) -> Unit,
    val openDeathProtectionApplyEffectsPage: (Player, ItemEditSession, Int) -> Unit,
    val openDecoratedPotPattern: (Player, ItemEditSession, DecoratedPotPartDescriptor, (Player, ItemEditSession) -> Unit) -> Unit,
    val openPotionEffectsPage: (Player, ItemEditSession, Int) -> Unit,
    val openPotionPageWithBack: (Player, ItemEditSession, (Player, ItemEditSession) -> Unit) -> Unit,
    val openArmorTrimPatternPage: (Player, ItemEditSession) -> Unit,
    val openArmorTrimMaterialPage: (Player, ItemEditSession) -> Unit
)

internal class ItemEditPageFactory(
    private val support: ItemEditMenuSupport,
    private val buttonFactory: MenuButtonFactory,
    private val requestApplyInput: (Player, ItemEditSession, EditorApplyKind, (Player, ItemEditSession) -> Unit) -> Unit,
    private val headMutationSupport: HeadTextureMutationSupport,
    private val textStyleService: ItemTextStyleService
) {
    fun create(handlers: ItemEditNavigationHandlers): ItemEditPages {
        val headTextureValueBookSupport = HeadTextureValueBookSupport()

        val rootPage = RootEditMenu(
            support = support,
            buttonFactory = buttonFactory,
            openSimple = handlers.openSimple,
            openAdvancedPageOne = handlers.openAdvancedPageOne
        )

        val simplePage = SimpleEditMenu(
            support = support,
            buttonFactory = buttonFactory,
            openRoot = handlers.openRoot,
            openEnchantments = handlers.openEnchantmentsFromSimple,
            openTextAppearance = handlers.openTextAppearanceFromSimple
        )

        val advancedMainPage = AdvancedEditMainPage(
            support = support,
            buttonFactory = buttonFactory,
            openRoot = handlers.openRoot,
            openAdvancedPageTwo = handlers.openAdvancedPageTwo,
            openSpecialParameters = handlers.openSpecialParametersFromAdvanced,
            openTextAppearance = handlers.openTextAppearanceFromAdvanced,
            requestApplyInput = requestApplyInput
        )

        val textAppearanceStylePage = TextAppearanceStylePage(
            support = support,
            buttonFactory = buttonFactory,
            textStyleService = textStyleService,
            requestApplyInput = requestApplyInput,
            openPageOne = handlers.openTextAppearancePageOne
        )

        val textAppearanceContentPage = TextAppearanceContentPage(
            support = support,
            buttonFactory = buttonFactory,
            textStyleService = textStyleService,
            requestApplyInput = requestApplyInput,
            openPageTwo = handlers.openTextAppearancePageTwo
        )

        val advancedDetailsPage = AdvancedEditDetailsPage(
            support = support,
            buttonFactory = buttonFactory,
            openAdvancedPageOne = handlers.openAdvancedPageOne,
            openAttributePage = handlers.openAttributePage,
            openEquippablePage = handlers.openEquippablePage,
            openToolPage = handlers.openToolPage,
            openEnchantmentsPage = handlers.openEnchantmentsFromAdvanced,
            openUseRemainderPage = handlers.openUseRemainderPage,
            openUseCooldownPage = handlers.openUseCooldownPage,
            openRestrictionsRootPage = handlers.openRestrictionsRoot,
            openDeathProtectionPage = handlers.openDeathProtectionPage,
            openFoodPage = handlers.openFoodPageFromAdvanced
        )

        val foodRemoveEffectsPage = FoodRemoveEffectsListPage(
            support = support,
            buttonFactory = buttonFactory,
            requestApplyInput = requestApplyInput,
            openFoodRoot = handlers.openFoodPage
        )

        val foodApplyEffectsPage = FoodApplyEffectsListPage(
            support = support,
            buttonFactory = buttonFactory,
            requestApplyInput = requestApplyInput,
            openFoodRoot = handlers.openFoodPage
        )

        val foodPage = FoodEditPage(
            support = support,
            buttonFactory = buttonFactory,
            openRemoveEffectsPage = { player, session, openBack ->
                handlers.openFoodRemoveEffectsPage(player, session, openBack, 0)
            },
            openApplyEffectsPage = { player, session, openBack ->
                handlers.openFoodApplyEffectsPage(player, session, openBack, 0)
            },
            requestApplyInput = requestApplyInput
        )

        val deathProtectionRemoveEffectsPage = DeathProtectionRemoveEffectsListPage(
            support = support,
            buttonFactory = buttonFactory,
            requestApplyInput = requestApplyInput,
            openDeathProtectionRoot = handlers.openDeathProtectionPage
        )

        val deathProtectionApplyEffectsPage = DeathProtectionApplyEffectsListPage(
            support = support,
            buttonFactory = buttonFactory,
            requestApplyInput = requestApplyInput,
            openDeathProtectionRoot = handlers.openDeathProtectionPage
        )

        val deathProtectionPage = DeathProtectionEditPage(
            support = support,
            buttonFactory = buttonFactory,
            openAdvancedPageTwo = handlers.openAdvancedPageTwo,
            openRemoveEffectsPage = { player, session ->
                handlers.openDeathProtectionRemoveEffectsPage(player, session, 0)
            },
            openApplyEffectsPage = { player, session ->
                handlers.openDeathProtectionApplyEffectsPage(player, session, 0)
            },
            requestApplyInput = requestApplyInput
        )

        val attributePage = AttributeEditPage(support, buttonFactory, handlers.openAdvancedPageTwo, requestApplyInput)
        val equippablePage = EquippableEditPage(support, buttonFactory, handlers.openAdvancedPageTwo, requestApplyInput)
        val useRemainderPage = UseRemainderEditPage(support, buttonFactory, handlers.openAdvancedPageTwo)
        val useCooldownPage = UseCooldownEditPage(support, buttonFactory, handlers.openAdvancedPageTwo, requestApplyInput)
        val lockPage = LockEditPage(support, buttonFactory, handlers.openAdvancedPageOne)
        val toolPage = ToolEditPage(support, buttonFactory, handlers.openAdvancedPageTwo, requestApplyInput)

        val enchantmentsActivePage = EnchantmentsActivePage(support, buttonFactory, requestApplyInput)
        val enchantmentsPage = EnchantmentsEditPage(support, buttonFactory, enchantmentsActivePage::open)

        val restrictionsListPage = RestrictionsListPage(support, buttonFactory, handlers.openRestrictionsRoot, requestApplyInput)
        val restrictionsRootPage = RestrictionsRootPage(support, buttonFactory, handlers.openAdvancedPageTwo, restrictionsListPage::open)

        val potPatternSelectPage = PotPatternSelectPage(support, buttonFactory)
        val potEditPage = PotEditPage(support, buttonFactory, handlers.openDecoratedPotPattern)

        val headTextureEditPage = HeadTextureEditPage(
            support = support,
            buttonFactory = buttonFactory,
            mutationSupport = headMutationSupport,
            textureValueBookSupport = headTextureValueBookSupport,
            openBack = handlers.openAdvancedPageOne,
            requestApplyInput = requestApplyInput
        )

        val potionEditPage = PotionEditPage(support, buttonFactory, requestApplyInput) { player, session ->
            handlers.openPotionEffectsPage(player, session, 0)
        }
        val potionEffectsPage = PotionEffectsActivePage(support, buttonFactory, requestApplyInput) { player, session ->
            handlers.openPotionPageWithBack(player, session, handlers.openAdvancedPageOne)
        }

        val mapEditPage = MapEditPage(support, buttonFactory, requestApplyInput, handlers.openAdvancedPageOne)

        val armorTrimPatternSelectPage = ArmorTrimPatternSelectPage(support, buttonFactory)
        val armorTrimMaterialSelectPage = ArmorTrimMaterialSelectPage(support, buttonFactory)
        val armorTrimEditPage = ArmorTrimEditPage(
            support = support,
            buttonFactory = buttonFactory,
            openPatternSelect = handlers.openArmorTrimPatternPage,
            openMaterialSelect = handlers.openArmorTrimMaterialPage
        )

        return ItemEditPages(
            root = rootPage,
            simple = simplePage,
            advancedMain = advancedMainPage,
            advancedDetails = advancedDetailsPage,
            textAppearanceContent = textAppearanceContentPage,
            textAppearanceStyle = textAppearanceStylePage,
            food = foodPage,
            foodRemoveEffects = foodRemoveEffectsPage,
            foodApplyEffects = foodApplyEffectsPage,
            deathProtection = deathProtectionPage,
            deathProtectionRemoveEffects = deathProtectionRemoveEffectsPage,
            deathProtectionApplyEffects = deathProtectionApplyEffectsPage,
            attribute = attributePage,
            equippable = equippablePage,
            useRemainder = useRemainderPage,
            useCooldown = useCooldownPage,
            lock = lockPage,
            tool = toolPage,
            enchantments = enchantmentsPage,
            restrictionsRoot = restrictionsRootPage,
            potPatternSelect = potPatternSelectPage,
            potEdit = potEditPage,
            headTexture = headTextureEditPage,
            potionEdit = potionEditPage,
            potionEffects = potionEffectsPage,
            mapEdit = mapEditPage,
            armorTrimPatternSelect = armorTrimPatternSelectPage,
            armorTrimMaterialSelect = armorTrimMaterialSelectPage,
            armorTrimEdit = armorTrimEditPage
        )
    }
}
