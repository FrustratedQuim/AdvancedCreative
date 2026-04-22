package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.persistence.PublishedBannerRepository
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class BannerAuthorCache(
    private val repository: PublishedBannerRepository
) {
    private val authorNamesByLower = ConcurrentHashMap<String, String>()

    fun reload() {
        authorNamesByLower.clear()
        repository.listAuthorNames().forEach { authorName ->
            authorNamesByLower[authorName.lowercase(Locale.ROOT)] = authorName
        }
    }

    fun suggest(prefix: String): List<String> {
        val normalized = prefix.lowercase(Locale.ROOT)
        return authorNamesByLower.entries
            .asSequence()
            .filter { it.key.startsWith(normalized) }
            .map { it.value }
            .sortedBy { it.lowercase(Locale.ROOT) }
            .toList()
    }

    fun resolve(name: String): String? = authorNamesByLower[name.lowercase(Locale.ROOT)]
}
