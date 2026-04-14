package com.ratger.acreative.decorationheads.api

import java.nio.charset.StandardCharsets
import java.util.Base64

class TextureValueNormalizer {
    private companion object {
        const val TEXTURE_HOST_PREFIX = "http://textures.minecraft.net/texture/"
        val bareTextureHashRegex = Regex("^[0-9a-fA-F]{32,128}$")
        val urlFieldRegex = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"")
    }

    fun resolveTextureValue(value: String?, url: String?): String? {
        val normalizedFromValue = value
            ?.takeUnless { it.isBlank() }
            ?.let(::normalizeEncodedTextureValue)
        if (normalizedFromValue != null) return normalizedFromValue

        val normalizedUrl = normalizeTextureUrl(url ?: return null) ?: return null
        return encodeTextureValue(normalizedUrl)
    }

    fun fallbackStableKey(url: String): String {
        val suffix = url.substringAfterLast('/').ifBlank { Integer.toHexString(url.hashCode()) }
        return "u:$suffix"
    }

    private fun normalizeEncodedTextureValue(value: String): String? {
        val decoded = runCatching {
            String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null

        val match = urlFieldRegex.find(decoded) ?: return value
        val currentUrl = match.groupValues[1]
        val normalizedUrl = normalizeTextureUrl(currentUrl) ?: return value
        if (normalizedUrl == currentUrl) return value

        val normalizedPayload = decoded.replaceRange(match.range.first, match.range.last + 1, "\"url\":\"$normalizedUrl\"")
        return Base64.getEncoder().encodeToString(normalizedPayload.toByteArray(StandardCharsets.UTF_8))
    }

    private fun normalizeTextureUrl(raw: String): String? {
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) return candidate

        val suffix = candidate.substringAfterLast('/').trim()
        if (!bareTextureHashRegex.matches(suffix)) return null
        return "$TEXTURE_HOST_PREFIX$suffix"
    }

    private fun encodeTextureValue(url: String): String {
        val payload = "{\"textures\":{\"SKIN\":{\"url\":\"$url\"}}}"
        return Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
    }
}
