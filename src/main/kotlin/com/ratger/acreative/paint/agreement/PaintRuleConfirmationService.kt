package com.ratger.acreative.paint.agreement

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.ManagedSystem
import com.ratger.acreative.menus.common.MenuUiSupport
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.paint.model.PaintCanvasSize
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import ru.violence.coreapi.bukkit.api.menu.MenuRows
import ru.violence.coreapi.bukkit.api.menu.button.Button
import ru.violence.coreapi.bukkit.api.util.ItemBuilder
import java.util.UUID

class PaintRuleConfirmationService(
    private val hooker: FunctionHooker,
    private val parser: MiniMessageParser,
    private val repository: PaintRuleConfirmationRepository,
    private val onConfirmed: (Player, PaintCanvasSize) -> Unit
) {
    private data class PendingRequest(
        val requestedSize: PaintCanvasSize,
        var remainingConfirmations: Int = REQUIRED_CONFIRMATIONS
    )

    private val confirmationCache = mutableMapOf<UUID, Boolean>()
    private val pendingRequests = mutableMapOf<UUID, PendingRequest>()
    private val clickCooldownTasks = mutableMapOf<UUID, Int>()

    fun requestPaintSession(player: Player, requestedSize: PaintCanvasSize): Boolean {
        if (isConfirmed(player.uniqueId)) {
            onConfirmed(player, requestedSize)
            return true
        }

        val request = PendingRequest(requestedSize)
        pendingRequests[player.uniqueId] = request
        openConfirmationMenu(player, request)
        return false
    }

    fun releaseAll() {
        val playerIds = pendingRequests.keys.toList()
        pendingRequests.clear()
        clickCooldownTasks.values.forEach(hooker.tickScheduler::cancel)
        clickCooldownTasks.clear()
        playerIds.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.takeIf { it.isOnline }?.closeInventory()
        }
    }

    fun clearRuntimeState(playerId: UUID) {
        pendingRequests.remove(playerId)
        confirmationCache.remove(playerId)
        clickCooldownTasks.remove(playerId)?.let(hooker.tickScheduler::cancel)
    }

    private fun isConfirmed(playerId: UUID): Boolean {
        return confirmationCache.getOrPut(playerId) {
            repository.hasConfirmed(playerId)
        }
    }

    private fun openConfirmationMenu(player: Player, request: PendingRequest) {
        val menu = MenuUiSupport.buildMenu(
            plugin = hooker.plugin,
            parser = parser,
            title = "<!i><#6A0000>▍ <#B00000>Правила использования",
            rows = MenuRows.FIVE,
            menuTopRange = 0 until MENU_SIZE,
            interactiveTopSlots = setOf(CONFIRM_SLOT, CANCEL_SLOT),
            allowPlayerInventoryClicks = false,
            onClose = {
                if (pendingRequests[player.uniqueId] === request) {
                    pendingRequests.remove(player.uniqueId)
                }
            }
        )

        MenuUiSupport.fillByMask(
            menu = menu,
            menuSize = MENU_SIZE,
            primarySlots = RED_FILLER_SLOTS,
            primaryButton = fillerButton(Material.RED_STAINED_GLASS_PANE),
            secondaryButton = fillerButton(Material.ORANGE_STAINED_GLASS_PANE)
        )

        menu.setButton(CONFIRM_SLOT, confirmButton(request) { handleConfirmationClick(player, it.menu) })
        menu.setButton(CANCEL_SLOT, cancelButton { scheduleMenuClose(player) })
        menu.open(player)
    }

    private fun handleConfirmationClick(player: Player, menu: Menu) {
        val request = pendingRequests[player.uniqueId] ?: return
        if (hasActiveClickCooldown(player.uniqueId)) {
            return
        }
        armClickCooldown(player.uniqueId)
        if (request.remainingConfirmations > 1) {
            request.remainingConfirmations -= 1
            menu.setButton(CONFIRM_SLOT, confirmButton(request) { handleConfirmationClick(player, it.menu) })
            return
        }

        pendingRequests.remove(player.uniqueId)
        confirmationCache[player.uniqueId] = true
        repository.saveConfirmed(player.uniqueId)

        val requestedSize = request.requestedSize
        hooker.tickScheduler.runLater(1L) {
            if (!player.isOnline) {
                return@runLater
            }
            player.closeInventory()
            if (!hooker.systemToggleService.isEnabled(ManagedSystem.PAINT)) {
                return@runLater
            }
            onConfirmed(player, requestedSize)
        }
    }

    private fun scheduleMenuClose(player: Player) {
        pendingRequests.remove(player.uniqueId)
        clickCooldownTasks.remove(player.uniqueId)?.let(hooker.tickScheduler::cancel)
        hooker.tickScheduler.runLater(1L) {
            if (player.isOnline) {
                player.closeInventory()
            }
        }
    }

    private fun fillerButton(material: Material): Button = Button.simple(
        ItemBuilder(material)
            .hideTooltip(true)
            .build()
    ).build()

    private fun cancelButton(action: () -> Unit): Button = Button.simple(
        ItemBuilder(Material.LIGHT_GRAY_WOOL)
            .name(parser.parse("<!i><gray>Отмена"))
            .build()
    ).action {
        action()
    }.build()

    private fun confirmButton(request: PendingRequest, action: (ru.violence.coreapi.bukkit.api.menu.event.ClickEvent) -> Unit): Button {
        return Button.simple(
            ItemBuilder(Material.RED_CONCRETE)
                .name(parser.parse("<!i><#FF2B2B>⚠ Соглашение с правилами"))
                .lore(confirmLore(request.remainingConfirmations).map(parser::parse))
                .build()
        ).action(action).build()
    }

    private fun confirmLore(remainingConfirmations: Int): List<String> = listOf(
        "",
        "<!i><#FF2B2B>Запрещены:",
        "<!i><#B00020> ● <#FFE5E5>Неподобающие фразы",
        "<!i><#B00020> ● <#FFE5E5>Сомнительная символика",
        "<!i><#B00020> ● <#FFE5E5>18+ рисунки",
        "",
        "<!i><#FF2B2B>Важно:",
        "<!i><#B00020> → <#FF8A8A>За нарушение вы можете быть",
        "<!i>    <#FFE5E5>наказаны<#FF8A8A> и лишены прав на команду.",
        "",
        "<!i><#B00020> → <#FF8A8A>Не пытайтесь <#FFE5E5>обойти <#FF8A8A>правила",
        "<!i>    <#FF8A8A>и просто пользуйтесь возможностью",
        "<!i>    <#FF8A8A>рисовать в своё<#FFE5E5> удовольствие.",
        "",
        "<!i><#FF2B2B>▍ Нажмите ещё <b><#FFE5E5>$remainingConfirmations</b><#FF2B2B> раз, чтобы подтвердить"
    )

    private fun hasActiveClickCooldown(playerId: UUID): Boolean = clickCooldownTasks.containsKey(playerId)

    private fun armClickCooldown(playerId: UUID) {
        clickCooldownTasks.remove(playerId)?.let(hooker.tickScheduler::cancel)
        clickCooldownTasks[playerId] = hooker.tickScheduler.runLater(CLICK_COOLDOWN_TICKS) {
            clickCooldownTasks.remove(playerId)
        }
    }

    private companion object {
        const val MENU_SIZE = 45
        const val CONFIRM_SLOT = 21
        const val CANCEL_SLOT = 23
        const val REQUIRED_CONFIRMATIONS = 10
        const val CLICK_COOLDOWN_TICKS = 20L

        val RED_FILLER_SLOTS = setOf(0, 1, 2, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 43, 44)
    }
}
