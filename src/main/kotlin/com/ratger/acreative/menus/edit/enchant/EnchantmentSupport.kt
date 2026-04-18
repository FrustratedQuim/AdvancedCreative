package com.ratger.acreative.menus.edit.enchant

import com.ratger.acreative.menus.edit.text.VanillaRuLocalization
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta

object EnchantmentSupport {
    data class EnchantmentEntry(
        val enchantment: Enchantment,
        val level: Int,
        val displayName: String,
        val keyPath: String
    )

    private val enchantmentRegistry by lazy { RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT) }
    private val preferredOrder = listOf(
        "unbreaking", "mending",
        "efficiency", "fortune", "silk_touch",
        "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge",
        "power", "punch", "flame", "infinity",
        "protection", "fire_protection", "blast_protection", "projectile_protection",
        "feather_falling", "respiration", "aqua_affinity", "thorns", "depth_strider", "frost_walker", "soul_speed", "swift_sneak",
        "impaling", "riptide", "loyalty", "channeling",
        "lure", "luck_of_the_sea",
        "quick_charge", "multishot", "piercing",
        "breach", "density", "wind_burst",
        "binding_curse", "vanishing_curse"
    )
    private val preferredOrderIndex = preferredOrder.withIndex().associate { it.value to it.index }


    fun entries(meta: ItemMeta?): List<EnchantmentEntry> {
        val source = enchantmentLevels(meta)
        return source.entries
            .sortedBy { keyPath(it.key) }
            .map { entry ->
                val path = keyPath(entry.key)
                EnchantmentEntry(
                    enchantment = entry.key,
                    level = entry.value,
                    displayName = displayName(entry.key),
                    keyPath = path
                )
            }
    }

    fun hasAny(meta: ItemMeta?): Boolean = enchantmentLevels(meta).isNotEmpty()

    fun contains(meta: ItemMeta?, enchantment: Enchantment): Boolean = enchantmentLevels(meta).containsKey(enchantment)

    fun add(meta: ItemMeta, enchantment: Enchantment, level: Int, ignoreLevelRestriction: Boolean = true) {
        val storageMeta = meta as? EnchantmentStorageMeta
        if (storageMeta != null) {
            storageMeta.addStoredEnchant(enchantment, level, ignoreLevelRestriction)
            return
        }
        meta.addEnchant(enchantment, level, ignoreLevelRestriction)
    }

    fun remove(meta: ItemMeta, enchantment: Enchantment): Boolean {
        val storageMeta = meta as? EnchantmentStorageMeta
        if (storageMeta != null) {
            val had = storageMeta.hasStoredEnchant(enchantment)
            storageMeta.removeStoredEnchant(enchantment)
            return had
        }
        val had = meta.hasEnchant(enchantment)
        meta.removeEnchant(enchantment)
        return had
    }

    fun clear(meta: ItemMeta) {
        val storageMeta = meta as? EnchantmentStorageMeta
        if (storageMeta != null) {
            storageMeta.storedEnchants.keys.toList().forEach(storageMeta::removeStoredEnchant)
            return
        }
        meta.removeEnchantments()
    }

    fun resolve(input: String): Enchantment? {
        val normalized = input.trim().lowercase()
        val direct = NamespacedKey.fromString(normalized)
        if (direct != null) {
            return enchantmentRegistry.get(direct)
        }
        return enchantmentRegistry.get(NamespacedKey.minecraft(normalized))
    }

    fun keyPath(enchantment: Enchantment): String {
        val fullKey = enchantmentRegistry.getKey(enchantment)?.asString()
        if (fullKey == null) return "unknown"
        return fullKey.removePrefix("minecraft:")
    }

    fun displayName(enchantment: Enchantment): String {
        val path = keyPath(enchantment)
        return VanillaRuLocalization.enchantmentName(path)
    }


    fun orderedEnchantments(): List<Enchantment> {
        return enchantmentRegistry.iterator().asSequence()
            .sortedWith(compareBy<Enchantment>(
                { preferredOrderIndex[keyPath(it)] ?: Int.MAX_VALUE },
                ::keyPath
            ))
            .toList()
    }

    fun suggestions(prefix: String): List<String> {
        return enchantmentRegistry.iterator().asSequence()
            .map(::keyPath)
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .sorted()
            .toList()
    }

    fun levelDisplay(level: Int, showOne: Boolean): String {
        if (level <= 1 && !showOne) return ""
        return " <#FFF3E0>$level"
    }

    private fun enchantmentLevels(meta: ItemMeta?): Map<Enchantment, Int> {
        if (meta == null) return emptyMap()
        val storageMeta = meta as? EnchantmentStorageMeta
        if (storageMeta != null) {
            return storageMeta.storedEnchants
        }
        return meta.enchants
    }

}
