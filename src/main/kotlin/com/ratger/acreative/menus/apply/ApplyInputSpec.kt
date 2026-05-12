package com.ratger.acreative.menus.apply

import com.ratger.acreative.core.MessageKey

/**
 * Reusable UX contract for one /apply input type.
 *
 * The spec describes only prompt/usage metadata for a value requested from a player.
 * Domain handlers remain responsible for parsing rules that are specific to their action
 * and for applying validated values to item/banner/plot state.
 */
data class ApplyInputSpec(
    val usageMessageKey: MessageKey,
    val promptSubtitleKey: MessageKey
)

object ApplyInputSpecs {
    val ID = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_ID, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ID)
    val AMOUNT = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_AMOUNT, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_AMOUNT)
    val ATTRIBUTE = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_ATTRIBUTE, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ATTRIBUTE)
    val SOUND = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_SOUND, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_SOUND)
    val ENCHANTMENT = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_ENCHANTMENT, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_ENCHANTMENT)
    val USE_COOLDOWN_GROUP = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_USE_COOLDOWN_GROUP, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_USE_COOLDOWN_GROUP)
    val COMMAND = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_COMMAND, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_COMMAND)
    val NAME = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_NAME, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_NAME)
    val TEXT = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_TEXT, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_TEXT)
    val VALUE = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_VALUE, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_VALUE)
    val POTION_COLOR = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_POTION_COLOR, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_POTION_COLOR)
    val POTION_EFFECT = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_POTION_EFFECT, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_POTION_EFFECT)
    val EFFECT = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_EFFECT, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_EFFECT)
    val CONSUME_SECONDS = ApplyInputSpec(MessageKey.EDIT_APPLY_USAGE_CONSUME_SECONDS, MessageKey.EDIT_APPLY_PROMPT_SUBTITLE_CONSUME_SECONDS)
}
