package com.ratger.acreative.commands.paint

import org.bukkit.Material
import org.bukkit.map.MapPalette
import java.awt.Color

enum class PaintPalette(
    val material: Material,
    val mapColor: Byte
) {
    WHITE(Material.WHITE_DYE, MapPalette.matchColor(Color.WHITE)),
    RED(Material.RED_DYE, MapPalette.matchColor(Color.RED)),
    GREEN(Material.GREEN_DYE, MapPalette.matchColor(Color.GREEN)),
    BLUE(Material.BLUE_DYE, MapPalette.matchColor(Color.BLUE));

    companion object {
        fun fromMaterial(material: Material): PaintPalette? = entries.firstOrNull { it.material == material }
    }
}
