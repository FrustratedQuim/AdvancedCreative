package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.api.MinecraftHeadsCategoryDto
import com.ratger.acreative.menus.decorationheads.category.ApiCategoryMapping
import com.ratger.acreative.menus.decorationheads.category.CategoryRegistry
import com.ratger.acreative.menus.decorationheads.category.CategoryResolver
import com.ratger.acreative.menus.decorationheads.persistence.CategoryMappingRepository
import com.ratger.acreative.menus.decorationheads.persistence.CategoryMappingSnapshotStorage

class CategoryMappingCacheService(
    private val categoryRegistry: CategoryRegistry,
    private val categoryResolver: CategoryResolver,
    private val repository: CategoryMappingRepository,
    private val snapshotStorage: CategoryMappingSnapshotStorage
) {
    enum class CacheSource {
        DATABASE,
        FILE
    }

    data class CachedLoadResult(
        val source: CacheSource?,
        val warnings: List<String>
    )

    data class RefreshResult(
        val applied: Boolean,
        val warnings: List<String>
    )

    fun loadCachedMappings(): CachedLoadResult {
        val mappingsFromDatabase = repository.findAll()
        if (mappingsFromDatabase.isNotEmpty()) {
            snapshotStorage.save(mappingsFromDatabase)
            return CachedLoadResult(
                source = CacheSource.DATABASE,
                warnings = applyMappings(mappingsFromDatabase)
            )
        }

        val mappingsFromFile = snapshotStorage.load()
        if (mappingsFromFile.isEmpty()) {
            return CachedLoadResult(source = null, warnings = emptyList())
        }

        repository.replaceAll(mappingsFromFile)
        return CachedLoadResult(
            source = CacheSource.FILE,
            warnings = applyMappings(mappingsFromFile)
        )
    }

    fun refreshFromApi(categories: Collection<MinecraftHeadsCategoryDto>): RefreshResult {
        val normalized = categories
            .map { ApiCategoryMapping(id = it.id, name = it.name) }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

        if (normalized.isEmpty()) {
            return RefreshResult(
                applied = false,
                warnings = listOf("Decoration heads category refresh returned no categories; keeping cached mappings")
            )
        }

        val snapshot = categoryResolver.buildSnapshot(
            apiCategories = normalized.associate { it.name to it.id },
            definitions = categoryRegistry.definitions
        )
        val currentResolvedGroups = categoryResolver.resolvedGroupCount()

        if (snapshot.resolvedGroupCount == 0) {
            return RefreshResult(
                applied = false,
                warnings = listOf("Decoration heads category refresh resolved zero configured groups; keeping cached mappings") + snapshot.warnings
            )
        }

        if (currentResolvedGroups > 0 && snapshot.resolvedGroupCount < currentResolvedGroups) {
            return RefreshResult(
                applied = false,
                warnings = listOf(
                    "Decoration heads category refresh resolved fewer groups than cached snapshot ($currentResolvedGroups -> ${snapshot.resolvedGroupCount}); keeping cached mappings"
                ) + snapshot.warnings
            )
        }

        categoryResolver.applySnapshot(snapshot)
        repository.replaceAll(normalized)
        snapshotStorage.save(normalized)
        return RefreshResult(applied = true, warnings = snapshot.warnings)
    }

    private fun applyMappings(entries: Collection<ApiCategoryMapping>): List<String> {
        val snapshot = categoryResolver.buildSnapshot(
            apiCategories = entries.associate { it.name to it.id },
            definitions = categoryRegistry.definitions
        )
        return categoryResolver.applySnapshot(snapshot)
    }
}
