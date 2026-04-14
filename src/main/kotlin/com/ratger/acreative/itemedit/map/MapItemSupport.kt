package com.ratger.acreative.itemedit.map

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView

object MapItemSupport {

    fun color(item: ItemStack): Color? {
        val meta = item.itemMeta as? MapMeta ?: return null
        return if (meta.hasColor()) meta.color else null
    }

    fun setColor(item: ItemStack, color: Color?) {
        val meta = item.itemMeta as? MapMeta ?: return
        meta.color = color
        item.itemMeta = meta
    }

    fun mapView(item: ItemStack): MapView? {
        val meta = item.itemMeta as? MapMeta ?: return null
        return meta.mapView
    }

    fun mapId(item: ItemStack): Int? = mapView(item)?.id

    fun resolveMapView(id: Int): MapView? = Bukkit.getMap(id)

    fun setMapView(item: ItemStack, mapView: MapView?) {
        val meta = item.itemMeta as? MapMeta ?: return
        meta.mapView = mapView
        item.itemMeta = meta
    }

    fun clearMapId(item: ItemStack) {
        val preservedColor = color(item)
        setMapView(item, null)
        if (preservedColor != null) {
            setColor(item, preservedColor)
        }
    }
}
