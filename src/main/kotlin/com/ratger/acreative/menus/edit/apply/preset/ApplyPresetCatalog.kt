package com.ratger.acreative.menus.edit.apply.preset

import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind

/**
 * Centralized catalog for apply command presets grouped by domain/action kind.
 * Provides consistent suggestion values across all handlers and removes
 * duplication of string literals.
 */
object ApplyPresetCatalog {

    // region Tool presets
    private val MINING_SPEED_PRESETS = listOf("0", "0.5", "5", "10", "50")
    private val DAMAGE_PRESETS = listOf("0", "15", "250", "15%", "50%", "max")
    private val DAMAGE_PER_BLOCK_PRESETS = listOf("0", "1", "5", "25", "100", "10%", "25%", "max")
    // endregion

    // region Meta presets
    private val STACK_SIZE_PRESETS = listOf("1", "8", "16", "32", "64", "max")
    private val AMOUNT_PRESETS = listOf("1", "8", "16", "32", "64")
    private val MAX_DURABILITY_PRESETS = listOf("5", "25", "150", "500", "10000")
    private val MAP_ID_PRESETS = listOf("1", "5", "10")
    // endregion

    // region Effects presets
    private val FOOD_NUTRITION_AND_SATURATION_PRESETS = listOf("2", "4", "8", "20")
    private val USE_COOLDOWN_SECONDS_PRESETS = listOf("0.5", "1", "5")
    private val CONSUMABLE_CONSUME_SECONDS_PRESETS = listOf("0.1", "0.5", "1.5", "5")
    private val TELEPORT_DIAMETER_PRESETS = listOf("5", "10", "15", "50", "100")
    // endregion

    // region Death Protection presets (reuses some effect presets)
    // TELEPORT_DIAMETER_PRESETS shared with consumables
    // endregion

    /**
     * Returns the preset values for the given action kind.
     * If a handler needs custom presets not in the catalog, it should override [getPresets]
     * or use its own property instead of relying on this method.
     */
    fun getPresets(kind: EditorApplyActionKind): List<String> = when (kind) {
        EditorApplyActionKind.MINING_SPEED -> MINING_SPEED_PRESETS
        EditorApplyActionKind.DAMAGE -> DAMAGE_PRESETS
        EditorApplyActionKind.DAMAGE_PER_BLOCK -> DAMAGE_PER_BLOCK_PRESETS
        EditorApplyActionKind.STACK_SIZE -> STACK_SIZE_PRESETS
        EditorApplyActionKind.AMOUNT -> AMOUNT_PRESETS
        EditorApplyActionKind.MAX_DURABILITY -> MAX_DURABILITY_PRESETS
        EditorApplyActionKind.MAP_ID -> MAP_ID_PRESETS
        EditorApplyActionKind.FOOD_NUTRITION,
        EditorApplyActionKind.FOOD_SATURATION -> FOOD_NUTRITION_AND_SATURATION_PRESETS
        EditorApplyActionKind.USE_COOLDOWN_SECONDS -> USE_COOLDOWN_SECONDS_PRESETS
        EditorApplyActionKind.CONSUMABLE_CONSUME_SECONDS -> CONSUMABLE_CONSUME_SECONDS_PRESETS
        EditorApplyActionKind.CONSUMABLE_RANDOM_TELEPORT_DIAMETER,
        EditorApplyActionKind.DEATH_PROTECTION_RANDOM_TELEPORT_DIAMETER -> TELEPORT_DIAMETER_PRESETS

        // Other kinds without presets (text, lore, enchant, etc.)
        EditorApplyActionKind.ITEM_ID,
        EditorApplyActionKind.NAME_TEXT,
        EditorApplyActionKind.LORE_TEXT,
        EditorApplyActionKind.NAME_RAW_MINIMESSAGE,
        EditorApplyActionKind.LORE_RAW_MINIMESSAGE_LINE,
        EditorApplyActionKind.ITEM_MODEL,
        EditorApplyActionKind.ATTRIBUTE,
        EditorApplyActionKind.EQUIP_SOUND,
        EditorApplyActionKind.ENCHANTMENT,
        EditorApplyActionKind.USE_COOLDOWN_GROUP,
        EditorApplyActionKind.CAN_PLACE_ON,
        EditorApplyActionKind.CAN_BREAK,
        EditorApplyActionKind.HEAD_ONLINE_NAME,
        EditorApplyActionKind.HEAD_TEXTURE_VALUE,
        EditorApplyActionKind.HEAD_LICENSED_NAME,
        EditorApplyActionKind.POTION_COLOR,
        EditorApplyActionKind.MAP_COLOR,
        EditorApplyActionKind.POTION_EFFECT_ADD,
        EditorApplyActionKind.DEATH_PROTECTION_SOUND,
        EditorApplyActionKind.DEATH_PROTECTION_REMOVE_EFFECT_ADD,
        EditorApplyActionKind.DEATH_PROTECTION_APPLY_EFFECT_ADD,
        EditorApplyActionKind.CONSUMABLE_SOUND,
        EditorApplyActionKind.CONSUMABLE_REMOVE_EFFECT_ADD,
        EditorApplyActionKind.CONSUMABLE_APPLY_EFFECT_ADD -> emptyList()
    }
}
