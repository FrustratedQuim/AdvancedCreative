package com.ratger.acreative.itemedit.enchant

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

    private val vanillaDisplayNames: Map<String, String> = mapOf(
        "aqua_affinity" to "Подводник",
        "bane_of_arthropods" to "Бич членистоногих",
        "blast_protection" to "Взрывоустойчивость",
        "breach" to "Пробитие",
        "channeling" to "Громовержец",
        "density" to "Плотность",
        "depth_strider" to "Подводная ходьба",
        "efficiency" to "Эффективность",
        "feather_falling" to "Невесомость",
        "fire_aspect" to "Заговор огня",
        "fire_protection" to "Огнеупорность",
        "flame" to "Горящая стрела",
        "fortune" to "Удача",
        "frost_walker" to "Ледоход",
        "impaling" to "Пронзатель",
        "infinity" to "Бесконечность",
        "knockback" to "Отдача",
        "looting" to "Добыча",
        "loyalty" to "Верность",
        "luck_of_the_sea" to "Везучий рыбак",
        "lure" to "Приманка",
        "mending" to "Починка",
        "multishot" to "Тройной выстрел",
        "piercing" to "Пробивание",
        "power" to "Сила",
        "projectile_protection" to "Защита от снарядов",
        "protection" to "Защита",
        "punch" to "Отбрасывание",
        "quick_charge" to "Быстрая перезарядка",
        "respiration" to "Подводное дыхание",
        "riptide" to "Тягун",
        "sharpness" to "Острота",
        "silk_touch" to "Шёлковое касание",
        "smite" to "Небесная кара",
        "soul_speed" to "Скорость души",
        "sweeping" to "Разящий клинок",
        "swift_sneak" to "Проворство",
        "thorns" to "Шипы",
        "unbreaking" to "Прочность",
        "vanishing_curse" to "Проклятие утраты",
        "wind_burst" to "Порыв ветра",
        "binding_curse" to "Проклятие несъёмности"
    )

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

    fun count(meta: ItemMeta?): Int = enchantmentLevels(meta).size

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
        return vanillaDisplayNames[path] ?: humanizePath(path)
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

    private fun humanizePath(path: String): String {
        return path.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
            }
    }
}
