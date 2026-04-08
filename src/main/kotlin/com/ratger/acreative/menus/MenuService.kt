package com.ratger.acreative.menus

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.experimental.ComponentsService
import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.itemedit.meta.MiniMessageParser
import com.ratger.acreative.menus.itemEdit.apply.AmountApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.ApplyPromptService
import com.ratger.acreative.menus.itemEdit.apply.EquipSoundApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.EnchantmentApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.AttributeApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.DamageApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.DamagePerBlockApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.ItemEditorApplyStateManager
import com.ratger.acreative.menus.itemEdit.apply.ItemIdApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.ItemModelApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.MaxDurabilityApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.MiningSpeedApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.StackSizeApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.UseCooldownGroupApplyHandler
import com.ratger.acreative.menus.itemEdit.apply.UseCooldownSecondsApplyHandler
import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditMenu
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.ItemEditSessionManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MenuService(
    private val hooker: FunctionHooker
) {
    private val parser = MiniMessageParser()
    private val editParsers = EditParsers()
    private val validationService = ValidationService()
    private val editTargetResolver = EditTargetResolver()
    private val sessionManager = ItemEditSessionManager()
    private val buttonFactory = MenuButtonFactory(parser, ComponentsService())
    private val itemIdApplyHandler = ItemIdApplyHandler(editParsers)
    private val stackSizeApplyHandler = StackSizeApplyHandler(validationService, editTargetResolver)
    private val attributeApplyHandler = AttributeApplyHandler()
    private val applyStateManager = ItemEditorApplyStateManager(
        hooker = hooker,
        sessionManager = sessionManager,
        promptService = ApplyPromptService(hooker.messageManager),
        handlers = listOf(
            itemIdApplyHandler,
            AmountApplyHandler(),
            ItemModelApplyHandler(editParsers, itemIdApplyHandler::suggestions),
            stackSizeApplyHandler,
            attributeApplyHandler,
            EquipSoundApplyHandler(),
            EnchantmentApplyHandler(),
            MaxDurabilityApplyHandler(validationService, editTargetResolver),
            DamageApplyHandler(validationService, editTargetResolver),
            MiningSpeedApplyHandler(validationService, editTargetResolver),
            DamagePerBlockApplyHandler(validationService, editTargetResolver),
            UseCooldownSecondsApplyHandler(validationService, editTargetResolver),
            UseCooldownGroupApplyHandler(validationService, editTargetResolver)
        )
    )
    private val itemEditMenu = ItemEditMenu(
        hooker = hooker,
        sessionManager = sessionManager,
        buttonFactory = buttonFactory,
        parser = parser
    ) { player, _, kind, reopen ->
        applyStateManager.beginWaiting(player, kind, reopen)
        player.closeInventory()
    }

    init {
        sessionManager.addCloseListener { player, _ ->
            applyStateManager.cancelWaiting(player, reopenMenu = false)
        }
    }

    fun isInItemEditSession(player: Player): Boolean = sessionManager.isInSession(player)
    fun canPickupDuringItemSession(player: Player): Boolean = applyStateManager.canPickupInCurrentState(player)

    fun openItemEditor(player: Player) {
        val existingSession = sessionManager.getSession(player)
        if (existingSession != null) {
            applyStateManager.cancelWaiting(player, reopenMenu = false)
            itemEditMenu.openRoot(player, existingSession)
            return
        }

        val handItem = player.inventory.itemInMainHand
        if (handItem.type == Material.AIR || handItem.amount <= 0) {
            hooker.messageManager.sendChat(player, MessageKey.EDIT_EMPTY_HAND)
            return
        }

        val session = sessionManager.openSession(player, handItem)
        player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        itemEditMenu.openRoot(player, session)
    }

    fun handleApply(player: Player, args: Array<out String>) {
        applyStateManager.handleApplyCommand(player, args)
    }

    fun tabCompleteApply(player: Player, args: Array<out String>): List<String> {
        return applyStateManager.tabComplete(player, args)
    }

    fun handlePlayerDisconnect(player: Player) {
        applyStateManager.cancelWaiting(player, reopenMenu = false)
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
}
