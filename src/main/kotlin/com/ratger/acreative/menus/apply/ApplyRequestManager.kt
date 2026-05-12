package com.ratger.acreative.menus.apply

import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.core.TickScheduler
import com.ratger.acreative.menus.common.MenuSoundSupport
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

interface ApplyPromptPresenter {
    fun showPrompt(player: Player, inputSpec: ApplyInputSpec, applyTimeoutSeconds: Int)
    fun clearPrompt(player: Player)
}

sealed interface ApplyRequestResult {
    data object KeepWaiting : ApplyRequestResult
    data class Complete(val afterClear: (Player) -> Unit = {}) : ApplyRequestResult
}

/**
 * Shared /apply lifecycle manager: request registration, timeout, usage, cancel,
 * suggestions and prompt cleanup. It intentionally does not know domain mutation logic.
 */
class ApplyRequestManager<T : Any>(
    private val tickScheduler: TickScheduler,
    private val promptPresenter: ApplyPromptPresenter,
    private val messageManager: MessageManager,
    private val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
) : ApplyCommandTarget {
    private data class StoredRequest<T : Any>(
        val payload: T,
        val inputSpec: ApplyInputSpec,
        val timeoutTaskId: Int,
        val suggestions: (T, Array<out String>) -> List<String>,
        val onTimeout: (Player, T) -> Unit,
        val onCancel: (Player, T) -> Unit,
        val onApply: (Player, T, Array<out String>) -> ApplyRequestResult
    )

    private val requests: MutableMap<UUID, StoredRequest<T>> = mutableMapOf()
    private val timeoutTicks: Long = timeoutSeconds * TICKS_PER_SECOND

    fun begin(
        player: Player,
        inputSpec: ApplyInputSpec,
        payload: T,
        suggestions: (T, Array<out String>) -> List<String> = { _, _ -> emptyList() },
        onTimeout: (Player, T) -> Unit,
        onCancel: (Player, T) -> Unit,
        onApply: (Player, T, Array<out String>) -> ApplyRequestResult
    ) {
        clearRequest(player)

        val timeoutTaskId = tickScheduler.runLater(timeoutTicks) {
            val onlinePlayer = Bukkit.getPlayer(player.uniqueId) ?: return@runLater
            val request = clearRequest(onlinePlayer) ?: return@runLater
            request.onTimeout(onlinePlayer, request.payload)
        }

        requests[player.uniqueId] = StoredRequest(
            payload = payload,
            inputSpec = inputSpec,
            timeoutTaskId = timeoutTaskId,
            suggestions = suggestions,
            onTimeout = onTimeout,
            onCancel = onCancel,
            onApply = onApply
        )
        promptPresenter.showPrompt(player, inputSpec, timeoutSeconds)
    }

    override fun isWaiting(player: Player): Boolean = requests.containsKey(player.uniqueId)

    override fun handle(player: Player, args: Array<out String>): Boolean {
        val request = requests[player.uniqueId] ?: return false
        if (args.isEmpty()) {
            messageManager.sendChat(player, request.inputSpec.usageMessageKey)
            MenuSoundSupport.error(player)
            return true
        }

        if (args[0].equals(CANCEL_ARGUMENT, ignoreCase = true)) {
            val cleared = clearRequest(player) ?: return false
            cleared.onCancel(player, cleared.payload)
            return true
        }

        return when (val result = request.onApply(player, request.payload, args)) {
            ApplyRequestResult.KeepWaiting -> true
            is ApplyRequestResult.Complete -> {
                clearRequest(player)
                result.afterClear(player)
                true
            }
        }
    }

    override fun tabComplete(player: Player, args: Array<out String>): List<String> {
        val request = requests[player.uniqueId] ?: return emptyList()
        val cancel = if (args.size == 1 && CANCEL_ARGUMENT.startsWith(args[0], ignoreCase = true)) listOf(CANCEL_ARGUMENT) else emptyList()
        return (request.suggestions(request.payload, args) + cancel).distinct()
    }

    override fun cancel(player: Player) {
        clearRequest(player)
    }

    fun clear(player: Player): T? = clearRequest(player)?.payload

    private fun clearRequest(player: Player): StoredRequest<T>? {
        val request = requests.remove(player.uniqueId) ?: return null
        tickScheduler.cancel(request.timeoutTaskId)
        promptPresenter.clearPrompt(player)
        return request
    }

    private companion object {
        const val DEFAULT_TIMEOUT_SECONDS: Int = 30
        const val TICKS_PER_SECOND: Long = 20L
        const val CANCEL_ARGUMENT: String = "cancel"
    }
}
