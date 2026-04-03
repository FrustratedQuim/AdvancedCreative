package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import com.ratger.acreative.menus.itemEdit.ItemEditSessionManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class ItemEditorApplyStateManager(
    private val hooker: FunctionHooker,
    private val sessionManager: ItemEditSessionManager,
    private val promptService: ApplyPromptService,
    handlers: Collection<EditorApplyHandler>
) {
    companion object {
        private const val TIMEOUT_SECONDS: Int = 30
        private const val TIMEOUT_TICKS: Long = 20L * TIMEOUT_SECONDS
    }

    private data class ApplyRequest(
        val kind: EditorApplyKind,
        val reopen: (Player, ItemEditSession) -> Unit,
        val timeoutTaskId: Int
    )

    private val handlersByKind = handlers.associateBy { it.kind }
    private val requests = mutableMapOf<UUID, ApplyRequest>()

    fun beginWaiting(player: Player, kind: EditorApplyKind, reopen: (Player, ItemEditSession) -> Unit) {
        cancelWaiting(player, reopenMenu = false)

        val timeoutTask = hooker.tickScheduler.runLater(TIMEOUT_TICKS) {
            val onlinePlayer = Bukkit.getPlayer(player.uniqueId) ?: return@runLater
            cancelWaiting(onlinePlayer, reopenMenu = true)
        }

        requests[player.uniqueId] = ApplyRequest(
            kind = kind,
            reopen = reopen,
            timeoutTaskId = timeoutTask
        )

        promptService.showPrompt(player, kind, TIMEOUT_SECONDS)
    }

    fun isWaiting(player: Player): Boolean = requests.containsKey(player.uniqueId)

    fun canPickupInCurrentState(player: Player): Boolean = isWaiting(player)

    fun handleApplyCommand(player: Player, args: Array<out String>) {
        val request = requests[player.uniqueId] ?: return

        if (args.isEmpty()) {
            hooker.messageManager.sendChat(player, usageMessageFor(request.kind))
            return
        }

        if (args.size != 1) {
            hooker.messageManager.sendChat(player, MessageKey.EDIT_APPLY_INVALID_VALUE)
            return
        }

        val rawValue = args[0]
        if (rawValue.equals("cancel", ignoreCase = true)) {
            cancelWaiting(player, reopenMenu = true)
            return
        }

        val session = sessionManager.getSession(player)
        if (session == null) {
            cancelWaiting(player, reopenMenu = false)
            return
        }

        val handler = handlersByKind[request.kind] ?: return
        when (handler.apply(player, session, rawValue)) {
            ApplyExecutionResult.Success -> cancelWaiting(player, reopenMenu = true)
            ApplyExecutionResult.InvalidValue -> hooker.messageManager.sendChat(player, MessageKey.EDIT_APPLY_INVALID_VALUE)
        }
    }

    fun tabComplete(player: Player, args: Array<out String>): List<String> {
        val request = requests[player.uniqueId] ?: return emptyList()
        if (args.size != 1) return emptyList()

        val prefix = args[0]
        val handler = handlersByKind[request.kind] ?: return emptyList()
        val values = handler.suggestions(prefix)
        val cancel = if ("cancel".startsWith(prefix, ignoreCase = true)) listOf("cancel") else emptyList()
        return values + cancel
    }

    fun cancelWaiting(player: Player, reopenMenu: Boolean) {
        val request = requests.remove(player.uniqueId) ?: return
        hooker.tickScheduler.cancel(request.timeoutTaskId)
        promptService.clearPrompt(player)

        if (!reopenMenu) return

        val session = sessionManager.getSession(player) ?: return
        request.reopen(player, session)
    }

    private fun usageMessageFor(kind: EditorApplyKind): MessageKey {
        return when (kind) {
            EditorApplyKind.ITEM_ID -> MessageKey.EDIT_APPLY_USAGE_ID
            EditorApplyKind.AMOUNT -> MessageKey.EDIT_APPLY_USAGE_AMOUNT
            EditorApplyKind.ITEM_MODEL -> MessageKey.EDIT_APPLY_USAGE_ID
            EditorApplyKind.STACK_SIZE -> MessageKey.EDIT_APPLY_USAGE_AMOUNT
        }
    }
}
