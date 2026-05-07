package com.ratger.acreative.core

enum class ManagedSystem(
    val id: String,
    val displayName: String
) {
    EDIT("edit", "edit"),
    PAINT("paint", "paint"),
    DECORATION_BANNERS("decorationbanners", "decorationbanners"),
    DECORATION_HEADS("decorationheads", "decorationheads"),
    PLOT_EDIT("plot-edit", "/p edit"),
    PLOT_MASSCLAIM("plot-massclaim", "/p massclaim");

    companion object {
        private val byId = entries.associateBy { it.id.lowercase() }

        fun fromId(id: String): ManagedSystem? = byId[id.lowercase()]
    }
}
