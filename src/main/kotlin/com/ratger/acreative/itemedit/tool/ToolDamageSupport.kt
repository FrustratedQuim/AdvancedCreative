package com.ratger.acreative.itemedit.tool

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import kotlin.math.roundToInt

object ToolDamageSupport {

    fun hasCustomMaxDamage(item: ItemStack): Boolean =
        (item.itemMeta as? Damageable)?.hasMaxDamage() == true

    fun customMaxDamage(item: ItemStack): Int? {
        val damageable = item.itemMeta as? Damageable ?: return null
        return if (damageable.hasMaxDamage()) damageable.maxDamage else null
    }

    fun effectiveMaxDamage(item: ItemStack): Int? {
        val custom = customMaxDamage(item)
        if (custom != null) return custom
        return item.type.maxDurability.takeIf { it > 0 }?.toInt()
    }

    fun isDamageOrdinary(item: ItemStack): Boolean =
        (item.itemMeta as? Damageable)?.hasDamage() != true

    fun currentDamage(item: ItemStack): Int {
        val damageable = item.itemMeta as? Damageable ?: return 0
        return if (damageable.hasDamage()) damageable.damage else 0
    }

    fun parseDamageLikeValue(raw: String, item: ItemStack): Int? {
        val normalized = raw.trim().lowercase()
        if (normalized == "max") {
            return effectiveMaxDamage(item)
        }
        if (normalized.endsWith("%")) {
            val percent = normalized.removeSuffix("%").toDoubleOrNull() ?: return null
            if (!percent.isFinite() || percent < 0.0) return null
            val max = effectiveMaxDamage(item) ?: return null
            return (max * (percent / 100.0)).roundToInt()
        }
        return normalized.toIntOrNull()
    }
}
