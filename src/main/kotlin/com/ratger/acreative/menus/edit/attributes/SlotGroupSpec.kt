package com.ratger.acreative.menus.edit.attributes

enum class SlotGroupSpec {
    MAINHAND,
    OFFHAND,
    HAND,
    FEET,
    LEGS,
    CHEST,
    HEAD,
    ARMOR,
    BODY,
    ANY;

    companion object {
        fun fromToken(input: String): SlotGroupSpec? {
            return when (input.lowercase()) {
                "mainhand" -> MAINHAND
                "offhand" -> OFFHAND
                "hand" -> HAND
                "feet" -> FEET
                "legs" -> LEGS
                "chest" -> CHEST
                "head" -> HEAD
                "armor" -> ARMOR
                "body" -> BODY
                "any" -> ANY
                else -> null
            }
        }
    }
}
