package com.ratger.acreative.menus.banner.storage

import com.ratger.acreative.menus.banner.service.BannerPatternSupport
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack

class BannerStorageItemNormalizer(
    private val nameSupport: BannerStorageNameSupport
) {
    fun normalizeForStorage(source: ItemStack): ItemStack? {
        val normalized = BannerPatternSupport.normalizeForStorage(source) ?: return null
        val title = nameSupport.extractPlainTitle(source)

        normalized.editMeta { meta ->
            meta.customName(
                title?.let {
                    Component.text(it).decoration(TextDecoration.ITALIC, false)
                }
            )
            meta.lore(null)
        }

        return normalized
    }

    fun plainTitle(item: ItemStack): String? = nameSupport.extractPlainTitle(item)
}
