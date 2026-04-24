package com.ratger.acreative.menus.decorationheads.service

import com.ratger.acreative.menus.decorationheads.persistence.CatalogRepository
import java.io.File
import java.util.logging.Logger

class HeadCatalogRestoreService(
    private val catalogRepository: CatalogRepository,
    private val syncService: SyncService,
    private val fallbackCatalogReader: HeadFallbackCatalogReader,
    private val dataFolder: File,
    private val logger: Logger
) {
    companion object {
        private const val POPULATED_CATALOG_THRESHOLD = 3
    }

    sealed interface RestoreResult {
        data object AlreadyPopulated : RestoreResult
        data object DatFileMissing : RestoreResult
        data object ApiKeyMissing : RestoreResult
        data object LoadError : RestoreResult
        data class Success(val amount: Int, val source: RestoreSource) : RestoreResult
    }

    enum class RestoreSource {
        DAT,
        API
    }

    fun restoreFromDat(): RestoreResult {
        if (isCatalogPopulated()) return RestoreResult.AlreadyPopulated

        val datFile = HeadFallbackCatalog.resolveFile(dataFolder)
        if (!datFile.isFile) return RestoreResult.DatFileMissing

        val entries = runCatching { fallbackCatalogReader.read(datFile) }
            .onFailure { error ->
                logger.warning(
                    "Failed to load fallback heads from ${datFile.absolutePath}: ${error::class.simpleName}: ${error.message ?: "no details"}"
                )
            }
            .getOrDefault(emptyList())

        if (entries.isEmpty()) return RestoreResult.LoadError

        catalogRepository.replaceAll(entries)
        return RestoreResult.Success(entries.size, RestoreSource.DAT)
    }

    fun restoreFromApi(): RestoreResult {
        if (isCatalogPopulated()) return RestoreResult.AlreadyPopulated
        if (!syncService.hasApiKey()) return RestoreResult.ApiKeyMissing

        val entries = runCatching { syncService.fetchAllConfiguredHeads() }
            .onFailure { error ->
                logger.warning("Failed to restore heads from API: ${error::class.simpleName}: ${error.message ?: "no details"}")
            }
            .getOrDefault(emptyList())

        if (entries.isEmpty()) return RestoreResult.LoadError

        catalogRepository.replaceAll(entries)
        return RestoreResult.Success(entries.size, RestoreSource.API)
    }

    private fun isCatalogPopulated(): Boolean = catalogRepository.countRecentPublished() > POPULATED_CATALOG_THRESHOLD
}
