package com.ratger.acreative.integration.plotsquared.commands

import com.plotsquared.core.PlotSquared
import com.plotsquared.core.database.DBFunc
import org.bukkit.Bukkit
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class PlotOwnerSuggestionService {

    private data class OwnerNameEntry(
        val uuid: UUID,
        val name: String
    )

    private val ownerNamesByLowerName = LinkedHashMap<String, OwnerNameEntry>()
    private val ownerCacheLock = Any()
    private val pendingWarmups = ConcurrentHashMap.newKeySet<UUID>()
    private val homeBasePlotCountByOwner = ConcurrentHashMap<UUID, Int>()

    @Volatile
    private var ownerCacheUpdatedAt = 0L

    @Volatile
    private var ownerWarmupInFlight = false

    fun invalidateOwnerSuggestions() = invalidateOwnerCache()

    fun invalidateHomeCount(ownerId: UUID) {
        homeBasePlotCountByOwner.remove(ownerId)
    }

    fun resolvePlotOwnerName(input: String): String? {
        refreshOwnerCacheIfNeeded()
        return ownerNamesByLowerName[normalizeNameKey(input)]?.name
    }

    fun resolvePlotOwnerUuid(input: String): UUID? {
        refreshOwnerCacheIfNeeded()
        return ownerNamesByLowerName[normalizeNameKey(input)]?.uuid
    }

    fun plotOwnerNames(maxResults: Int): List<String> {
        refreshOwnerCacheIfNeeded()
        return ownerNamesByLowerName.values
            .asSequence()
            .map { it.name }
            .distinctBy(::normalizeNameKey)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .take(maxResults)
            .toList()
    }

    fun completeVisitTargets(rawOwner: String, rawTarget: String, maxResults: Int): List<String> {
        val ownerUuid = resolvePlotOwnerUuid(rawOwner) ?: return emptyList()
        val pageCount = homeBasePlotCountByOwner.computeIfAbsent(ownerUuid, ::scanHomeBasePlotCount)
        if (pageCount <= 0) return emptyList()

        val prefix = rawTarget.trim()
        val numeric = (1..pageCount).asSequence().map(Int::toString)
        val extras = sequenceOf("last")

        return (numeric + extras)
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .take(maxResults)
            .toList()
    }

    fun countHomeBasePlots(ownerId: UUID): Int =
        homeBasePlotCountByOwner.computeIfAbsent(ownerId, ::scanHomeBasePlotCount)

    private fun refreshOwnerCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - ownerCacheUpdatedAt < OWNER_CACHE_TTL_MILLIS && ownerNamesByLowerName.isNotEmpty()) return

        synchronized(ownerCacheLock) {
            if (now - ownerCacheUpdatedAt < OWNER_CACHE_TTL_MILLIS && ownerNamesByLowerName.isNotEmpty()) return

            val pipeline = PlotSquared.get().impromptuUUIDPipeline
            val fresh = LinkedHashMap<String, OwnerNameEntry>()
            val unresolved = LinkedHashSet<UUID>()

            for (area in PlotSquared.get().plotAreaManager.allPlotAreas) {
                for (plot in area.plots) {
                    if (!plot.hasOwner()) continue
                    for (uuid in plot.owners) {
                        if (isReservedPlotUuid(uuid)) continue
                        val onlineName = Bukkit.getPlayer(uuid)?.name
                            ?: PlotSquared.platform().playerManager().getPlayerIfExists(uuid)?.name
                        val resolvedName = onlineName ?: pipeline.getImmediately(uuid)?.username()
                        if (resolvedName.isNullOrBlank()) unresolved += uuid
                        else fresh.putIfAbsent(normalizeNameKey(resolvedName), OwnerNameEntry(uuid, resolvedName))
                    }
                }
            }

            ownerNamesByLowerName.clear()
            ownerNamesByLowerName.putAll(fresh)
            ownerCacheUpdatedAt = now
            warmupNamesAsync(unresolved, invalidateOwners = true)
        }
    }

    private fun invalidateOwnerCache() {
        synchronized(ownerCacheLock) {
            ownerCacheUpdatedAt = 0L
            ownerNamesByLowerName.clear()
        }
    }

    private fun scanHomeBasePlotCount(ownerId: UUID): Int =
        PlotSquared.get().plotAreaManager.allPlotAreas
            .sumOf { area ->
                area.plots.count { plot -> plot.isBasePlot && plot.isOwner(ownerId) }
            }

    fun resolveScopedNames(uuids: Collection<UUID>): List<String> {
        if (uuids.isEmpty()) return emptyList()
        val pipeline = PlotSquared.get().impromptuUUIDPipeline
        val names = LinkedHashMap<String, String>()
        val unresolved = LinkedHashSet<UUID>()
        for (uuid in uuids) {
            if (isReservedPlotUuid(uuid)) continue
            val onlineName = Bukkit.getPlayer(uuid)?.name ?: PlotSquared.platform().playerManager().getPlayerIfExists(uuid)?.name
            when {
                !onlineName.isNullOrBlank() -> names.putIfAbsent(normalizeNameKey(onlineName), onlineName)
                else -> {
                    val mapping = pipeline.getImmediately(uuid)
                    if (mapping?.username().isNullOrBlank()) unresolved += uuid
                    else names.putIfAbsent(normalizeNameKey(mapping.username()), mapping.username())
                }
            }
        }
        warmupNamesAsync(unresolved, invalidateOwners = false)
        return names.values.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun warmupNamesAsync(uuids: Set<UUID>, invalidateOwners: Boolean) {
        if (uuids.isEmpty()) return
        val request = uuids.asSequence().filterNot(::isReservedPlotUuid).distinct().filter { pendingWarmups.add(it) }.toSet()
        if (request.isEmpty()) return
        if (invalidateOwners) {
            synchronized(ownerCacheLock) {
                if (ownerWarmupInFlight) {
                    request.forEach(pendingWarmups::remove)
                    return
                }
                ownerWarmupInFlight = true
            }
        }
        PlotSquared.get().impromptuUUIDPipeline.getNames(request).whenComplete { _, _ ->
            request.forEach(pendingWarmups::remove)
            if (invalidateOwners) {
                synchronized(ownerCacheLock) {
                    ownerWarmupInFlight = false
                    ownerCacheUpdatedAt = 0L
                }
            }
        }
    }

    private fun normalizeNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)
    private fun isReservedPlotUuid(uuid: UUID): Boolean = uuid == DBFunc.SERVER || uuid == DBFunc.EVERYONE

    private companion object {
        private const val OWNER_CACHE_TTL_MILLIS = 60_000L
    }
}
