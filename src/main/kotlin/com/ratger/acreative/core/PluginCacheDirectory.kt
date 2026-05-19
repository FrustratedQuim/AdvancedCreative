package com.ratger.acreative.core

import java.nio.file.Files
import java.nio.file.Path

object PluginCacheDirectory {
    private const val CACHE_DIRECTORY_NAME = "cache"

    fun resolve(dataDirectory: Path): Path = dataDirectory.resolve(CACHE_DIRECTORY_NAME)

    fun ensure(dataDirectory: Path): Path {
        val cacheDirectory = resolve(dataDirectory)
        Files.createDirectories(cacheDirectory)
        return cacheDirectory
    }
}
