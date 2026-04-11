package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

interface EditorApplyHandler {
    val kind: EditorApplyKind

    fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult

    fun suggestions(args: Array<out String>): List<String>
}

enum class EditorApplyKind {
    ITEM_ID,
    NAME_TEXT,
    LORE_TEXT,
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
