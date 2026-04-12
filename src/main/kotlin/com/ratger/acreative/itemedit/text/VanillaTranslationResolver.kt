package com.ratger.acreative.itemedit.text

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class VanillaTranslationResolver(
    dataDirectory: Path,
    private val logger: Logger,
    private val minecraftVersion: String = "1.21.4"
) {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()
    private val cacheDirectory: Path = dataDirectory.resolve("lang-cache")
    private val bundles = ConcurrentHashMap<String, Map<String, String>>()

    fun resolve(translationKey: String, locale: Locale): String? {
        val localeTag = normalizeLocale(locale)
        val localized = loadBundle(localeTag)[translationKey]
        if (localized != null) return localized
        if (localeTag == DEFAULT_LOCALE) return null
        return loadBundle(DEFAULT_LOCALE)[translationKey]
    }

    private fun loadBundle(localeTag: String): Map<String, String> {
        bundles[localeTag]?.let { return it }

        val loaded = runCatching {
            Files.createDirectories(cacheDirectory)
            val filePath = cacheDirectory.resolve("$localeTag.json")
            val json = if (Files.exists(filePath)) {
                Files.readString(filePath, StandardCharsets.UTF_8)
            } else {
                val downloaded = download(localeTag)
                if (downloaded != null) {
                    Files.writeString(filePath, downloaded, StandardCharsets.UTF_8)
                }
                downloaded ?: "{}"
            }
            parseLangJson(json)
        }.onFailure {
            logger.fine("Failed to load lang bundle for $localeTag: ${it.message}")
        }.getOrElse { emptyMap() }

        bundles[localeTag] = loaded
        return loaded
    }

    private fun parseLangJson(rawJson: String): Map<String, String> {
        val out = HashMap<String, String>()
        var i = 0
        val n = rawJson.length

        fun skipWhitespace() {
            while (i < n && rawJson[i].isWhitespace()) i++
        }

        fun parseString(): String? {
            if (i >= n || rawJson[i] != '"') return null
            i++
            val sb = StringBuilder()
            while (i < n) {
                val ch = rawJson[i++]
                if (ch == '"') return sb.toString()
                if (ch != '\\') {
                    sb.append(ch)
                    continue
                }
                if (i >= n) return null
                when (val esc = rawJson[i++]) {
                    '"', '\\', '/' -> sb.append(esc)
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 4 > n) return null
                        val hex = rawJson.substring(i, i + 4)
                        val code = hex.toIntOrNull(16) ?: return null
                        sb.append(code.toChar())
                        i += 4
                    }
                    else -> return null
                }
            }
            return null
        }

        skipWhitespace()
        if (i >= n || rawJson[i] != '{') return emptyMap()
        i++

        while (true) {
            skipWhitespace()
            if (i < n && rawJson[i] == '}') break

            val key = parseString() ?: return out
            skipWhitespace()
            if (i >= n || rawJson[i] != ':') return out
            i++
            skipWhitespace()
            val value = parseString() ?: return out
            out[key] = value

            skipWhitespace()
            if (i >= n) return out
            val sep = rawJson[i]
            if (sep == ',') {
                i++
                continue
            }
            if (sep == '}') break
            return out
        }

        return out
    }

    private fun download(localeTag: String): String? {
        val url = LANG_URL_TEMPLATE
            .replace("{version}", minecraftVersion)
            .replace("{locale}", localeTag)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(4))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        return if (response.statusCode() in 200..299) response.body() else null
    }

    private fun normalizeLocale(locale: Locale): String {
        val raw = locale.toLanguageTag().replace('-', '_').lowercase(Locale.ROOT)
        return if (raw.contains('_')) raw else "${raw}_${raw}"
    }

    companion object {
        private const val DEFAULT_LOCALE = "en_us"
        private const val LANG_URL_TEMPLATE =
            "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/assets/minecraft/lang/{locale}.json"
    }
}
