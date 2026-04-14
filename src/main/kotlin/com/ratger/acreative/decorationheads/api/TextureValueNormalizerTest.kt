package com.ratger.acreative.decorationheads.api

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TextureValueNormalizerTest {
    private val normalizer = TextureValueNormalizer()

    @Test
    fun `adds minecraft texture host prefix for short hash inside base64 value`() {
        val shortHash = "85204a6b56632212a330c84db4857afe5b11af6584c4ccfb2cdb0e9f1239f"
        val badValue = encode("{\"textures\":{\"SKIN\":{\"url\":\"$shortHash\"}}}")

        val normalized = normalizer.resolveTextureValue(badValue, null)
        val decoded = decode(assertNotNull(normalized))

        assertEquals(
            "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/$shortHash\"}}}",
            decoded
        )
    }

    @Test
    fun `creates texture value from short hash url`() {
        val shortHash = "fe8e80cad773bbbeb06d9c1ca6707361ba4746f7513be1dae24215f86c7c1"

        val normalized = normalizer.resolveTextureValue(null, shortHash)
        val decoded = decode(assertNotNull(normalized))

        assertEquals(
            "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/$shortHash\"}}}",
            decoded
        )
    }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String = String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
}
