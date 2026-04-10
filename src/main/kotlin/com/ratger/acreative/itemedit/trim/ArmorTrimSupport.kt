package com.ratger.acreative.itemedit.trim

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern

object ArmorTrimSupport {
    private val defaultPattern = TrimPattern.WAYFINDER
    private val defaultMaterial = TrimMaterial.IRON

    fun supports(item: ItemStack): Boolean {
        val currentMeta = runCatching { item.itemMeta }.getOrNull()
        if (currentMeta is ArmorMeta) return true
        return runCatching { Bukkit.getItemFactory().getItemMeta(item.type) is ArmorMeta }.getOrDefault(false)
    }

    fun hasTrim(item: ItemStack): Boolean {
        val meta = item.itemMeta as? ArmorMeta ?: return false
        return meta.hasTrim()
    }

    fun currentPattern(item: ItemStack): TrimPattern? {
        val meta = item.itemMeta as? ArmorMeta ?: return null
        return if (meta.hasTrim()) meta.trim?.pattern else null
    }

    fun currentMaterial(item: ItemStack): TrimMaterial? {
        val meta = item.itemMeta as? ArmorMeta ?: return null
        return if (meta.hasTrim()) meta.trim?.material else null
    }

    fun set(item: ItemStack, material: TrimMaterial, pattern: TrimPattern): Boolean {
        return applyTrim(item, ArmorTrim(material, pattern))
    }

    fun clear(item: ItemStack): Boolean {
        return applyTrim(item, null)
    }

    fun togglePattern(item: ItemStack, pattern: TrimPattern): Boolean {
        val current = currentPattern(item)
        if (current == pattern) {
            return clear(item)
        }
        val material = currentMaterial(item) ?: defaultMaterial
        return set(item, material, pattern)
    }

    fun toggleMaterial(item: ItemStack, material: TrimMaterial): Boolean {
        val current = currentMaterial(item)
        if (current == material) {
            return clear(item)
        }
        val pattern = currentPattern(item) ?: defaultPattern
        return set(item, material, pattern)
    }

    private fun applyTrim(item: ItemStack, trim: ArmorTrim?): Boolean {
        val meta = item.itemMeta as? ArmorMeta ?: return false
        meta.trim = trim
        item.itemMeta = meta
        return true
    }
}
