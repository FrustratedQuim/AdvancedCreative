package com.ratger.acreative.menus.decorationheads.category

import org.bukkit.configuration.file.FileConfiguration

class CategoryRegistry(config: FileConfiguration) {
    val definitions: List<CategoryDefinition>

    init {
        val section = config.getConfigurationSection("decoration-heads.categories")
        definitions = section?.getKeys(false)?.map { key ->
            val mode = runCatching {
                CategoryMode.valueOf(
                    section.getString("$key.mode", "CATEGORY_GROUP")!!.uppercase()
                )
            }.getOrDefault(CategoryMode.CATEGORY_GROUP)
            CategoryDefinition(
                key = key,
                displayName = section.getString("$key.display", key)!!,
                mode = mode,
                apiNames = section.getStringList("$key.apiNames")
            )
        } ?: emptyList()
    }

    fun byKey(key: String): CategoryDefinition? = definitions.firstOrNull { it.key == key }
    fun firstCategoryKey(): String = definitions.firstOrNull()?.key ?: "new"
}
