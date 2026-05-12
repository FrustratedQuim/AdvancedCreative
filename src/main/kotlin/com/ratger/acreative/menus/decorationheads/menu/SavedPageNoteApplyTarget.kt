package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.core.TickScheduler
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.apply.ApplyRequestManager
import com.ratger.acreative.menus.apply.ApplyRequestResult
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.apply.ApplyPromptPresenter
import org.bukkit.entity.Player

class SavedPageNoteApplyTarget(
    tickScheduler: TickScheduler,
    messageManager: MessageManager,
    promptPresenter: ApplyPromptPresenter,
    private val onApply: (player: Player, pageId: Long, note: String?) -> Unit,
    private val onReopen: (player: Player, pageId: Long) -> Unit
) : ApplyCommandTarget {
    private val requestManager = ApplyRequestManager<Long>(
        tickScheduler = tickScheduler,
        promptPresenter = promptPresenter,
        messageManager = messageManager
    )

    fun begin(player: Player, pageId: Long) {
        requestManager.begin(
            player = player,
            inputSpec = ApplyInputSpecs.TEXT,
            payload = pageId,
            onTimeout = { timeoutPlayer, timeoutPageId -> onReopen(timeoutPlayer, timeoutPageId) },
            onCancel = { cancelPlayer, cancelPageId -> onReopen(cancelPlayer, cancelPageId) },
            onApply = { applyPlayer, applyPageId, args ->
                val note = args.joinToString(" ").trim().takeIf { it.isNotBlank() }
                MenuSoundSupport.success(applyPlayer)
                ApplyRequestResult.Complete { completePlayer -> onApply(completePlayer, applyPageId, note) }
            }
        )
    }

    override fun isWaiting(player: Player): Boolean = requestManager.isWaiting(player)

    override fun handle(player: Player, args: Array<out String>): Boolean = requestManager.handle(player, args)

    override fun tabComplete(player: Player, args: Array<out String>): List<String> = requestManager.tabComplete(player, args)

    override fun cancel(player: Player) = requestManager.cancel(player)
}
