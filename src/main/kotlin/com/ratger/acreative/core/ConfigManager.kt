package com.ratger.acreative.core

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import java.io.File
import java.io.InputStreamReader

class ConfigManager(private val hooker: FunctionHooker) {

    private val pluginFolder = hooker.plugin.dataFolder
    private val configFile = File(pluginFolder, "config.yml")
    private val messagesFile = File(pluginFolder, "messages.yml")
    private val stringToNumericIds = HashMap<String, String>()

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

        initStringToNumericIds()

        config.save(configFile)
        messages.save(messagesFile)
    }

    fun getBlockedDisguises(): Set<EntityType> {
        return config.getStringList("blocked-disguises")
            .mapNotNull { runCatching { EntityType.valueOf(it) }.getOrNull() }
            .toSet()
    }

    fun getNumericId(materialName: String): String {
        println("[ConfigManager] Получаем материал $materialName, цифровое ID: ${stringToNumericIds[materialName]}")
        return stringToNumericIds[materialName] ?: "none"
    }

    private fun initStringToNumericIds() {
        config.getConfigurationSection("string-to-numeric-ids")?.getKeys(false)?.forEach { key ->
            stringToNumericIds[key] = config.getString("string-to-numeric-ids.$key") ?: "none"
        }
    }

    private fun fixEmptyValues(yaml: YamlConfiguration, resourceName: String) {
        hooker.plugin.getResource(resourceName)?.use { inputStream ->
            val defaultYaml = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
            for (key in defaultYaml.getKeys(true)) {
                if (!yaml.contains(key) || yaml.get(key) == null) {
                    yaml.set(key, defaultYaml.get(key))
                }
            }
        }
    }
}