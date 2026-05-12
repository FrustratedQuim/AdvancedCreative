package com.ratger.acreative.menus.apply

import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.MessageManager
import org.bukkit.entity.Player
import java.time.Duration

class DefaultApplyPromptPresenter(
    private val messageManager: MessageManager
) : ApplyPromptPresenter {
    override fun showPrompt(player: Player, inputSpec: ApplyInputSpec, applyTimeoutSeconds: Int) {
        messageManager.sendTitle(
            player = player,
            titleKey = MessageKey.EDIT_APPLY_PROMPT_TITLE,
            subtitleKey = inputSpec.promptSubtitleKey,
            fadeIn = Duration.ofMillis(500L),
            stay = Duration.ofSeconds((applyTimeoutSeconds + 10).toLong()),
            fadeOut = Duration.ofMillis(500L)
        )
    }

    override fun clearPrompt(player: Player) {
        messageManager.sendTitle(
            player = player,
            titleKey = MessageKey.EDIT_PROMPT_CLEAR,
            subtitleKey = MessageKey.EDIT_PROMPT_CLEAR,
            fadeIn = Duration.ZERO,
            stay = Duration.ZERO,
            fadeOut = Duration.ZERO
        )
    }
}
