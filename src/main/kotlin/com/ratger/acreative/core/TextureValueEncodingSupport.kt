package com.ratger.acreative.core

import java.nio.charset.StandardCharsets
import java.util.Base64

object TextureValueEncodingSupport {
    fun encode(textureUrl: String): String {
        val payload = "{\"textures\":{\"SKIN\":{\"url\":\"$textureUrl\"}}}"
        return Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
    }
}
