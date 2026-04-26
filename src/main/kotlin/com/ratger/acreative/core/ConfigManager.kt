package com.ratger.acreative.core

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import java.io.File
import java.io.InputStreamReader

class ConfigManager(private val hooker: FunctionHooker) {

    private val pluginFolder = hooker.plugin.dataFolder
    private val configFile = File(pluginFolder, CONFIG_RESOURCE_NAME)
    private val stringToNumericIds = HashMap<String, String>()

    lateinit var config: YamlConfiguration
        private set

    fun initConfigs() {
        if (!pluginFolder.exists()) pluginFolder.mkdirs()

        if (!configFile.exists()) hooker.plugin.saveResource(CONFIG_RESOURCE_NAME, false)

        config = YamlConfiguration.loadConfiguration(configFile)

        fixEmptyValues(config)

        initStringToNumericIds()

        config.save(configFile)
    }

    fun getBlockedDisguises(): Set<EntityType> {
        return config.getStringList("blocked-disguises")
            .mapNotNull { runCatching { EntityType.valueOf(it) }.getOrNull() }
            .toSet()
    }

    fun getNumericId(materialName: String): String {
        return stringToNumericIds[materialName] ?: "none"
    }

    private fun initStringToNumericIds() {
        config.getConfigurationSection("string-to-numeric-ids")?.getKeys(false)?.forEach { key ->
            stringToNumericIds[key] = config.getString("string-to-numeric-ids.$key") ?: "none"
        }
    }

    private fun fixEmptyValues(yaml: YamlConfiguration) {
        hooker.plugin.getResource(CONFIG_RESOURCE_NAME)?.use { inputStream ->
            val defaultYaml = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
            mergeMissingValues(target = yaml, defaults = defaultYaml)
        }
    }

    private fun mergeMissingValues(target: ConfigurationSection, defaults: ConfigurationSection) {
        defaults.getKeys(false).forEach { key ->
            val defaultValue = defaults.get(key)
            val defaultSection = defaults.getConfigurationSection(key)
            val targetSection = target.getConfigurationSection(key)

            when {
                defaultSection != null -> {
                    val nextTarget = targetSection ?: target.createSection(key)
                    mergeMissingValues(nextTarget, defaultSection)
                }
                !target.contains(key) || target.get(key) == null -> {
                    target.set(key, defaultValue)
                }
            }
        }
    }

    companion object {
        private const val CONFIG_RESOURCE_NAME = "config.yml"
    }
}
