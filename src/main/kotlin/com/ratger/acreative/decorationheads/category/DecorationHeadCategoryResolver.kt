package com.ratger.acreative.decorationheads.category

import java.util.concurrent.ConcurrentHashMap

class DecorationHeadCategoryResolver {
    private val apiNameToId = ConcurrentHashMap<String, Int>()
    private val resolvedByUiKey = ConcurrentHashMap<String, Set<Int>>()

    fun applyApiCategories(apiCategories: Map<String, Int>, definitions: List<DecorationHeadCategoryDefinition>): List<String> {
        apiNameToId.clear()
        apiNameToId.putAll(apiCategories.mapKeys { it.key.lowercase() })
        resolvedByUiKey.clear()

        val warnings = mutableListOf<String>()
        definitions.forEach { def ->
            val ids = def.apiNames.mapNotNull { apiNameToId[it.lowercase()] }.toSet()
            resolvedByUiKey[def.key] = ids
            if (def.mode == DecorationHeadCategoryMode.CATEGORY_GROUP && ids.isEmpty()) {
                warnings += "Decoration heads category '${def.key}' unresolved for apiNames=${def.apiNames}"
            }
        }
        return warnings
    }

    fun resolveUiCategoryToApiIds(uiCategoryKey: String): Set<Int> = resolvedByUiKey[uiCategoryKey] ?: emptySet()
}
