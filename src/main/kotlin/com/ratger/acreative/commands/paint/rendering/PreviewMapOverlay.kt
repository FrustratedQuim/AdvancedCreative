package com.ratger.acreative.commands.paint.rendering

data class PreviewMapOverlay(
    val mapId: Int,
    val indices: IntArray,
    val colors: ByteArray,
    val fingerprint: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreviewMapOverlay

        if (mapId != other.mapId) return false
        if (fingerprint != other.fingerprint) return false
        if (!indices.contentEquals(other.indices)) return false
        if (!colors.contentEquals(other.colors)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mapId
        result = 31 * result + fingerprint.hashCode()
        result = 31 * result + indices.contentHashCode()
        result = 31 * result + colors.contentHashCode()
        return result
    }
}

internal class PreviewOverlayBuilder(private val mapId: Int) {
    private val positionsByMapIndex = IntArray(MAP_AREA) { -1 }
    private var indices = IntArray(INITIAL_CAPACITY)
    private var colors = ByteArray(INITIAL_CAPACITY)
    private var size = 0

    fun put(index: Int, color: Byte) {
        val existingPosition = positionsByMapIndex[index]
        if (existingPosition >= 0) {
            colors[existingPosition] = color
            return
        }
        ensureCapacity(size + 1)
        positionsByMapIndex[index] = size
        indices[size] = index
        colors[size] = color
        size += 1
    }

    fun build(): PreviewMapOverlay {
        val compactIndices = indices.copyOf(size)
        val compactColors = colors.copyOf(size)
        var hash = 1_125_899_906_842_597L
        for (i in 0 until size) {
            hash = hash * 1_099_511_628_211L + compactIndices[i]
            hash = hash * 1_099_511_628_211L + (compactColors[i].toInt() and 0xFF)
        }
        return PreviewMapOverlay(mapId, compactIndices, compactColors, hash)
    }

    private fun ensureCapacity(required: Int) {
        if (required <= indices.size) return
        val newSize = kotlin.math.max(required, indices.size * 2)
        indices = indices.copyOf(newSize)
        colors = colors.copyOf(newSize)
    }

    private companion object {
        private const val INITIAL_CAPACITY = 256
        private const val MAP_AREA = 128 * 128
    }
}
