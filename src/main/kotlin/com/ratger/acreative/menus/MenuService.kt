package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.ManagedSystem
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.apply.ApplyCommandCoordinator
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.edit.equippable.EquippableSupport
import com.ratger.acreative.menus.edit.experimental.ComponentsService
import com.ratger.acreative.menus.edit.head.HeadProfileService
import com.ratger.acreative.menus.edit.head.HeadTextureMutationSupport
import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.menus.edit.restrictions.RestrictionMode
import com.ratger.acreative.menus.edit.text.ItemTextStyleService
import com.ratger.acreative.menus.edit.text.VanillaNameLocalizationService
import com.ratger.acreative.menus.edit.text.VanillaTranslationResolver
import com.ratger.acreative.menus.edit.text.VanillaRuLocalization
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditMenu
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.ItemEditSessionManager
import com.ratger.acreative.menus.edit.apply.meta.AmountApplyHandler
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import com.ratger.acreative.menus.edit.apply.attributes.AttributeApplyHandler
import com.ratger.acreative.menus.edit.apply.tool.DamageApplyHandler
import com.ratger.acreative.menus.edit.apply.tool.DamagePerBlockApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.ConsumableApplyEffectAddApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.ConsumableConsumeSecondsApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.ConsumableRandomTeleportDiameterApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.ConsumableRemoveEffectAddApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.ConsumableSoundApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.FoodNutritionApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.FoodSaturationApplyHandler
import com.ratger.acreative.menus.edit.apply.deathprotection.DeathProtectionApplyEffectAddApplyHandler
import com.ratger.acreative.menus.edit.apply.deathprotection.DeathProtectionRandomTeleportDiameterApplyHandler
import com.ratger.acreative.menus.edit.apply.deathprotection.DeathProtectionRemoveEffectAddApplyHandler
import com.ratger.acreative.menus.edit.apply.deathprotection.DeathProtectionSoundApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.edit.apply.enchant.EnchantmentApplyHandler
import com.ratger.acreative.menus.edit.apply.head.EquipSoundApplyHandler
import com.ratger.acreative.menus.edit.apply.head.HeadLicensedNameApplyHandler
import com.ratger.acreative.menus.edit.apply.head.HeadOnlineNameApplyHandler
import com.ratger.acreative.menus.edit.apply.head.HeadTextureValueApplyHandler
import com.ratger.acreative.menus.edit.apply.core.ItemEditorApplyStateManager
import com.ratger.acreative.menus.edit.apply.meta.ItemIdApplyHandler
import com.ratger.acreative.menus.edit.apply.meta.ItemModelApplyHandler
import com.ratger.acreative.menus.edit.apply.meta.MaxDurabilityApplyHandler
import com.ratger.acreative.menus.edit.apply.map.MapIdApplyHandler
import com.ratger.acreative.menus.edit.apply.map.MapColorApplyHandler
import com.ratger.acreative.menus.edit.apply.tool.MiningSpeedApplyHandler
import com.ratger.acreative.menus.edit.apply.text.NameTextApplyHandler
import com.ratger.acreative.menus.edit.apply.text.NameRawMiniMessageApplyHandler
import com.ratger.acreative.menus.edit.apply.text.LoreTextApplyHandler
import com.ratger.acreative.menus.edit.apply.text.LoreRawMiniMessageLineApplyHandler
import com.ratger.acreative.menus.edit.apply.potion.PotionColorApplyHandler
import com.ratger.acreative.menus.edit.apply.potion.PotionEffectAddApplyHandler
import com.ratger.acreative.menus.edit.apply.restrictions.RestrictionBlockApplyHandler
import com.ratger.acreative.menus.edit.apply.meta.StackSizeApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.UseCooldownGroupApplyHandler
import com.ratger.acreative.menus.edit.apply.effects.UseCooldownSecondsApplyHandler
import com.ratger.acreative.menus.edit.effects.visual.VisualEffectFlowService
import com.ratger.acreative.menus.edit.personal.PersonalItemsRepository
import com.ratger.acreative.menus.edit.personal.PersonalItemsService
import com.ratger.acreative.menus.decorationheads.support.SignInputService
import com.ratger.acreative.menus.paint.PaintToolMarker
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.Executors

