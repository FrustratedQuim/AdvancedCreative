package com.ratger.acreative.menus.edit.apply.core

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.apply.ApplyRequestManager
import com.ratger.acreative.menus.apply.ApplyPromptPresenter
import com.ratger.acreative.menus.apply.ApplyRequestResult
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.ItemEditSessionManager
import org.bukkit.entity.Player

class ItemEditorApplyDispatcher(
    private val hooker: FunctionHooker,
    private val sessionManager: ItemEditSessionManager,
    promptPresenter: ApplyPromptPresenter,
    handlers: Collection<EditorApplyHandler>
) {
    private data class ApplyRequest(
        val kind: EditorApplyActionKind,
        val reopen: (Player, ItemEditSession) -> Unit
    )

    private val handlersByKind = handlers.associateBy { it.kind }
    private val requestManager = ApplyRequestManager<ApplyRequest>(
        tickScheduler = hooker.tickScheduler,
        promptPresenter = promptPresenter,
        messageManager = hooker.messageManager
    )

    fun beginWaiting(player: Player, kind: EditorApplyActionKind, reopen: (Player, ItemEditSession) -> Unit) {
        val handler = handlersByKind[kind]
        if (handler == null) {
            hooker.plugin.logger.warning("Missing item editor apply handler for ${kind.name}")
            return
        }
        requestManager.begin(
            player = player,
            inputSpec = handler.inputSpec,
            payload = ApplyRequest(kind = kind, reopen = reopen),
            suggestions = { payload, args -> handlersByKind[payload.kind]?.suggestions(args).orEmpty() },
            onTimeout = { timeoutPlayer, payload -> reopenRequest(timeoutPlayer, payload) },
            onCancel = { cancelPlayer, payload -> reopenRequest(cancelPlayer, payload) },
            onApply = ::handleApplyValue
        )
    }

    fun isWaiting(player: Player): Boolean = requestManager.isWaiting(player)

    fun canPickupInCurrentState(player: Player): Boolean = isWaiting(player)

    fun handleApplyCommand(player: Player, args: Array<out String>) {
        requestManager.handle(player, args)
    }

    fun tabComplete(player: Player, args: Array<out String>): List<String> = requestManager.tabComplete(player, args)

    fun cancelWaiting(player: Player, reopenMenu: Boolean) {
        val request = requestManager.clear(player) ?: return
        if (reopenMenu) {
            reopenRequest(player, request)
        }
    }

    private fun handleApplyValue(
        player: Player,
        request: ApplyRequest,
        args: Array<out String>
    ): ApplyRequestResult {
        val session = sessionManager.getSession(player) ?: return ApplyRequestResult.Complete()
        val handler = handlersByKind[request.kind] ?: return ApplyRequestResult.KeepWaiting

        return when (handler.apply(player, session, args)) {
            ApplyExecutionResult.Success -> {
                sessionManager.markCurrentContentLoggedIfChanged(session)
                hooker.actionLogger.info {
                    "Item editor apply succeeded for ${hooker.actionLogger.playerRef(player)} kind=${request.kind.name.lowercase()} args=${args.joinToString(" ")}"
                }
                if (request.kind != EditorApplyActionKind.HEAD_LICENSED_NAME) {
                    MenuSoundSupport.success(player)
                }
                ApplyRequestResult.Complete { completePlayer -> reopenRequest(completePlayer, request) }
            }
            ApplyExecutionResult.InvalidValue -> {
                hooker.messageManager.sendChat(player, MessageKey.EDIT_APPLY_INVALID_VALUE)
                MenuSoundSupport.error(player)
                ApplyRequestResult.KeepWaiting
            }
            ApplyExecutionResult.UnknownValue -> {
                hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_VALUE)
                MenuSoundSupport.error(player)
                ApplyRequestResult.KeepWaiting
            }
            ApplyExecutionResult.AwaitingAsync -> ApplyRequestResult.KeepWaiting
        }
    }

    private fun reopenRequest(player: Player, request: ApplyRequest) {
        val session = sessionManager.getSession(player) ?: return
        request.reopen(player, session)
    }
}
