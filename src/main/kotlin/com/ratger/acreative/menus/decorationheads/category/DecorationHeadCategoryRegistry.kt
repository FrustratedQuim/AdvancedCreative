package com.ratger.acreative.menus.decorationheads.category

import org.bukkit.configuration.file.FileConfiguration

class DecorationHeadCategoryRegistry(config: FileConfiguration) {
    val definitions: List<DecorationHeadCategoryDefinition>

    init {
        val section = config.getConfigurationSection("decoration-heads.categories")
        definitions = section?.getKeys(false)?.map { key ->
            val mode = runCatching {
                DecorationHeadCategoryMode.valueOf(
                    section.getString("$key.mode", "CATEGORY_GROUP")!!.uppercase()
                )
            }.getOrDefault(DecorationHeadCategoryMode.CATEGORY_GROUP)
            DecorationHeadCategoryDefinition(
                key = key,
                displayName = section.getString("$key.display", key)!!,
                mode = mode,
                apiNames = section.getStringList("$key.apiNames")
            )
        } ?: emptyList()
    }

    fun byKey(key: String): DecorationHeadCategoryDefinition? = definitions.firstOrNull { it.key == key }
    fun firstCategoryKey(): String = definitions.firstOrNull()?.key ?: "new"
}