class MenuService(
    private val hooker: FunctionHooker
) {
    private companion object {
        const val EDIT_PERMISSION = "advancedcreative.edit"
    }

    data class MemorySnapshot(
        val cachedPlayers: Int,
        val cachedItems: Int,
        val items: List<ItemStack>
    )

    private val parser = MiniMessageParser()
    private val editParsers = EditParsers()
    private val validationService = ValidationService()
    private val editTargetResolver = EditTargetResolver()
    private val visualEffectFlowService = VisualEffectFlowService(validationService, editTargetResolver)
    private val vanillaTranslationResolver = VanillaTranslationResolver(hooker.plugin.dataFolder.toPath(), hooker.plugin.logger)
    private val vanillaNameLocalizationService = VanillaNameLocalizationService(vanillaTranslationResolver)
    private val textStyleService = ItemTextStyleService(vanillaNameLocalizationService)
    private val sessionManager = ItemEditSessionManager()
    private val buttonFactory = MenuButtonFactory(parser, ComponentsService(), hooker.tickScheduler)
    private val signInputService = SignInputService(hooker.plugin)
    private val headMutationSupport = HeadTextureMutationSupport()
    private val headLookupService = LicensedProfileLookupService()
    private val headProfileService = HeadProfileService(headLookupService)
    private val personalDataExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "acreative-edit-personal-items").apply { isDaemon = true }
    }
    private val personalItemsRepository = PersonalItemsRepository(hooker.database, 21)
    private val personalItemsService = PersonalItemsService(personalItemsRepository, personalDataExecutor, 21)
    private var periodicPersonalItemsFlushTask: BukkitTask? = null

    private val itemIdApplyHandler = ItemIdApplyHandler(editParsers)
    private val stackSizeApplyHandler = StackSizeApplyHandler(validationService, editTargetResolver)
    private val attributeApplyHandler = AttributeApplyHandler()
    private val canPlaceOnApplyHandler = RestrictionBlockApplyHandler(EditorApplyKind.CAN_PLACE_ON, RestrictionMode.CAN_PLACE_ON, editParsers)
    private val canBreakApplyHandler = RestrictionBlockApplyHandler(EditorApplyKind.CAN_BREAK, RestrictionMode.CAN_BREAK, editParsers)
    private val potionColorApplyHandler = PotionColorApplyHandler(validationService, editTargetResolver)
    private val mapColorApplyHandler = MapColorApplyHandler(validationService)
    private val mapIdApplyHandler = MapIdApplyHandler(validationService)
    private val potionEffectAddApplyHandler = PotionEffectAddApplyHandler(editParsers, validationService, editTargetResolver)
    private val deathProtectionSoundApplyHandler = DeathProtectionSoundApplyHandler(editParsers, validationService, editTargetResolver)
    private val deathProtectionRemoveEffectAddApplyHandler = DeathProtectionRemoveEffectAddApplyHandler(editParsers)
    private val deathProtectionRandomTeleportApplyHandler = DeathProtectionRandomTeleportDiameterApplyHandler(validationService, editTargetResolver)
    private val deathProtectionApplyEffectAddApplyHandler = DeathProtectionApplyEffectAddApplyHandler(editParsers, validationService, editTargetResolver)
    private val foodNutritionApplyHandler = FoodNutritionApplyHandler(validationService, editTargetResolver)
    private val foodSaturationApplyHandler = FoodSaturationApplyHandler(validationService, editTargetResolver)
    private val consumableConsumeSecondsApplyHandler = ConsumableConsumeSecondsApplyHandler(validationService, editTargetResolver)
    private val consumableSoundApplyHandler = ConsumableSoundApplyHandler(editParsers, validationService, editTargetResolver)
    private val consumableRemoveEffectAddApplyHandler = ConsumableRemoveEffectAddApplyHandler(editParsers)
    private val consumableRandomTeleportApplyHandler = ConsumableRandomTeleportDiameterApplyHandler(validationService, editTargetResolver)
    private val consumableApplyEffectAddApplyHandler = ConsumableApplyEffectAddApplyHandler(editParsers, validationService, editTargetResolver)
    private var applyStateManager: ItemEditorApplyStateManager
    private var itemEditorApplyTarget: ApplyCommandTarget
    private val applyCoordinator = ApplyCommandCoordinator()

    private val itemEditMenu = ItemEditMenu(
        hooker = hooker,
        sessionManager = sessionManager,
        buttonFactory = buttonFactory,
        parser = parser,
        requestApplyInput = { player, _, kind, reopen ->
            applyStateManager.beginWaiting(player, kind, reopen)
            player.closeInventory()
        },
        headMutationSupport = headMutationSupport,
        textStyleService = textStyleService,
        visualEffectFlowService = visualEffectFlowService,
        personalItemsService = personalItemsService,
        requestSignInput = { player, templateLines, onSubmit, onLeave ->
            signInputService.open(
                player = player,
                templateLines = templateLines,
                onSubmit = { submitPlayer, input ->
                    hooker.tickScheduler.runNow { onSubmit(submitPlayer, input) }
                },
                onLeave = { leavePlayer ->
                    hooker.tickScheduler.runNow { onLeave(leavePlayer) }
                }
            )
        }
    )

    init {
        VanillaRuLocalization.initialize(vanillaTranslationResolver)
        val flushIntervalTicks = 6L * 60L * 60L * 20L
        periodicPersonalItemsFlushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            hooker.plugin,
            Runnable { personalItemsService.flushDirtyToDatabase() },
            flushIntervalTicks,
            flushIntervalTicks
        )
        applyStateManager = ItemEditorApplyStateManager(
            hooker = hooker,
            sessionManager = sessionManager,
            promptService = ApplyPromptService(hooker.messageManager),
            handlers = listOf(
                itemIdApplyHandler,
                NameTextApplyHandler(textStyleService),
                LoreTextApplyHandler(textStyleService),
                NameRawMiniMessageApplyHandler(textStyleService),
                LoreRawMiniMessageLineApplyHandler(textStyleService),
                AmountApplyHandler(),
                ItemModelApplyHandler(editParsers, itemIdApplyHandler::suggestions),
                stackSizeApplyHandler,
                attributeApplyHandler,
                EquipSoundApplyHandler(editParsers),
                EnchantmentApplyHandler(),
                MaxDurabilityApplyHandler(validationService, editTargetResolver),
                DamageApplyHandler(validationService, editTargetResolver),
                MiningSpeedApplyHandler(validationService, editTargetResolver),
                DamagePerBlockApplyHandler(validationService, editTargetResolver),
                UseCooldownSecondsApplyHandler(validationService, editTargetResolver),
                UseCooldownGroupApplyHandler(validationService, editTargetResolver),
                canPlaceOnApplyHandler,
                canBreakApplyHandler,
                HeadOnlineNameApplyHandler(headMutationSupport),
                HeadTextureValueApplyHandler(headMutationSupport),
                potionColorApplyHandler,
                mapColorApplyHandler,
                mapIdApplyHandler,
                potionEffectAddApplyHandler,
                deathProtectionSoundApplyHandler,
                deathProtectionRemoveEffectAddApplyHandler,
                deathProtectionRandomTeleportApplyHandler,
                deathProtectionApplyEffectAddApplyHandler,
                foodNutritionApplyHandler,
                foodSaturationApplyHandler,
                consumableConsumeSecondsApplyHandler,
                consumableSoundApplyHandler,
                consumableRemoveEffectAddApplyHandler,
                consumableRandomTeleportApplyHandler,
                consumableApplyEffectAddApplyHandler,
                HeadLicensedNameApplyHandler(
                    plugin = hooker.plugin,
                    sessionManager = sessionManager,
                    headProfileService = headProfileService,
                    mutationSupport = headMutationSupport,
                    reopenHeadTexturePage = { waitingPlayer ->
                        val activeSession = sessionManager.getSession(waitingPlayer)
                        if (activeSession != null) {
                            itemEditMenu.openHeadTexturePage(waitingPlayer, activeSession)
                        }
                    }
                )
            )
        )

        itemEditorApplyTarget = object : ApplyCommandTarget {
            override fun isWaiting(player: Player): Boolean = applyStateManager.isWaiting(player)
            override fun handle(player: Player, args: Array<out String>): Boolean {
                applyStateManager.handleApplyCommand(player, args)
                return true
            }
            override fun tabComplete(player: Player, args: Array<out String>): List<String> = applyStateManager.tabComplete(player, args)
            override fun cancel(player: Player) = applyStateManager.cancelWaiting(player, reopenMenu = false)
        }
        applyCoordinator.registerTarget(itemEditorApplyTarget)

        sessionManager.addCloseListener { player, _ ->
            applyStateManager.cancelWaiting(player, reopenMenu = false)
        }
        sessionManager.addCloseListener { player, session ->
            personalItemsService.onEditSessionClosed(player.uniqueId, session.editableItem, session.initialContentHash)
        }
    }

    fun isInItemEditSession(player: Player): Boolean = sessionManager.isInSession(player)
    fun canPickupDuringItemSession(player: Player): Boolean = applyStateManager.canPickupInCurrentState(player)

    fun openItemEditor(player: Player) {
        if (!player.hasPermission(EDIT_PERMISSION)) {
            hooker.permissionManager.sendPermissionDenied(player, "edit")
            return
        }

        hooker.bannerMenuService.clearApplyRecoveryContext(player)
        val existingSession = sessionManager.getSession(player)
        if (existingSession != null) {
            applyStateManager.cancelWaiting(player, reopenMenu = false)
            itemEditMenu.openLastCategoryOrDefault(player, existingSession)
            return
        }

        val handItem = player.inventory.itemInMainHand
        if (handItem.type == Material.AIR || handItem.amount <= 0) {
            itemEditMenu.openMyItemsStandalone(player)
            return
        }
        if (PaintToolMarker.isPaintTool(hooker.plugin, handItem)) {
            hooker.messageManager.sendChat(player, MessageKey.EDIT_NOT_EDITABLE)
            return
        }

        val session = sessionManager.openSession(player, handItem)
        player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        itemEditMenu.openLastCategoryOrDefault(player, session)
    }

    fun handleApply(player: Player, args: Array<out String>) {
        if (!applyCoordinator.isWaiting(player)) {
            if (hooker.bannerMenuService.reopenPostFromApply(player)) {
                return
            }
            if (!hooker.systemToggleService.isEnabled(ManagedSystem.EDIT)) {
                hooker.messageManager.sendChat(player, MessageKey.SYSTEM_DISABLED)
                return
            }
            openItemEditor(player)
            return
        }

        val activeTarget = applyCoordinator.activeTarget(player)
        if (activeTarget === itemEditorApplyTarget && !hooker.systemToggleService.isEnabled(ManagedSystem.EDIT)) {
            applyStateManager.cancelWaiting(player, reopenMenu = false)
            hooker.messageManager.sendChat(player, MessageKey.SYSTEM_DISABLED)
            return
        }

        applyCoordinator.handle(player, args)
    }

    fun tabCompleteApply(player: Player, args: Array<out String>): List<String> {
        return applyCoordinator.tabComplete(player, args)
    }

    fun handlePlayerRuntimeReset(player: Player) {
        applyCoordinator.cancel(player)
        personalItemsService.commitDeferredPromotions(player.uniqueId)
    }

    fun closeAllItemEditorSessions() {
        itemEditSessionsSnapshot().forEach { session ->
            val player = Bukkit.getPlayer(session.playerId) ?: return@forEach
            applyStateManager.cancelWaiting(player, reopenMenu = false)
            player.closeInventory()
            val closedSession = sessionManager.closeSession(player)
            if (closedSession != null) {
                syncEditedItemBack(player, closedSession)
            }
        }
    }

    fun handlePlayerDisconnect(player: Player) {
        handlePlayerRuntimeReset(player)
        personalItemsService.evictPlayer(player.uniqueId)
    }

    fun handlePlayerJoin(player: Player) {
        personalItemsService.pruneExpiredOnFirstJoin(player.uniqueId)
    }

    fun registerApplyTarget(target: ApplyCommandTarget) {
        applyCoordinator.registerTarget(target)
    }

    fun headMutationSupport(): HeadTextureMutationSupport = headMutationSupport
    fun buttonFactory(): MenuButtonFactory = buttonFactory
    fun itemEditSessionsSnapshot(): List<ItemEditSession> = sessionManager.sessionsSnapshot()
    fun personalItemsMemorySnapshot(): MemorySnapshot {
        val snapshot = personalItemsService.memorySnapshot()
        return MemorySnapshot(snapshot.cachedPlayers, snapshot.cachedItems, snapshot.items)
    }

    fun syncEditedItemBack(player: Player, session: ItemEditSession) {
        EquippableSupport.normalizeAfterMutation(session.editableItem)
        val item = session.editableItem.clone()
        if (item.type == Material.AIR || item.amount <= 0) return

        val inventory = player.inventory
        val targetSlotItem = inventory.getItem(session.originalMainHandSlot)

        if (targetSlotItem == null || targetSlotItem.type == Material.AIR || targetSlotItem.amount <= 0) {
            inventory.setItem(session.originalMainHandSlot, item)
            return
        }

        val emptySlot = inventory.firstEmpty()
        if (emptySlot != -1) {
            inventory.setItem(emptySlot, item)
            return
        }

        player.world.dropItemNaturally(player.location.clone().add(0.0, 1.0, 0.0), item)
    }

    fun shutdown() {
        periodicPersonalItemsFlushTask?.cancel()
        runCatching {
            personalDataExecutor.submit { personalItemsService.flushDirtyToDatabase() }.get(5, java.util.concurrent.TimeUnit.SECONDS)
        }.onFailure {
            hooker.plugin.logger.warning("Failed to flush personal items cache before shutdown: ${it.message}")
            personalItemsService.flushDirtyToDatabase()
        }
        personalDataExecutor.shutdownNow()
    }
}
