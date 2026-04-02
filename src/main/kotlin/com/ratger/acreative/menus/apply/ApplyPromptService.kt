package com.ratger.acreative.menus.apply

import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.core.MessageKey
import org.bukkit.entity.Player
import java.time.Duration

class ApplyPromptService(
    private val messageManager: MessageManager
) {
    fun showPrompt(player: Player, applyTimeoutSeconds: Int) {
        messageManager.sendTitle(
            player = player,
            titleKey = MessageKey.EDIT_APPLY_PROMPT_TITLE,
            subtitleKey = MessageKey.EDIT_APPLY_PROMPT_SUBTITLE,
            fadeIn = Duration.ofMillis(500L),
            stay = Duration.ofSeconds((applyTimeoutSeconds + 10).toLong()),
            fadeOut = Duration.ofMillis(500L)
        )
    }

    fun clearPrompt(player: Player) {
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
