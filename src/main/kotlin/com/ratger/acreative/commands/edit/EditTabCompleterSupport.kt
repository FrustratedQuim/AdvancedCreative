package com.ratger.acreative.commands.edit

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EditTabCompleterSupport(private val parser: EditParsers) {
    private val roots = listOf(
        "show", "reset", "name", "lore", "component", "tooltip", "enchant", "can_place_on", "can_break",
        "consumable", "death_protection", "tool", "equippable", "remainder", "attribute", "potion", "lock", "head"
    )

    fun complete(sender: CommandSender, args: Array<out String>): List<String> {
        val player = sender as? Player
        val item = player?.inventory?.itemInMainHand
        val type = item?.type
        return when (args.size) {
            1 -> rootsByItem(type).filter { it.startsWith(args[0], true) }
            2 -> when (args[0].lowercase()) {
                "reset" -> listOf("all", "plugin")
                "name" -> listOf("set", "clear")
                "lore" -> listOf("add", "set", "remove", "clear")
                "component" -> listOf("item_model", "unbreakable", "glider", "max_damage", "damage", "max_stack_size", "rarity", "tooltip_style")
                "tooltip" -> listOf("enchantments", "attribute_modifiers", "unbreakable", "dyed_color", "can_break", "can_place_on", "trim", "jukebox_playable", "hide_additional_tooltip", "hide_tooltip")
                "enchant" -> listOf("add", "remove", "clear", "glint", "tooltip")
                "potion" -> listOf("color", "effect_add", "effect_remove", "effect_clear")
                "head" -> listOf("texture", "clear")
                "attribute" -> listOf("add", "remove", "clear")
                else -> emptyList()
            }.filter { it.startsWith(args[1], true) }

            3 -> when (args[0].lowercase()) {
                "component" -> when (args[1].lowercase()) {
                    "unbreakable", "glider" -> listOf("on", "off")
                    "rarity" -> listOf("common", "uncommon", "rare", "epic")
                    "tooltip_style" -> listOf("basic", "broken")
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

                "attribute" -> when (args[1].lowercase()) {
                    "add" -> parser.attributeSuggestions(args[2])
                    "remove" -> (item?.itemMeta?.attributeModifiers?.entries()?.indices?.map(Int::toString) ?: emptyList())
                    else -> emptyList()
                }

                else -> emptyList()
            }.filter { it.startsWith(args[2], true) }

            4 -> when {
                args[0].equals("enchant", true) && args[1].equals("add", true) -> listOf("1", "2", "3", "5", "10")
                args[0].equals("attribute", true) && args[1].equals("add", true) -> listOf("1", "2", "5", "10")
                args[0].equals("potion", true) && args[1].equals("effect_add", true) -> listOf("200", "600", "1200")
                else -> emptyList()
            }

            5 -> if (args[0].equals("attribute", true) && args[1].equals("add", true)) {
                listOf("add_number", "add_scalar", "multiply_scalar_1")
            } else if (args[0].equals("potion", true) && args[1].equals("effect_add", true)) {
                listOf("0", "1", "2")
            } else emptyList()

            6 -> if (args[0].equals("attribute", true) && args[1].equals("add", true)) {
                listOf("mainhand", "offhand", "hand", "armor", "feet", "legs", "chest", "head", "body")
            } else emptyList()

            else -> emptyList()
        }
    }

    private fun rootsByItem(material: Material?): List<String> {
        if (material == null || material == Material.AIR) return listOf("show")
        return roots.filterNot { root ->
            (root == "potion" && !(material.name.endsWith("POTION") || material == Material.TIPPED_ARROW)) ||
                (root == "head" && material != Material.PLAYER_HEAD) ||
                (root == "lock" && !material.name.endsWith("SHULKER_BOX")) ||
                (root == "attribute" && !(material.name.endsWith("_HELMET") || material.name.endsWith("_CHESTPLATE") || material.name.endsWith("_LEGGINGS") || material.name.endsWith("_BOOTS")))
        }
    }
}
