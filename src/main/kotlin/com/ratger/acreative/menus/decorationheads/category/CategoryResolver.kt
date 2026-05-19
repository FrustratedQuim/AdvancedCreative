package com.ratger.acreative.menus.decorationheads.category

import java.util.concurrent.ConcurrentHashMap

class CategoryResolver {
    private val resolvedByUiKey = ConcurrentHashMap<String, Set<Int>>()
    private val uiKeyByApiCategoryId = ConcurrentHashMap<Int, String>()

    data class ResolutionSnapshot(
        val resolvedByUiKey: Map<String, Set<Int>>,
        val uiKeyByApiCategoryId: Map<Int, String>,
        val warnings: List<String>
    ) {
        val resolvedGroupCount: Int = resolvedByUiKey.values.count { it.isNotEmpty() }
    }

    fun buildSnapshot(apiCategories: Map<String, Int>, definitions: List<CategoryDefinition>): ResolutionSnapshot {
        val apiNameToId = apiCategories.mapKeys { it.key.lowercase() }
        val nextResolvedByUiKey = LinkedHashMap<String, Set<Int>>()
        val nextUiKeyByApiCategoryId = LinkedHashMap<Int, String>()
        val warnings = mutableListOf<String>()

        definitions.forEach { def ->
            val ids = def.apiNames.mapNotNull { apiNameToId[it.lowercase()] }.toSet()
            nextResolvedByUiKey[def.key] = ids
            ids.forEach { apiCategoryId ->
                nextUiKeyByApiCategoryId.putIfAbsent(apiCategoryId, def.key)
            }
            if (def.mode == CategoryMode.CATEGORY_GROUP && ids.isEmpty()) {
                warnings += "Decoration heads category '${def.key}' unresolved for apiNames=${def.apiNames}"
            }
        }
        return ResolutionSnapshot(
            resolvedByUiKey = nextResolvedByUiKey,
            uiKeyByApiCategoryId = nextUiKeyByApiCategoryId,
            warnings = warnings
        )
    }

    fun applySnapshot(snapshot: ResolutionSnapshot): List<String> {
        resolvedByUiKey.clear()
        resolvedByUiKey.putAll(snapshot.resolvedByUiKey)
        uiKeyByApiCategoryId.clear()
        uiKeyByApiCategoryId.putAll(snapshot.uiKeyByApiCategoryId)
        return snapshot.warnings
    }

    fun applyApiCategories(apiCategories: Map<String, Int>, definitions: List<CategoryDefinition>): List<String> =
        applySnapshot(buildSnapshot(apiCategories, definitions))

    fun resolveUiCategoryToApiIds(uiCategoryKey: String): Set<Int> = resolvedByUiKey[uiCategoryKey] ?: emptySet()

    fun resolveUiCategoryKey(apiCategoryId: Int): String? = uiKeyByApiCategoryId[apiCategoryId]

    fun resolvedGroupCount(): Int = resolvedByUiKey.values.count { it.isNotEmpty() }
}
