package com.ratger.acreative.commands.edit

import com.ratger.acreative.menus.edit.attributes.SlotGroupSpec
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffectType

class EditParsers {

    fun color(input: String): Int? {
        val value = input.trim()
        return when {
            value.startsWith("#") && value.length == 7 -> value.substring(1).toIntOrNull(16)
            else -> value.toIntOrNull()?.takeIf { it in 0..0xFFFFFF }
        }
    }

    fun effect(input: String): PotionEffectType? = Registry.MOB_EFFECT.get(NamespacedKey.minecraft(input.lowercase()))
    fun effectFromToken(input: String): PotionEffectType? {
        val key = NamespacedKey.fromString(input.lowercase()) ?: NamespacedKey.minecraft(input.lowercase())
        return Registry.MOB_EFFECT.get(key)
    }

    fun parseSoundNamespacedKey(input: String): NamespacedKey? {
        val normalized = input.trim().lowercase()
        if (normalized.isEmpty()) return null
        return NamespacedKey.fromString(normalized) ?: NamespacedKey.fromString("minecraft:$normalized")
    }
    fun parseSoundKey(input: String): Key? = parseSoundNamespacedKey(input)?.let { Key.key(it.asString()) }
    fun parseBooleanStrict(input: String?): Boolean? = input?.lowercase()?.let {
        when (it) {
            "true" -> true
            "false" -> false
            else -> null
        }
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

    fun itemMaterial(input: String): Material? = material(input)?.takeIf { it.isItem && it != Material.AIR }

    fun blockItemMaterial(input: String): Material? = material(input)?.takeIf { it.isBlock && it.isItem }

    fun slotGroup(input: String): SlotGroupSpec? = SlotGroupSpec.fromToken(input)

    fun materialSuggestions(prefix: String): List<String> {
        val normalizedPrefix = prefix.removePrefix("minecraft:")
        return Registry.MATERIAL.iterator().asSequence()
            .filter { it.isItem && it != Material.AIR }
            .map { it.key.key }
            .filter { it.startsWith(normalizedPrefix, true) }
            .sorted()
            .toList()
    }

    fun blockItemSuggestions(prefix: String): List<String> {
        val normalizedPrefix = prefix.removePrefix("minecraft:")
        return Registry.MATERIAL.iterator().asSequence()
            .filter { it.isBlock && it.isItem }
            .map { it.key.key }
            .filter { it.startsWith(normalizedPrefix, true) }
            .sorted()
            .toList()
    }

    fun soundSuggestions(prefix: String): List<String> {
        val normalizedPrefix = prefix.removePrefix("minecraft:")
        return Registry.SOUNDS.iterator().asSequence()
            .mapNotNull { Registry.SOUNDS.getKey(it)?.asString() }
            .map { it.removePrefix("minecraft:") }
            .distinct()
            .filter { it.startsWith(normalizedPrefix, ignoreCase = true) }
            .sorted()
            .toList()
    }

}
