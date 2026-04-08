package com.ratger.acreative.itemedit.head

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType

object HeadSourceMarkerSupport {
    private val sourceKey = NamespacedKey("acreative", "head_source_marker")

    fun read(meta: SkullMeta): HeadTextureSource? {
        val raw = meta.persistentDataContainer.get(sourceKey, PersistentDataType.STRING) ?: return null
        return HeadTextureSource.fromMarker(raw)
    }

    fun write(meta: SkullMeta, source: HeadTextureSource) {
        if (source == HeadTextureSource.NONE) {
            clear(meta)
            return
        }
        meta.persistentDataContainer.set(sourceKey, PersistentDataType.STRING, source.markerValue)
    }

    fun clear(meta: SkullMeta) {
        meta.persistentDataContainer.remove(sourceKey)
    }
}
