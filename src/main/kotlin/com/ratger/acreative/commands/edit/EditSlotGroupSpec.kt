package com.ratger.acreative.commands.edit

enum class EditSlotGroupSpec {
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
        fun fromToken(input: String): EditSlotGroupSpec? {
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
