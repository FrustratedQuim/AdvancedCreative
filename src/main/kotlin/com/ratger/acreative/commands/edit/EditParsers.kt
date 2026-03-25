package com.ratger.acreative.commands.edit

import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemRarity
import org.bukkit.potion.PotionEffectType

class EditParsers {
    fun namespacedKey(input: String): NamespacedKey? = NamespacedKey.fromString(input)

    fun color(input: String): Int? {
        val value = input.trim()
        return when {
            value.startsWith("#") && value.length == 7 -> value.substring(1).toIntOrNull(16)
            else -> value.toIntOrNull()?.takeIf { it in 0..0xFFFFFF }
        }
    }

    fun enchantment(input: String): Enchantment? = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(input.lowercase()))
    fun effect(input: String): PotionEffectType? = Registry.EFFECT.get(NamespacedKey.minecraft(input.lowercase()))

    fun rarity(input: String): ItemRarity? = when (input.lowercase()) {
        "common" -> ItemRarity.COMMON
        "uncommon" -> ItemRarity.UNCOMMON
        "rare" -> ItemRarity.RARE
        "epic" -> ItemRarity.EPIC
        else -> null
    }

    fun attribute(input: String): Attribute? = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(input.lowercase()))

    fun attributeOperation(input: String): AttributeModifier.Operation? = when (input.lowercase()) {
        "add_number" -> AttributeModifier.Operation.ADD_NUMBER
        "add_scalar", "add_multiplied_base" -> AttributeModifier.Operation.ADD_SCALAR
        "multiply_scalar_1", "add_multiplied_total" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1
        else -> null
    }

    fun slotGroup(input: String): EquipmentSlotGroup? {
        return when (input.lowercase()) {
            "mainhand" -> EquipmentSlotGroup.MAINHAND
            "offhand" -> EquipmentSlotGroup.OFFHAND
            "hand" -> EquipmentSlotGroup.HAND
            "feet" -> EquipmentSlotGroup.FEET
            "legs" -> EquipmentSlotGroup.LEGS
            "chest" -> EquipmentSlotGroup.CHEST
            "head" -> EquipmentSlotGroup.HEAD
            "armor" -> EquipmentSlotGroup.ARMOR
            "body" -> EquipmentSlotGroup.BODY
            "any" -> EquipmentSlotGroup.ANY
            else -> null
        }
    }

    fun rawTail(args: Array<out String>, from: Int): String = if (args.size <= from) "" else args.copyOfRange(from, args.size).joinToString(" ")

    fun parseAction(args: Array<out String>): EditAction? {
        if (args.isEmpty()) return EditAction.Show
        return when (args[0].lowercase()) {
            "show" -> EditAction.Show
            "reset" -> EditAction.Reset(args.getOrNull(1)?.lowercase() ?: "")
            "name" -> when (args.getOrNull(1)?.lowercase()) {
                "set" -> EditAction.NameSet(rawTail(args, 2))
                "clear" -> EditAction.NameClear
                else -> null
            }

            "lore" -> when (args.getOrNull(1)?.lowercase()) {
                "add" -> EditAction.LoreAdd(rawTail(args, 2))
                "set" -> EditAction.LoreSet(args.getOrNull(2)?.toIntOrNull() ?: return null, rawTail(args, 3))
                "remove" -> EditAction.LoreRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
                "clear" -> EditAction.LoreClear
                else -> null
            }

            "component" -> parseComponent(args)
            "enchant" -> parseEnchant(args)
            "tooltip" -> EditAction.TooltipToggle(args.getOrNull(1) ?: return null, args.getOrNull(2)?.lowercase() == "hide")
            "can_place_on" -> parseNamespacedSet(args, true)
            "can_break" -> parseNamespacedSet(args, false)
            "potion" -> parsePotion(args)
            "head" -> parseHead(args)
            "attribute" -> parseAttribute(args)
            "consumable", "death_protection", "tool", "equippable", "remainder", "lock" -> EditAction.Reset("unsupported:${args[0].lowercase()}")
            else -> null
        }
    }

    private fun parseComponent(args: Array<out String>): EditAction? {
        val node = args.getOrNull(1)?.lowercase() ?: return null
        val arg = args.getOrNull(2)
        return when (node) {
            "item_model" -> EditAction.SetItemModel(arg?.let { namespacedKey(it) })
            "unbreakable" -> EditAction.SetUnbreakable(arg == "on")
            "glider" -> EditAction.SetGlider(arg == "on")
            "max_damage" -> EditAction.SetMaxDamage(arg?.toIntOrNull())
            "damage" -> EditAction.SetDamage(arg?.toIntOrNull() ?: return null)
            "max_stack_size" -> EditAction.SetMaxStackSize(arg?.toIntOrNull())
            "rarity" -> EditAction.SetRarity(arg?.let { rarity(it) })
            "tooltip_style" -> EditAction.SetTooltipStyle(
                when (arg?.lowercase()) {
                    "basic" -> null
                    "broken" -> NamespacedKey.minecraft("null")
                    else -> return null
                }
            )
            else -> null
        }
    }

    private fun parseEnchant(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "add" -> EditAction.EnchantAdd(enchantment(args.getOrNull(2) ?: return null) ?: return null, args.getOrNull(3)?.toIntOrNull() ?: return null)
            "remove" -> EditAction.EnchantRemove(enchantment(args.getOrNull(2) ?: return null) ?: return null)
            "clear" -> EditAction.EnchantClear
            "glint" -> EditAction.SetEnchantmentGlint(
                when (args.getOrNull(2)?.lowercase()) {
                    "on" -> true
                    "off" -> false
                    "default" -> null
                    else -> return null
                }
            )

            "tooltip" -> EditAction.TooltipToggle("enchantments", args.getOrNull(2)?.lowercase() == "hide")
            else -> null
        }
    }

    private fun parseNamespacedSet(args: Array<out String>, place: Boolean): EditAction? {
        val keys = args.drop(1).mapNotNull { namespacedKey(it) }.toSet()
        return if (place) EditAction.SetCanPlaceOn(keys) else EditAction.SetCanBreak(keys)
    }

    private fun parsePotion(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "color" -> EditAction.PotionColor(args.getOrNull(2)?.takeUnless { it == "clear" }?.let { color(it) })
            "effect_add" -> EditAction.PotionEffectAdd(
                effect(args.getOrNull(2) ?: return null) ?: return null,
                args.getOrNull(3)?.toIntOrNull() ?: return null,
                args.getOrNull(4)?.toIntOrNull() ?: return null,
                args.getOrNull(5)?.toBooleanStrictOrNull() ?: true,
                args.getOrNull(6)?.toBooleanStrictOrNull() ?: true,
                args.getOrNull(7)?.toBooleanStrictOrNull() ?: true
            )

            "effect_remove" -> EditAction.PotionEffectRemove(effect(args.getOrNull(2) ?: return null) ?: return null)
            "effect_clear" -> EditAction.PotionEffectClear
            else -> null
        }
    }

    private fun parseHead(args: Array<out String>): EditAction? = when (args.getOrNull(1)?.lowercase()) {
        "texture" -> EditAction.HeadTextureSet(rawTail(args, 2))
        "clear" -> EditAction.HeadTextureClear
        else -> null
    }

    private fun parseAttribute(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "add" -> {
                val attribute = attribute(args.getOrNull(2) ?: return null) ?: return null
                val amount = args.getOrNull(3)?.toDoubleOrNull() ?: return null
                val operation = attributeOperation(args.getOrNull(4) ?: return null) ?: return null
                EditAction.AttributeAdd(attribute, amount, operation, args.getOrNull(5))
            }

            "remove" -> EditAction.AttributeRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "clear" -> EditAction.AttributeClear
            else -> null
        }
    }

    fun enchantSuggestions(prefix: String): List<String> = Registry.ENCHANTMENT.iterator().asSequence().map { it.key.key }.filter { it.startsWith(prefix, true) }.sorted().toList()
    fun effectSuggestions(prefix: String): List<String> = Registry.EFFECT.iterator().asSequence().map { it.key.key }.filter { it.startsWith(prefix, true) }.sorted().toList()
    fun attributeSuggestions(prefix: String): List<String> = Registry.ATTRIBUTE.iterator().asSequence().map { it.key.key }.filter { it.startsWith(prefix, true) }.sorted().toList()
}
