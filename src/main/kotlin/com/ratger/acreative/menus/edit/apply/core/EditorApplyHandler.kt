package com.ratger.acreative.menus.edit.apply.core

import com.ratger.acreative.menus.apply.ApplyInputSpec
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

/**
 * Domain item-editor action handler. Each handler exposes input metadata next to
 * its mutation logic, so adding a new editor apply no longer requires updating a
 * separate prompt/usage registry.
 */
interface EditorApplyHandler {
    val kind: EditorApplyActionKind
    val inputSpec: ApplyInputSpec

    fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult

    fun suggestions(args: Array<out String>): List<String>
}

enum class EditorApplyActionKind {
    ITEM_ID,
    NAME_TEXT,
    LORE_TEXT,
    NAME_RAW_MINIMESSAGE,
    LORE_RAW_MINIMESSAGE_LINE,
    AMOUNT,
    ITEM_MODEL,
    STACK_SIZE,
    ATTRIBUTE,
    EQUIP_SOUND,
    ENCHANTMENT,
    MAX_DURABILITY,
    DAMAGE,
    MINING_SPEED,
    DAMAGE_PER_BLOCK,
    USE_COOLDOWN_SECONDS,
    USE_COOLDOWN_GROUP,
    COMMAND,
    CAN_PLACE_ON,
    CAN_BREAK,
    HEAD_ONLINE_NAME,
    HEAD_TEXTURE_VALUE,
    HEAD_LICENSED_NAME,
    POTION_COLOR,
    MAP_COLOR,
    MAP_ID,
    POTION_EFFECT_ADD,
    DEATH_PROTECTION_SOUND,
    DEATH_PROTECTION_REMOVE_EFFECT_ADD,
    DEATH_PROTECTION_RANDOM_TELEPORT_DIAMETER,
    DEATH_PROTECTION_APPLY_EFFECT_ADD,
    FOOD_NUTRITION,
    FOOD_SATURATION,
    CONSUMABLE_CONSUME_SECONDS,
    CONSUMABLE_SOUND,
    CONSUMABLE_REMOVE_EFFECT_ADD,
    CONSUMABLE_RANDOM_TELEPORT_DIAMETER,
    CONSUMABLE_APPLY_EFFECT_ADD
}

sealed interface ApplyExecutionResult {
    data object Success : ApplyExecutionResult
    data object InvalidValue : ApplyExecutionResult
    data object UnknownValue : ApplyExecutionResult
    data object AwaitingAsync : ApplyExecutionResult
}
