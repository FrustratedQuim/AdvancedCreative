package com.ratger.acreative.menus.banner.menu

import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.core.TickScheduler
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.apply.ApplyRequestManager
import com.ratger.acreative.menus.apply.ApplyRequestResult
import com.ratger.acreative.menus.banner.model.BannerPostDraft
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.apply.ApplyPromptPresenter
import org.bukkit.entity.Player

class BannerTitleApplyTarget(
    tickScheduler: TickScheduler,
    messageManager: MessageManager,
    promptPresenter: ApplyPromptPresenter,
    private val onApply: (Player, String?) -> Unit,
    private val onReopen: (Player) -> Unit
) : ApplyCommandTarget {
    private val requestManager = ApplyRequestManager<Unit>(
        tickScheduler = tickScheduler,
        promptPresenter = promptPresenter,
        messageManager = messageManager
    )

    fun begin(player: Player) {
        requestManager.begin(
            player = player,
            inputSpec = ApplyInputSpecs.TEXT,
            payload = Unit,
            onTimeout = { timeoutPlayer, _ -> onReopen(timeoutPlayer) },
            onCancel = { cancelPlayer, _ -> onReopen(cancelPlayer) },
            onApply = { applyPlayer, _, args ->
                val title = BannerPostDraft.normalizeTitle(args.joinToString(" "))
                MenuSoundSupport.success(applyPlayer)
                ApplyRequestResult.Complete { completePlayer -> onApply(completePlayer, title) }
            }
        )
    }

    override fun isWaiting(player: Player): Boolean = requestManager.isWaiting(player)

    override fun handle(player: Player, args: Array<out String>): Boolean = requestManager.handle(player, args)

    override fun tabComplete(player: Player, args: Array<out String>): List<String> = requestManager.tabComplete(player, args)

    override fun cancel(player: Player) = requestManager.cancel(player)
}
