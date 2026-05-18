package com.ratger.acreative.core

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8

class ConfigManager(private val hooker: FunctionHooker) {

    private val pluginFolder = hooker.plugin.dataFolder
    private val stringToNumericIds = HashMap<String, String>()
    private val managedFiles = CONFIG_FILES.associateWith { spec ->
        ManagedConfigFile(spec = spec, file = File(pluginFolder, spec.resourcePath))
    }

    lateinit var config: YamlConfiguration
        private set

    fun initConfigs() {
        if (!pluginFolder.exists()) pluginFolder.mkdirs()

        val legacyConfig = ensureAndLoadMainConfig()
        val migratedRootKeys = mutableSetOf<String>()

        managedFiles.values.forEach { managed ->
            val fileMissing = !managed.file.exists()
            ensureConfigFileExists(managed.spec)

            managed.yaml = YamlConfiguration.loadConfiguration(managed.file)
            fixEmptyValues(managed.yaml, managed.spec.resourcePath)

            if (fileMissing) {
                migrateLegacySections(legacyConfig, managed, migratedRootKeys)
            }

            managed.yaml.save(managed.file)
        }

        pruneMigratedKeysFromMainConfig(migratedRootKeys)
        rebuildMergedConfig()

        initStringToNumericIds()
    }

    fun save() {
        managedFiles.values.forEach { managed -> managed.yaml.save(managed.file) }
    }

    fun getNumericId(materialName: String): String {
        return stringToNumericIds[materialName] ?: "none"
    }

    fun set(path: String, value: Any?) {
        resolveOwner(path).yaml.set(path, value)
        config.set(path, value)

        if (path.startsWith("$STRING_TO_NUMERIC_IDS_ROOT.")) {
            val materialName = path.removePrefix("$STRING_TO_NUMERIC_IDS_ROOT.")
            if (value == null) {
                stringToNumericIds.remove(materialName)
            } else {
                stringToNumericIds[materialName] = value.toString()
            }
        }
    }

    private fun initStringToNumericIds() {
        stringToNumericIds.clear()
        config.getConfigurationSection("string-to-numeric-ids")?.getKeys(false)?.forEach { key ->
            stringToNumericIds[key] = config.getString("string-to-numeric-ids.$key") ?: "none"
        }
    }

    private fun ensureAndLoadMainConfig(): YamlConfiguration {
        ensureConfigFileExists(MAIN_CONFIG_SPEC)
        return YamlConfiguration.loadConfiguration(managedFiles.getValue(MAIN_CONFIG_SPEC).file)
    }

    private fun ensureConfigFileExists(spec: ConfigFileSpec) {
        val file = managedFiles.getValue(spec).file
        if (file.exists()) return

        file.parentFile?.mkdirs()
        hooker.plugin.saveResource(spec.resourcePath, false)
    }

    private fun migrateLegacySections(
        legacyConfig: YamlConfiguration,
        managed: ManagedConfigFile,
        migratedRootKeys: MutableSet<String>
    ) {
        if (managed.spec == MAIN_CONFIG_SPEC) return

        managed.spec.rootKeys.forEach { rootKey ->
            if (!legacyConfig.contains(rootKey)) return@forEach

            managed.yaml.set(rootKey, null)
            copyNode(legacyConfig, managed.yaml, rootKey, overwrite = true)
            migratedRootKeys += rootKey
        }
    }

    private fun pruneMigratedKeysFromMainConfig(migratedRootKeys: Set<String>) {
        if (migratedRootKeys.isEmpty()) return

        val mainConfig = managedFiles.getValue(MAIN_CONFIG_SPEC).yaml
        migratedRootKeys.forEach { rootKey -> mainConfig.set(rootKey, null) }
        mainConfig.save(managedFiles.getValue(MAIN_CONFIG_SPEC).file)
    }

    private fun rebuildMergedConfig() {
        val merged = YamlConfiguration()
        CONFIG_FILES.forEach { spec ->
            mergeValues(merged, managedFiles.getValue(spec).yaml, overwrite = true)
        }
        config = merged
    }

    private fun fixEmptyValues(yaml: YamlConfiguration, resourcePath: String) {
        hooker.plugin.getResource(resourcePath)?.use { inputStream ->
            val defaultYaml = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream, UTF_8))
            mergeMissingValues(target = yaml, defaults = defaultYaml)
        }
    }

    private fun mergeMissingValues(target: ConfigurationSection, defaults: ConfigurationSection) {
        defaults.getKeys(false).forEach { key ->
            val defaultValue = defaults.get(key)
            val defaultSection = defaults.getConfigurationSection(key)
            val targetSection = target.getConfigurationSection(key)
            val targetValue = target.get(key)

            when {
                defaultSection != null -> {
                    val nextTarget = targetSection ?: target.createSection(key)
                    mergeMissingValues(nextTarget, defaultSection)
                }
                defaultValue is List<*> && targetValue is List<*> -> {
                    val mergedList = targetValue.toMutableList()
                    defaultValue.forEach { item ->
                        if (!mergedList.contains(item)) {
                            mergedList += item
                        }
                    }
                    target.set(key, mergedList)
                }
                !target.contains(key) || target.get(key) == null -> {
                    target.set(key, defaultValue)
                }
            }
        }
    }

    private fun mergeValues(target: ConfigurationSection, source: ConfigurationSection, overwrite: Boolean) {
        source.getKeys(false).forEach { key ->
            copyNode(source, target, key, overwrite)
        }
    }

    private fun copyNode(
        source: ConfigurationSection,
        target: ConfigurationSection,
        key: String,
        overwrite: Boolean
    ) {
        val sourceSection = source.getConfigurationSection(key)
        if (sourceSection != null) {
            val nextTarget = target.getConfigurationSection(key) ?: target.createSection(key)
            mergeValues(nextTarget, sourceSection, overwrite)
            return
        }

        if (overwrite || !target.contains(key)) {
            target.set(key, source.get(key))
        }
    }

    private fun resolveOwner(path: String): ManagedConfigFile {
        val rootKey = path.substringBefore('.')
        return managedFiles.values.firstOrNull { rootKey in it.spec.rootKeys }
            ?: managedFiles.getValue(MAIN_CONFIG_SPEC)
    }

    private data class ConfigFileSpec(
        val resourcePath: String,
        val rootKeys: Set<String>
    )

    private data class ManagedConfigFile(
        val spec: ConfigFileSpec,
        val file: File,
        var yaml: YamlConfiguration = YamlConfiguration()
    )

    companion object {
        private const val STRING_TO_NUMERIC_IDS_ROOT = "string-to-numeric-ids"

        private val MAIN_CONFIG_SPEC = ConfigFileSpec(
            resourcePath = "config.yml",
            rootKeys = setOf("cooldowns", "blocked-disguises", "systems")
        )

        private val CONFIG_FILES = listOf(
            MAIN_CONFIG_SPEC,
            ConfigFileSpec(resourcePath = "configs/permissions.yml", rootKeys = setOf("roles")),
            ConfigFileSpec(resourcePath = "configs/plotsquared.yml", rootKeys = setOf("plotsquared")),
            ConfigFileSpec(resourcePath = "configs/decoration-heads.yml", rootKeys = setOf("decoration-heads")),
            ConfigFileSpec(resourcePath = "configs/banner.yml", rootKeys = setOf("banner")),
            ConfigFileSpec(resourcePath = "configs/itemdb.yml", rootKeys = setOf(STRING_TO_NUMERIC_IDS_ROOT))
        )
    }
}
