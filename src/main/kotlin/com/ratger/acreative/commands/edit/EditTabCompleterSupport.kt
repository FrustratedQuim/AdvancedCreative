package com.ratger.acreative.commands.edit

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EditTabCompleterSupport(private val parser: EditParsers) {
    private val roots = listOf(
        "show", "reset", "name", "lore", "component", "tooltip", "enchant", "can_place_on", "can_break",
        "consumable", "death_protection", "tool", "equippable", "remainder", "attribute", "potion", "lock", "head", "trim", "pot", "container", "id"
    )

    fun complete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player
        val item = player?.inventory?.itemInMainHand
        val type = item?.type
        return when (args.size) {
            1 -> rootsByItem(type).filter { it.startsWith(args[0], true) }
            2 -> when (args[0].lowercase()) {
                "reset" -> listOf("all")
                "id" -> parser.materialSuggestions(args[1])
                "name" -> listOf("set", "clear")
                "lore" -> listOf("add", "set", "remove", "clear")
                "component" -> listOf("item_model", "unbreakable", "glider", "max_damage", "damage", "max_stack_size", "rarity", "tooltip_style", "use_cooldown")
                "tooltip" -> listOf("enchantments", "attribute_modifiers", "unbreakable", "dyed_color", "can_break", "can_place_on", "trim", "jukebox_playable", "hide_additional_tooltip", "hide_tooltip")
                "enchant" -> listOf("add", "remove", "clear", "glint", "tooltip")
                "potion" -> listOf("color", "effect_add", "effect_remove", "effect_clear")
                "consumable" -> listOf("toggle", "animation", "particles", "seconds", "sound", "nutrition", "saturation", "can_always_eat", "effect_add", "effect_remove", "effect_clear")
                "death_protection" -> listOf("toggle", "effect_add", "effect_remove", "effect_clear")
                "remainder" -> listOf("set", "clear")
                "lock" -> listOf("set", "clear")
                "equippable" -> listOf("slot", "clear", "dispensable", "swappable", "damage_on_hurt", "equip_sound", "camera_overlay", "asset_id")
                "tool" -> listOf("speed", "damage_per_block", "clear")
                "head" -> listOf("clear", "from_texture", "from_name", "from_online")
                "attribute" -> listOf("add", "remove", "clear")
                "trim" -> listOf("set", "clear")
                "pot" -> listOf("clear", "set", "side")
                "container" -> {
                    val capacity = type?.let { EditContainerSupport.containerCapacity(it) }
                    if (capacity == null) emptyList() else (0 until capacity).map(Int::toString)
                }
                else -> emptyList()
            }.filter { it.startsWith(args[1], true) }

            3 -> when (args[0].lowercase()) {
                "component" -> when (args[1].lowercase()) {
                    "unbreakable", "glider" -> listOf("on", "off")
                    "rarity" -> listOf("common", "uncommon", "rare", "epic")
                    "tooltip_style" -> listOf("basic", "broken")
                    "use_cooldown" -> listOf("clear", "0.2", "1.0", "5.0")
                    else -> emptyList()
                }

                "tooltip" -> listOf("show", "hide")
                "enchant" -> when (args[1].lowercase()) {
                    "add", "remove" -> parser.enchantSuggestions(args[2])
                    "glint" -> listOf("on", "off", "default")
                    "tooltip" -> listOf("show", "hide")
                    else -> emptyList()
                }

                "potion" -> when (args[1].lowercase()) {
                    "color" -> listOf("#FF0000", "clear")
                    "effect_add", "effect_remove" -> parser.effectSuggestions(args[2])
                    else -> emptyList()
                }
                "consumable" -> when (args[1].lowercase()) {
                    "toggle", "particles", "can_always_eat" -> listOf("on", "off")
                    "animation" -> listOf("none", "eat", "drink", "block", "bow", "crossbow", "spear", "spyglass", "toot_horn", "brush")
                    "seconds" -> listOf("1.0", "0.8", "0.2")
                    "sound" -> listOf("minecraft:entity.wither.spawn", "default")
                    "effect_add" -> effectKinds()
                    "effect_remove" -> item?.let(EditExperimentalEffectSupport::consumableEffectIndices) ?: emptyList()
                    "nutrition" -> listOf("1", "5", "10")
                    "saturation" -> listOf("0.1", "1.0", "5.0")
                    else -> emptyList()
                }
                "death_protection" -> when (args[1].lowercase()) {
                    "toggle" -> listOf("on", "off")
                    "effect_add" -> effectKinds()
                    "effect_remove" -> item?.let(EditExperimentalEffectSupport::deathProtectionEffectIndices) ?: emptyList()
                    else -> emptyList()
                }
                "equippable" -> when (args[1].lowercase()) {
                    "slot" -> listOf("head", "chest", "legs", "feet", "mainhand", "offhand")
                    "dispensable", "swappable", "damage_on_hurt" -> listOf("on", "off")
                    "equip_sound" -> listOf("minecraft:entity.wither.spawn", "minecraft:item.armor.equip_netherite", "default")
                    "camera_overlay" -> listOf("minecraft:misc/spyglass_scope", "clear")
                    "asset_id" -> listOf("minecraft:netherite", "minecraft:diamond", "clear")
                    else -> emptyList()
                }
                "tool" -> when (args[1].lowercase()) {
                    "speed" -> listOf("1.0", "5.0", "10.0")
                    "damage_per_block" -> listOf("0", "1", "5")
                    else -> emptyList()
                }

                "attribute" -> when (args[1].lowercase()) {
                    "add" -> parser.attributeSuggestions(args[2])
                    "remove" -> (item?.itemMeta?.attributeModifiers?.entries()?.indices?.map(Int::toString) ?: emptyList())
                    else -> emptyList()
                }
                "head" -> when (args[1].lowercase()) {
                    "from_online", "from_name" -> org.bukkit.Bukkit.getOnlinePlayers().map { it.name }
                    else -> emptyList()
                }
                "trim" -> when (args[1].lowercase()) {
                    "set" -> EditTrimPotSupport.trimPatternIds()
                    else -> emptyList()
                }
                "pot" -> when (args[1].lowercase()) {
                    "set" -> EditTrimPotSupport.potDecorationMaterialIds
                    "side" -> EditTrimPotSupport.sideOptions()
                    else -> emptyList()
                }

                else -> emptyList()
            }.filter { it.startsWith(args[2], true) }

            4 -> when {
                args[0].equals("tool", true) && args[1].equals("speed", true) && args[2].toFloatOrNull() != null ->
                    listOf("effective_only", "ineffective_only", "all_blocks")
                args[0].equals("enchant", true) && args[1].equals("add", true) -> listOf("1", "2", "3", "5", "10")
                args[0].equals("attribute", true) && args[1].equals("add", true) -> listOf("1", "2", "5", "10")
                args[0].equals("potion", true) && args[1].equals("effect_add", true) -> listOf("200", "600", "1200")
                args[0].equals("component", true) && args[1].equals("use_cooldown", true) && args[2].toFloatOrNull() != null ->
                    listOf("minecraft:test", "minecraft:ender_pearl", "minecraft:custom_group")
                isEffectAddCommand(args) && args[2].equals("play_sound", true) -> listOf("minecraft:entity.wither.spawn")
                isEffectAddCommand(args) && args[2].equals("remove_effects", true) -> parser.effectSuggestions(args[3])
                isEffectAddCommand(args) && args[2].equals("teleport_randomly", true) -> listOf("5.0", "8.0", "16.0")
                isEffectAddCommand(args) && args[2].equals("apply_effects", true) -> listOf("1.0", "0.5", "0.25")
                args[0].equals("trim", true) && args[1].equals("set", true) -> EditTrimPotSupport.trimMaterialIds()
                args[0].equals("pot", true) && args[1].equals("set", true) -> EditTrimPotSupport.potDecorationMaterialIds
                args[0].equals("pot", true) && args[1].equals("side", true) -> EditTrimPotSupport.potDecorationMaterialIds
                else -> emptyList()
            }

            5 -> if (args[0].equals("attribute", true) && args[1].equals("add", true)) {
                listOf("add_number", "add_scalar", "multiply_scalar_1")
            } else if (args[0].equals("potion", true) && args[1].equals("effect_add", true)) {
                listOf("0", "1", "2")
            } else if (isEffectAddCommand(args) && args[2].equals("apply_effects", true)) {
                parser.effectSuggestions(args[4])
            } else if (args[0].equals("pot", true) && args[1].equals("set", true)) {
                EditTrimPotSupport.potDecorationMaterialIds
            } else emptyList()

            6 -> when {
                args[0].equals("attribute", true) && args[1].equals("add", true) -> listOf("mainhand", "offhand", "hand", "armor", "feet", "legs", "chest", "head", "body")
                isEffectAddCommand(args) && args[2].equals("apply_effects", true) -> listOf("100", "200", "600")
                args[0].equals("pot", true) && args[1].equals("set", true) -> EditTrimPotSupport.potDecorationMaterialIds
                else -> emptyList()
            }

            7 -> if (isEffectAddCommand(args) && args[2].equals("apply_effects", true)) listOf("0", "1", "2") else emptyList()
            8, 9 -> if (isEffectAddCommand(args) && args[2].equals("apply_effects", true)) listOf("true", "false") else emptyList()

            else -> emptyList()
        }
    }

    private fun effectKinds(): List<String> = listOf("clear_all_effects", "play_sound", "remove_effects", "teleport_randomly", "apply_effects")

    private fun isEffectAddCommand(args: Array<out String>): Boolean {
        if (args.size < 3) return false
        val root = args[0].lowercase()
        if (root != "consumable" && root != "death_protection") return false
        return args[1].equals("effect_add", true)
    }

    private fun rootsByItem(material: Material?): List<String> {
        if (material == null || material == Material.AIR) return listOf("show")
        return roots.filterNot { root ->
            (root == "potion" && !(material.name.endsWith("POTION") || material == Material.TIPPED_ARROW)) ||
                (root == "head" && material != Material.PLAYER_HEAD) ||
                (root == "lock" && !material.name.endsWith("SHULKER_BOX")) ||
                (root == "attribute" && !(material.name.endsWith("_HELMET") || material.name.endsWith("_CHESTPLATE") || material.name.endsWith("_LEGGINGS") || material.name.endsWith("_BOOTS"))) ||
                (root == "trim" && !(material.name.endsWith("_HELMET") || material.name.endsWith("_CHESTPLATE") || material.name.endsWith("_LEGGINGS") || material.name.endsWith("_BOOTS"))) ||
                (root == "pot" && material != Material.DECORATED_POT) ||
                (root == "container" && EditContainerSupport.containerCapacity(material) == null)
        }
    }
}
