package com.ratger.acreative.menus.banner.storage

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack

class BannerStorageNameSupport {
    private val plainText = PlainTextComponentSerializer.plainText()

    fun extractPlainTitle(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        val fromComponent = meta.customName()?.let(plainText::serialize)
        val fromLegacy = meta.takeIf { it.hasDisplayName() }?.displayName

        val raw = (fromComponent ?: fromLegacy.orEmpty())
            .let(ChatColor::stripColor)
            ?.replace('\n', ' ')
            ?.trim()
            .orEmpty()

        if (raw.isBlank()) {
            return null
        }

        return raw.take(MAX_TITLE_LENGTH)
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 64
    }
}
