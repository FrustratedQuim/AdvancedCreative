package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.menus.banner.service.BannerPermissionLimitResolver
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import kotlin.math.ceil
import kotlin.math.max

class BannerStorageConfigResolver(
    private val hooker: FunctionHooker,
    private val permissionLimitResolver: BannerPermissionLimitResolver = BannerPermissionLimitResolver()
) {
    fun readConfig(): BannerStorageConfig {
        val config = hooker.configManager.config
        val root = config.getConfigurationSection("banner.storage")

        val pageSize = root?.getInt("page-size", 45)?.coerceAtLeast(9) ?: 45
        val minPages = root?.getInt("min-pages", 10)?.coerceAtLeast(1) ?: 10
        val defaultLimit = root?.getInt("default-limit", 45) ?: 45

        val limitsSection = root?.getConfigurationSection("limits")
        val limits = linkedMapOf<String, Int>()
        limitsSection?.let { flattenLimits(it, limits) }

        return BannerStorageConfig(
            defaultLimit = defaultLimit,
            minPages = minPages,
            pageSize = pageSize,
            limitsByPermission = limits
        )
    }

    fun resolveLimit(player: Player, config: BannerStorageConfig = readConfig()): Int {
        return permissionLimitResolver.resolveLimit(player, config.defaultLimit, config.limitsByPermission)
    }

    fun computeTotalPages(
        limit: Int,
        maxOccupiedSlotIndex: Int,
        config: BannerStorageConfig = readConfig()
    ): Int {
        return if (limit < 0) {
            max(config.minPages, ceil((maxOccupiedSlotIndex + 2) / config.pageSize.toDouble()).toInt())
        } else {
            max(config.minPages, ceil(limit / config.pageSize.toDouble()).toInt())
        }
    }

    private fun flattenLimits(section: ConfigurationSection, target: MutableMap<String, Int>, prefix: String = "") {
        section.getKeys(false).forEach { key ->
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
            val nested = section.getConfigurationSection(key)
            if (nested != null) {
                flattenLimits(nested, target, fullKey)
                return@forEach
            }

            val raw = section.get(key) ?: return@forEach
            val numeric = when (raw) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            } ?: return@forEach

            target[fullKey] = numeric
        }
    }
}
