package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.configuration.ConfigurationSection

class BannerPublicationConfigResolver(
    private val hooker: FunctionHooker
) {
    fun readConfig(): BannerPublicationConfig {
        val root = hooker.configManager.config.getConfigurationSection("banner.post")
        val defaultLimit = root?.getInt("default-limit", 18) ?: 18

        val limits = linkedMapOf<String, Int>()
        root?.getConfigurationSection("limits")?.let { flattenLimits(it, limits) }

        return BannerPublicationConfig(
            defaultLimit = defaultLimit,
            limitsByPermission = limits
        )
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
