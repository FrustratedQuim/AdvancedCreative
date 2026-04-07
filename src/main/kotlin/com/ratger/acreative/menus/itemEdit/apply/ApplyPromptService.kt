package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.core.MessageKey
import org.bukkit.entity.Player
import java.time.Duration

class ApplyPromptService(
    private val messageManager: MessageManager
) {
    fun showPrompt(player: Player, kind: EditorApplyKind, applyTimeoutSeconds: Int) {
        val subtitleKey = when (kind) {
            EditorApplyKind.ITEM_ID, EditorApplyKind.ITEM_MODEL -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ID
            EditorApplyKind.AMOUNT, EditorApplyKind.STACK_SIZE -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_AMOUNT
            EditorApplyKind.ATTRIBUTE -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ATTRIBUTE
            EditorApplyKind.EQUIP_SOUND -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_SOUND
            EditorApplyKind.ENCHANTMENT -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ENCHANTMENT
        }
        messageManager.sendTitle(
            player = player,
            titleKey = MessageKey.EDIT_APPLY_PROMPT_TITLE,
            subtitleKey = subtitleKey,
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
