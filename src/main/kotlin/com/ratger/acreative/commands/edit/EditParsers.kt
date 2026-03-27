package com.ratger.acreative.commands.edit

import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import net.kyori.adventure.key.Key
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemRarity
import org.bukkit.potion.PotionEffectType
import org.bukkit.Material

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
    fun effectFromToken(input: String): PotionEffectType? {
        val key = NamespacedKey.fromString(input.lowercase()) ?: NamespacedKey.minecraft(input.lowercase())
        return Registry.EFFECT.get(key)
    }
    fun parseAdventureKey(input: String): Key? = runCatching { Key.key(input.lowercase()) }.getOrNull()
    fun parseCooldownGroup(input: String): Key? = runCatching { Key.key(input.lowercase()) }.getOrNull()
    fun parseBooleanStrict(input: String?): Boolean? = input?.lowercase()?.let {
        when (it) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    fun rarity(input: String): ItemRarity? = when (input.lowercase()) {
        "common" -> ItemRarity.COMMON
        "uncommon" -> ItemRarity.UNCOMMON
        "rare" -> ItemRarity.RARE
        "epic" -> ItemRarity.EPIC
        else -> null
    }

    fun attribute(input: String): Attribute? = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(input.lowercase()))
    fun material(input: String): Material? {
        val normalized = input.lowercase()
        val namespaced = NamespacedKey.fromString(normalized)
        if (namespaced != null) {
            return Registry.MATERIAL.get(namespaced)
        }
        return Registry.MATERIAL.get(NamespacedKey.minecraft(normalized))
    }

    fun attributeOperation(input: String): AttributeModifier.Operation? = when (input.lowercase()) {
        "add_number" -> AttributeModifier.Operation.ADD_NUMBER
        "add_scalar", "add_multiplied_base" -> AttributeModifier.Operation.ADD_SCALAR
        "multiply_scalar_1", "add_multiplied_total" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1
        else -> null
    }


    fun equippableSlot(input: String): EquipmentSlot? = when (input.lowercase()) {
        "head" -> EquipmentSlot.HEAD
        "chest" -> EquipmentSlot.CHEST
        "legs" -> EquipmentSlot.LEGS
        "feet" -> EquipmentSlot.FEET
        "mainhand" -> EquipmentSlot.HAND
        "offhand" -> EquipmentSlot.OFF_HAND
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
            "id" -> EditAction.SetItemId(material(args.getOrNull(1) ?: return null) ?: return null)
            "enchant" -> parseEnchant(args)
            "tooltip" -> EditAction.TooltipToggle(args.getOrNull(1) ?: return null, args.getOrNull(2)?.lowercase() == "hide")
            "can_place_on" -> parseNamespacedSet(args, true)
            "can_break" -> parseNamespacedSet(args, false)
            "potion" -> parsePotion(args)
            "head" -> parseHead(args)
            "attribute" -> parseAttribute(args)
            "consumable" -> parseConsumable(args)
            "death_protection" -> parseDeathProtection(args)
            "remainder" -> parseRemainder(args)
            "equippable" -> parseEquippable(args)
            "tool" -> parseTool(args)
            "lock" -> parseLock(args)
            "container" -> parseContainer(args)
            "trim" -> parseTrim(args)
            "pot" -> parsePot(args)
            else -> null
        }
    }

    private fun parseContainer(args: Array<out String>): EditAction? {
        if (args.size != 2) return null
        val index = args.getOrNull(1)?.toIntOrNull() ?: return null
        return EditAction.ContainerSetSlotFromOffhand(index)
    }

    private fun parseTrim(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "set" -> {
                val pattern = EditTrimPotSupport.parseTrimPatternTemplateId(args.getOrNull(2) ?: return null) ?: return null
                val material = EditTrimPotSupport.parseTrimMaterialItemId(args.getOrNull(3) ?: return null) ?: return null
                EditAction.TrimSet(pattern, material)
            }
            "clear" -> EditAction.TrimClear
            else -> null
        }
    }

    private fun parsePot(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "clear" -> EditAction.PotClear
            "set" -> {
                val back = EditTrimPotSupport.parsePotDecorationMaterial(args.getOrNull(2) ?: return null) ?: return null
                val left = EditTrimPotSupport.parsePotDecorationMaterial(args.getOrNull(3) ?: return null) ?: return null
                val right = EditTrimPotSupport.parsePotDecorationMaterial(args.getOrNull(4) ?: return null) ?: return null
                val front = EditTrimPotSupport.parsePotDecorationMaterial(args.getOrNull(5) ?: return null) ?: return null
                EditAction.PotSet(back, left, right, front)
            }
            "side" -> {
                val side = EditTrimPotSupport.parsePotSide(args.getOrNull(2) ?: return null) ?: return null
                val material = EditTrimPotSupport.parsePotDecorationMaterial(args.getOrNull(3) ?: return null) ?: return null
                EditAction.PotSetSide(side, material)
            }
            else -> null
        }
    }

    private fun parseLock(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "set" -> EditAction.LockSetFromOffhand
            "clear" -> EditAction.LockClear
            else -> null
        }
    }

    private fun parseTool(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "speed" -> EditAction.ToolSetDefaultMiningSpeed(
                args.getOrNull(2)?.toFloatOrNull() ?: return null,
                parseToolSpeedScope(args.getOrNull(3)) ?: return null
            )
            "damage_per_block" -> EditAction.ToolSetDamagePerBlock(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "clear" -> EditAction.ToolClear
            else -> null
        }
    }

    private fun parseToolSpeedScope(raw: String?): ToolSpeedScope? {
        return when (raw?.lowercase()) {
            "all", "all_blocks", "both" -> ToolSpeedScope.ALL_BLOCKS
            "effective", "effective_only" -> ToolSpeedScope.EFFECTIVE_ONLY
            "ineffective", "ineffective_only" -> ToolSpeedScope.INEFFECTIVE_ONLY
            else -> null
        }
    }


    private fun parseEquippable(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "slot" -> EditAction.EquippableSetSlot(equippableSlot(args.getOrNull(2) ?: return null) ?: return null)
            "clear" -> EditAction.EquippableClear
            "dispensable" -> EditAction.EquippableSetDispensable(parseToggle(args.getOrNull(2)) ?: return null)
            "swappable" -> EditAction.EquippableSetSwappable(parseToggle(args.getOrNull(2)) ?: return null)
            "damage_on_hurt" -> EditAction.EquippableSetDamageOnHurt(parseToggle(args.getOrNull(2)) ?: return null)
            "equip_sound" -> {
                val value = args.getOrNull(2) ?: return null
                if (value.equals("default", true)) {
                    EditAction.EquippableSetEquipSound(null)
                } else {
                    EditAction.EquippableSetEquipSound(parseAdventureKey(value) ?: return null)
                }
            }
            "camera_overlay" -> {
                val value = args.getOrNull(2) ?: return null
                if (value.equals("clear", true)) {
                    EditAction.EquippableSetCameraOverlay(null)
                } else {
                    EditAction.EquippableSetCameraOverlay(parseAdventureKey(value) ?: return null)
                }
            }
            "asset_id" -> {
                val value = args.getOrNull(2) ?: return null
                if (value.equals("clear", true)) {
                    EditAction.EquippableSetAssetId(null)
                } else {
                    EditAction.EquippableSetAssetId(parseAdventureKey(value) ?: return null)
                }
            }
            else -> null
        }
    }

    private fun parseRemainder(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "set" -> EditAction.RemainderSetFromOffhand
            "clear" -> EditAction.RemainderClear
            else -> null
        }
    }

    private fun parseConsumable(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "toggle" -> EditAction.ConsumableToggle(parseToggle(args.getOrNull(2)) ?: return null)
            "animation" -> EditAction.ConsumableAnimation(consumableAnimation(args.getOrNull(2) ?: return null) ?: return null)
            "particles" -> EditAction.ConsumableHasParticles(parseToggle(args.getOrNull(2)) ?: return null)
            "seconds" -> EditAction.ConsumableConsumeSeconds(args.getOrNull(2)?.toFloatOrNull() ?: return null)
            "sound" -> parseConsumableSound(args.getOrNull(2) ?: return null)
            "effect_add" -> EditAction.ConsumableEffectAdd(EditEffectActionsSupport.parseEffectSpec(this, args.drop(2)) ?: return null)
            "effect_remove" -> EditAction.ConsumableEffectRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "effect_clear" -> EditAction.ConsumableEffectClear
            "nutrition" -> EditAction.FoodNutrition(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "saturation" -> EditAction.FoodSaturation(args.getOrNull(2)?.toFloatOrNull() ?: return null)
            "can_always_eat" -> EditAction.FoodCanAlwaysEat(parseToggle(args.getOrNull(2)) ?: return null)
            else -> null
        }
    }

    private fun parseToggle(value: String?): Boolean? = when (value?.lowercase()) {
        "on" -> true
        "off" -> false
        else -> null
    }

    private fun consumableAnimation(value: String): ItemUseAnimation? = when (value.lowercase()) {
        "none" -> ItemUseAnimation.NONE
        "eat" -> ItemUseAnimation.EAT
        "drink" -> ItemUseAnimation.DRINK
        "block" -> ItemUseAnimation.BLOCK
        "bow" -> ItemUseAnimation.BOW
        "crossbow" -> ItemUseAnimation.CROSSBOW
        "spear" -> ItemUseAnimation.SPEAR
        "spyglass" -> ItemUseAnimation.SPYGLASS
        "toot_horn" -> ItemUseAnimation.TOOT_HORN
        "brush" -> ItemUseAnimation.BRUSH
        else -> null
    }

    private fun parseConsumableSound(value: String): EditAction? {
        if (value.equals("default", true)) return EditAction.ConsumableSound(null)
        val key = parseAdventureKey(value) ?: return null
        return EditAction.ConsumableSound(key)
    }

    private fun parseDeathProtection(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "toggle" -> EditAction.DeathProtectionToggle(parseToggle(args.getOrNull(2)) ?: return null)
            "effect_add" -> EditAction.DeathProtectionEffectAdd(EditEffectActionsSupport.parseEffectSpec(this, args.drop(2)) ?: return null)
            "effect_remove" -> EditAction.DeathProtectionEffectRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "effect_clear" -> EditAction.DeathProtectionEffectClear
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
            "use_cooldown" -> {
                if (arg.equals("clear", true)) {
                    EditAction.ClearUseCooldown
                } else {
                    val seconds = arg?.toFloatOrNull() ?: return null
                    val cooldownGroup = args.getOrNull(3)?.let { parseCooldownGroup(it) ?: return null }
                    EditAction.SetUseCooldown(seconds, cooldownGroup)
                }
            }
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

    private fun parseHead(args: Array<out String>): EditAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "clear" -> EditAction.HeadClear
            "from_texture" -> EditAction.HeadSetFromTexture(rawTail(args, 2))
            "from_name" -> {
                val name = args.getOrNull(2) ?: return null
                EditAction.HeadSetFromName(name)
            }
            "from_online" -> {
                val name = args.getOrNull(2) ?: return null
                EditAction.HeadSetFromOnline(name)
            }
            else -> null
        }
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
    fun materialSuggestions(prefix: String): List<String> = Registry.MATERIAL.iterator().asSequence()
        .map { it.key.asString() }
        .filter { it.startsWith(prefix, true) || it.removePrefix("minecraft:").startsWith(prefix, true) }
        .sorted()
        .toList()
}
