package com.ratger.acreative.core

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import java.io.File
import java.io.InputStreamReader

class ConfigManager(private val hooker: FunctionHooker) {

    private val pluginFolder = hooker.plugin.dataFolder
    private val configFile = File(pluginFolder, "config.yml")
    private val messagesFile = File(pluginFolder, "messages.yml")

    lateinit var config: YamlConfiguration
        private set
    lateinit var messages: YamlConfiguration
        private set

    fun initConfigs() {
        if (!pluginFolder.exists()) pluginFolder.mkdirs()

        if (!configFile.exists()) hooker.plugin.saveResource("config.yml", false)
        if (!messagesFile.exists()) hooker.plugin.saveResource("messages.yml", false)

        config = YamlConfiguration.loadConfiguration(configFile)
        messages = YamlConfiguration.loadConfiguration(messagesFile)

        fixEmptyValues(config, "config.yml")
        fixEmptyValues(messages, "messages.yml")

        config.save(configFile)
        messages.save(messagesFile)
    }

    fun getBlockedDisguises(): Set<EntityType> {
        return config.getStringList("blocked-disguises")
            .mapNotNull { runCatching { EntityType.valueOf(it) }.getOrNull() }
            .toSet()
    }

    private fun fixEmptyValues(yaml: YamlConfiguration, resourceName: String) {
        hooker.plugin.getResource(resourceName)?.use { inputStream ->
            val defaultYaml = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
            for (key in defaultYaml.getKeys(false)) {
                checkAndSet(yaml, defaultYaml, key)
            }
        }
    }

    private fun checkAndSet(yaml: YamlConfiguration, defaultYaml: YamlConfiguration, key: String) {
        if (!yaml.contains(key) || yaml.get(key) == null) {
            yaml.set(key, defaultYaml.get(key))
        } else if (defaultYaml.isConfigurationSection(key)) {
            val section = defaultYaml.getConfigurationSection(key) ?: return
            val keys = section.getKeys(false)
            for (subKey in keys) {
                checkAndSet(yaml, section, subKey, "$key.$subKey")
            }
        }
    }

    private fun checkAndSet(
        yaml: YamlConfiguration,
        section: org.bukkit.configuration.ConfigurationSection,
        key: String,
        fullKey: String
    ) {
        if (!yaml.contains(fullKey) || yaml.get(fullKey) == null) {
            yaml.set(fullKey, section.get(key))
        } else if (section.isConfigurationSection(key)) {
            val subSection = section.getConfigurationSection(key) ?: return
            val keys = subSection.getKeys(false)
            for (subKey in keys) {
                checkAndSet(yaml, subSection, subKey, "$fullKey.$subKey")
            }
        }
    }
}
