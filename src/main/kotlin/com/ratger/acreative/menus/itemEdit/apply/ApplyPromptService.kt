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
            EditorApplyKind.MAX_DURABILITY,
            EditorApplyKind.DAMAGE,
            EditorApplyKind.MINING_SPEED,
            EditorApplyKind.DAMAGE_PER_BLOCK,
            EditorApplyKind.USE_COOLDOWN_SECONDS -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_AMOUNT
            EditorApplyKind.USE_COOLDOWN_GROUP -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_USE_COOLDOWN_GROUP
            EditorApplyKind.CAN_PLACE_ON,
            EditorApplyKind.CAN_BREAK -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ID
            EditorApplyKind.HEAD_ONLINE_NAME,
            EditorApplyKind.HEAD_LICENSED_NAME -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_NAME
            EditorApplyKind.HEAD_TEXTURE_VALUE -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_VALUE
            EditorApplyKind.POTION_COLOR -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_POTION_COLOR
            EditorApplyKind.POTION_EFFECT_ADD -> MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_POTION_EFFECT
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
