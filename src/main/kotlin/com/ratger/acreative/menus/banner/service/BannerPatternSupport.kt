package com.ratger.acreative.menus.banner.service

import com.ratger.acreative.menus.banner.model.BannerProfileSnapshot
import com.ratger.acreative.menus.edit.text.VanillaRuLocalization
import org.bukkit.Bukkit
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.SkullMeta

object BannerPatternSupport {
    const val EDITOR_VISIBLE_PATTERN_LIMIT: Int = 16

    data class VisibleBannerPattern(
        val actualIndex: Int,
        val pattern: Pattern,
        val descriptor: com.ratger.acreative.menus.banner.model.BannerPatternDescriptor
    )

    fun isBanner(item: ItemStack?): Boolean {
        return item != null && item.type.name.endsWith("_BANNER")
    }

    fun normalizeForStorage(item: ItemStack): ItemStack? {
        if (!isBanner(item)) return null
        val sourceMeta = item.itemMeta as? BannerMeta ?: return null
        val clean = ItemStack(item.type, 1)
        val cleanMeta = clean.itemMeta as? BannerMeta ?: return null
        cleanMeta.patterns = sourceMeta.patterns.toList()
        clean.itemMeta = cleanMeta
        return clean
    }

    fun localizedBaseName(item: ItemStack): String =
        VanillaRuLocalization.itemName(item.type.key.key)

    fun patterns(item: ItemStack?): List<Pattern> {
        val meta = item?.itemMeta as? BannerMeta ?: return emptyList()
        return meta.patterns.toList()
    }

    fun patternCount(item: ItemStack?): Int = patterns(item).size

    fun clearPatterns(item: ItemStack?): Boolean {
        val meta = item?.itemMeta as? BannerMeta ?: return false
        if (meta.patterns.isEmpty()) return false
        meta.patterns = emptyList()
        item.itemMeta = meta
        return true
    }

    fun addPattern(item: ItemStack?, color: DyeColor, type: PatternType): Boolean {
        val meta = item?.itemMeta as? BannerMeta ?: return false
        meta.addPattern(Pattern(color, type))
        item.itemMeta = meta
        return true
    }

    fun removePatternAt(item: ItemStack?, actualIndex: Int): Boolean {
        val meta = item?.itemMeta as? BannerMeta ?: return false
        if (actualIndex !in 0 until meta.numberOfPatterns()) return false
        meta.removePattern(actualIndex)
        item.itemMeta = meta
        return true
    }

    fun replacePatternAt(item: ItemStack?, actualIndex: Int, color: DyeColor, type: PatternType): Boolean {
        val meta = item?.itemMeta as? BannerMeta ?: return false
        if (actualIndex !in 0 until meta.numberOfPatterns()) return false
        meta.setPattern(actualIndex, Pattern(color, type))
        item.itemMeta = meta
        return true
    }

    fun swapPatterns(item: ItemStack?, firstIndex: Int, secondIndex: Int): Boolean {
        val meta = item?.itemMeta as? BannerMeta ?: return false
        val patternCount = meta.numberOfPatterns()
        if (patternCount <= 1) return false
        if (firstIndex !in 0 until patternCount) return false
        if (secondIndex !in 0 until patternCount) return false
        if (firstIndex == secondIndex) return true

        val mutablePatterns = meta.patterns.toMutableList()
        val firstPattern = mutablePatterns[firstIndex]
        mutablePatterns[firstIndex] = mutablePatterns[secondIndex]
        mutablePatterns[secondIndex] = firstPattern
        meta.patterns = mutablePatterns
        item.itemMeta = meta
        return true
    }

    fun visiblePatterns(item: ItemStack?): List<VisibleBannerPattern> {
        val actualPatterns = patterns(item)
        if (actualPatterns.isEmpty()) return emptyList()
        val visible = actualPatterns.take(EDITOR_VISIBLE_PATTERN_LIMIT)
        return visible.mapIndexedNotNull { actualIndex, pattern ->
            val descriptor = BannerCatalog.patternByType(pattern.pattern) ?: return@mapIndexedNotNull null
            VisibleBannerPattern(actualIndex, pattern, descriptor)
        }
    }

    fun previewWithPattern(
        item: ItemStack,
        color: DyeColor?,
        type: PatternType?,
        replaceActualIndex: Int? = null
    ): ItemStack {
        val preview = item.clone().apply { amount = 1 }
        if (color == null || type == null) {
            return preview
        }

        if (replaceActualIndex != null && replacePatternAt(preview, replaceActualIndex, color, type)) {
            return preview
        }

        addPattern(preview, color, type)
        return preview
    }

    fun patternSignature(item: ItemStack): String? {
        val normalized = normalizeForStorage(item) ?: return null
        val meta = normalized.itemMeta as? BannerMeta ?: return null
        val parts = buildList {
            add(normalized.type.key.toString())
            meta.patterns.forEach { pattern ->
                add("${pattern.color.name}:${pattern.pattern.key}")
            }
        }
        return parts.joinToString("|")
    }

    fun createProfiledHead(
        name: String,
        snapshot: BannerProfileSnapshot?
    ): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD)
        val skull = head.itemMeta as? SkullMeta ?: return head
        if (snapshot != null) {
            @Suppress("DEPRECATION") // Paper still routes custom texture injection through the PlayerProfile bridge here.
            val profile = Bukkit.createProfile(null as java.util.UUID?, name)
            profile.setProperty(com.destroystokyo.paper.profile.ProfileProperty("textures", snapshot.value, snapshot.signature))
            skull.playerProfile = profile
        }
        head.itemMeta = skull
        return head
    }
}
