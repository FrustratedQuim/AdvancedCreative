package com.ratger.acreative.itemedit.head

enum class HeadTextureSource(val markerValue: String) {
    ONLINE_PLAYER("online_player"),
    TEXTURE_VALUE("texture_value"),
    LICENSED_NAME("licensed_name"),
    NONE("none");

    companion object {
        fun fromMarker(value: String?): HeadTextureSource? {
            return entries.firstOrNull { it.markerValue == value }
        }
    }
}
