package com.ratger.acreative.decorationheads.api

import java.nio.charset.StandardCharsets
import java.util.Base64

class TextureValueNormalizer {
    fun resolveTextureValue(value: String?, url: String?): String? {
        if (!value.isNullOrBlank()) return value
        if (url.isNullOrBlank()) return null
        val payload = "{\"textures\":{\"SKIN\":{\"url\":\"$url\"}}}"
        return Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
    }

    fun fallbackStableKey(url: String): String {
        val suffix = url.substringAfterLast('/').ifBlank { Integer.toHexString(url.hashCode()) }
        return "u:$suffix"
    }
}
