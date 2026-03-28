package com.ratger.acreative.commands.edit

import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ToolSpeedScope
import com.ratger.acreative.itemedit.attributes.SlotGroupSpec
import com.ratger.acreative.itemedit.effects.EffectActionsSupport
import com.ratger.acreative.itemedit.trim.TrimPotSupport
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
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

    fun enchantment(input: String): Enchantment? = enchantmentRegistry().get(NamespacedKey.minecraft(input.lowercase()))
    fun effect(input: String): PotionEffectType? = Registry.MOB_EFFECT.get(NamespacedKey.minecraft(input.lowercase()))
    fun effectFromToken(input: String): PotionEffectType? {
        val key = NamespacedKey.fromString(input.lowercase()) ?: NamespacedKey.minecraft(input.lowercase())
        return Registry.MOB_EFFECT.get(key)
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

    fun slotGroup(input: String): SlotGroupSpec? = SlotGroupSpec.fromToken(input)

    fun rawTail(args: Array<out String>, from: Int): String = if (args.size <= from) "" else args.copyOfRange(from, args.size).joinToString(" ")

    fun parseAction(args: Array<out String>): ItemAction? {
        if (args.isEmpty()) return ItemAction.Show
        return when (args[0].lowercase()) {
            "show" -> ItemAction.Show
            "reset" -> ItemAction.Reset(args.getOrNull(1)?.lowercase() ?: "")
            "name" -> when (args.getOrNull(1)?.lowercase()) {
                "set" -> ItemAction.NameSet(rawTail(args, 2))
                "clear" -> ItemAction.NameClear
                else -> null
            }

            "lore" -> when (args.getOrNull(1)?.lowercase()) {
                "add" -> ItemAction.LoreAdd(rawTail(args, 2))
                "set" -> ItemAction.LoreSet(args.getOrNull(2)?.toIntOrNull() ?: return null, rawTail(args, 3))
                "remove" -> ItemAction.LoreRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
                "clear" -> ItemAction.LoreClear
                else -> null
            }

            "component" -> parseComponent(args)
            "id" -> ItemAction.SetItemId(material(args.getOrNull(1) ?: return null) ?: return null)
            "enchant" -> parseEnchant(args)
            "tooltip" -> ItemAction.TooltipToggle(args.getOrNull(1) ?: return null, args.getOrNull(2)?.lowercase() == "hide")
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
            "frame" -> parseFrame(args)
            else -> null
        }
    }

    private fun parseFrame(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "invisible" -> {
                val toggle = parseToggle(args.getOrNull(2)) ?: return null
                ItemAction.FrameSetInvisibility(toggle)
            }
            else -> null
        }
    }

    private fun parseContainer(args: Array<out String>): ItemAction? {
        if (args.size != 2) return null
        val index = args.getOrNull(1)?.toIntOrNull() ?: return null
        return ItemAction.ContainerSetSlotFromOffhand(index)
    }

    private fun parseTrim(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "set" -> {
                val pattern = TrimPotSupport.parseTrimPatternTemplateId(args.getOrNull(2) ?: return null) ?: return null
                val material = TrimPotSupport.parseTrimMaterialItemId(args.getOrNull(3) ?: return null) ?: return null
                ItemAction.TrimSet(pattern, material)
            }
            "clear" -> ItemAction.TrimClear
            else -> null
        }
    }

    private fun parsePot(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "clear" -> ItemAction.PotClear
            "set" -> {
                val back = TrimPotSupport.parsePotDecorationMaterial(args.getOrNull(2) ?: return null) ?: return null
                val left = TrimPotSupport.parsePotDecorationMaterial(args.getOrNull(3) ?: return null) ?: return null
                val right = TrimPotSupport.parsePotDecorationMaterial(args.getOrNull(4) ?: return null) ?: return null
                val front = TrimPotSupport.parsePotDecorationMaterial(args.getOrNull(5) ?: return null) ?: return null
                ItemAction.PotSet(back, left, right, front)
            }
            "side" -> {
                val side = TrimPotSupport.parsePotSide(args.getOrNull(2) ?: return null) ?: return null
                val material = TrimPotSupport.parsePotDecorationMaterial(args.getOrNull(3) ?: return null) ?: return null
                ItemAction.PotSetSide(side, material)
            }
            else -> null
        }
    }

    private fun parseLock(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "set" -> ItemAction.LockSetFromOffhand
            "clear" -> ItemAction.LockClear
            else -> null
        }
    }

    private fun parseTool(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "speed" -> ItemAction.ToolSetDefaultMiningSpeed(
                args.getOrNull(2)?.toFloatOrNull() ?: return null,
                parseToolSpeedScope(args.getOrNull(3)) ?: return null
            )
            "damage_per_block" -> ItemAction.ToolSetDamagePerBlock(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "clear" -> ItemAction.ToolClear
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


    private fun parseEquippable(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "slot" -> ItemAction.EquippableSetSlot(equippableSlot(args.getOrNull(2) ?: return null) ?: return null)
            "clear" -> ItemAction.EquippableClear
            "dispensable" -> ItemAction.EquippableSetDispensable(parseToggle(args.getOrNull(2)) ?: return null)
            "swappable" -> ItemAction.EquippableSetSwappable(parseToggle(args.getOrNull(2)) ?: return null)
            "damage_on_hurt" -> ItemAction.EquippableSetDamageOnHurt(parseToggle(args.getOrNull(2)) ?: return null)
            "equip_sound" -> {
                val value = args.getOrNull(2) ?: return null
                if (value.equals("default", true)) {
                    ItemAction.EquippableSetEquipSound(null)
                } else {
                    ItemAction.EquippableSetEquipSound(parseAdventureKey(value) ?: return null)
                }
            }
            "camera_overlay" -> {
                val value = args.getOrNull(2) ?: return null
                if (value.equals("clear", true)) {
                    ItemAction.EquippableSetCameraOverlay(null)
                } else {
                    ItemAction.EquippableSetCameraOverlay(parseAdventureKey(value) ?: return null)
                }
            }
            "asset_id" -> {
                val value = args.getOrNull(2) ?: return null
                if (value.equals("clear", true)) {
                    ItemAction.EquippableSetAssetId(null)
                } else {
                    ItemAction.EquippableSetAssetId(parseAdventureKey(value) ?: return null)
                }
            }
            else -> null
        }
    }

    private fun parseRemainder(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "set" -> ItemAction.RemainderSetFromOffhand
            "clear" -> ItemAction.RemainderClear
            else -> null
        }
    }

    private fun parseConsumable(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "toggle" -> ItemAction.ConsumableToggle(parseToggle(args.getOrNull(2)) ?: return null)
            "animation" -> ItemAction.ConsumableAnimation(consumableAnimation(args.getOrNull(2) ?: return null) ?: return null)
            "particles" -> ItemAction.ConsumableHasParticles(parseToggle(args.getOrNull(2)) ?: return null)
            "seconds" -> ItemAction.ConsumableConsumeSeconds(args.getOrNull(2)?.toFloatOrNull() ?: return null)
            "sound" -> parseConsumableSound(args.getOrNull(2) ?: return null)
            "effect_add" -> ItemAction.ConsumableEffectAdd(EffectActionsSupport.parseEffectSpec(this, args.drop(2)) ?: return null)
            "effect_remove" -> ItemAction.ConsumableEffectRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "effect_clear" -> ItemAction.ConsumableEffectClear
            "nutrition" -> ItemAction.FoodNutrition(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "saturation" -> ItemAction.FoodSaturation(args.getOrNull(2)?.toFloatOrNull() ?: return null)
            "can_always_eat" -> ItemAction.FoodCanAlwaysEat(parseToggle(args.getOrNull(2)) ?: return null)
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

    private fun parseConsumableSound(value: String): ItemAction? {
        if (value.equals("default", true)) return ItemAction.ConsumableSound(null)
        val key = parseAdventureKey(value) ?: return null
        return ItemAction.ConsumableSound(key)
    }

    private fun parseDeathProtection(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "toggle" -> ItemAction.DeathProtectionToggle(parseToggle(args.getOrNull(2)) ?: return null)
            "effect_add" -> ItemAction.DeathProtectionEffectAdd(EffectActionsSupport.parseEffectSpec(this, args.drop(2)) ?: return null)
            "effect_remove" -> ItemAction.DeathProtectionEffectRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "effect_clear" -> ItemAction.DeathProtectionEffectClear
            else -> null
        }
    }

    private fun parseComponent(args: Array<out String>): ItemAction? {
        val node = args.getOrNull(1)?.lowercase() ?: return null
        val arg = args.getOrNull(2)
        return when (node) {
            "item_model" -> ItemAction.SetItemModel(arg?.let { namespacedKey(it) })
            "unbreakable" -> ItemAction.SetUnbreakable(arg == "on")
            "glider" -> ItemAction.SetGlider(arg == "on")
            "max_damage" -> ItemAction.SetMaxDamage(arg?.toIntOrNull())
            "damage" -> ItemAction.SetDamage(arg?.toIntOrNull() ?: return null)
            "max_stack_size" -> ItemAction.SetMaxStackSize(arg?.toIntOrNull())
            "rarity" -> ItemAction.SetRarity(arg?.let { rarity(it) })
            "tooltip_style" -> ItemAction.SetTooltipStyle(
                when (arg?.lowercase()) {
                    "basic" -> null
                    "broken" -> NamespacedKey.minecraft("null")
                    else -> return null
                }
            )
            "use_cooldown" -> {
                if (arg.equals("clear", true)) {
                    ItemAction.ClearUseCooldown
                } else {
                    val seconds = arg?.toFloatOrNull() ?: return null
                    val cooldownGroup = args.getOrNull(3)?.let { parseCooldownGroup(it) ?: return null }
                    ItemAction.SetUseCooldown(seconds, cooldownGroup)
                }
            }
            else -> null
        }
    }

    private fun parseEnchant(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "add" -> ItemAction.EnchantAdd(enchantment(args.getOrNull(2) ?: return null) ?: return null, args.getOrNull(3)?.toIntOrNull() ?: return null)
            "remove" -> ItemAction.EnchantRemove(enchantment(args.getOrNull(2) ?: return null) ?: return null)
            "clear" -> ItemAction.EnchantClear
            "glint" -> ItemAction.SetEnchantmentGlint(
                when (args.getOrNull(2)?.lowercase()) {
                    "on" -> true
                    "off" -> false
                    "default" -> null
                    else -> return null
                }
            )

            "tooltip" -> ItemAction.TooltipToggle("enchantments", args.getOrNull(2)?.lowercase() == "hide")
            else -> null
        }
    }

    private fun parseNamespacedSet(args: Array<out String>, place: Boolean): ItemAction? {
        val keys = args.drop(1).mapNotNull { namespacedKey(it) }.toSet()
        return if (place) ItemAction.SetCanPlaceOn(keys) else ItemAction.SetCanBreak(keys)
    }

    private fun parsePotion(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "color" -> ItemAction.PotionColor(args.getOrNull(2)?.takeUnless { it == "clear" }?.let { color(it) })
            "effect_add" -> ItemAction.PotionEffectAdd(
                effect(args.getOrNull(2) ?: return null) ?: return null,
                args.getOrNull(3)?.toIntOrNull() ?: return null,
                args.getOrNull(4)?.toIntOrNull() ?: return null,
                args.getOrNull(5)?.toBooleanStrictOrNull() ?: true,
                args.getOrNull(6)?.toBooleanStrictOrNull() ?: true,
                args.getOrNull(7)?.toBooleanStrictOrNull() ?: true
            )

            "effect_remove" -> ItemAction.PotionEffectRemove(effect(args.getOrNull(2) ?: return null) ?: return null)
            "effect_clear" -> ItemAction.PotionEffectClear
            else -> null
        }
    }

    private fun parseHead(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "clear" -> ItemAction.HeadClear
            "from_texture" -> ItemAction.HeadSetFromTexture(rawTail(args, 2))
            "from_name" -> {
                val name = args.getOrNull(2) ?: return null
                ItemAction.HeadSetFromName(name)
            }
            "from_online" -> {
                val name = args.getOrNull(2) ?: return null
                ItemAction.HeadSetFromOnline(name)
            }
            else -> null
        }
    }

    private fun parseAttribute(args: Array<out String>): ItemAction? {
        return when (args.getOrNull(1)?.lowercase()) {
            "add" -> {
                val attribute = attribute(args.getOrNull(2) ?: return null) ?: return null
                val amount = args.getOrNull(3)?.toDoubleOrNull() ?: return null
                val operation = attributeOperation(args.getOrNull(4) ?: return null) ?: return null
                ItemAction.AttributeAdd(attribute, amount, operation, args.getOrNull(5))
            }

            "remove" -> ItemAction.AttributeRemove(args.getOrNull(2)?.toIntOrNull() ?: return null)
            "clear" -> ItemAction.AttributeClear
            else -> null
        }
    }

    fun enchantSuggestions(prefix: String): List<String> = enchantmentRegistry().iterator().asSequence().map { it.key.key }.filter { it.startsWith(prefix, true) }.sorted().toList()
    fun effectSuggestions(prefix: String): List<String> = Registry.MOB_EFFECT.iterator().asSequence().map { it.key.key }.filter { it.startsWith(prefix, true) }.sorted().toList()
    fun attributeSuggestions(prefix: String): List<String> = Registry.ATTRIBUTE.iterator().asSequence().map { it.key.key }.filter { it.startsWith(prefix, true) }.sorted().toList()
    fun materialSuggestions(prefix: String): List<String> = Registry.MATERIAL.iterator().asSequence()
        .map { it.key.asString() }
        .filter { it.startsWith(prefix, true) || it.removePrefix("minecraft:").startsWith(prefix, true) }
        .sorted()
        .toList()

    private fun enchantmentRegistry() = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
}
