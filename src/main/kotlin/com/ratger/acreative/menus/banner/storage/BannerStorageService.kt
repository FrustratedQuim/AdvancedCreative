package com.ratger.acreative.menus.banner.storage

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class BannerStorageService(
    private val repository: BannerStorageRepository,
    private val normalizer: BannerStorageItemNormalizer,
    private val configResolver: BannerStorageConfigResolver
) {
    data class LimitSnapshot(
        val current: Int,
        val limit: Int,
        val limitText: String
    )

    fun loadLayout(player: Player): MutableMap<Int, ItemStack> {
        return repository.load(player.uniqueId)
            .associate { it.slotIndex to it.item }
            .toMutableMap()
    }

    fun saveLayout(player: Player, layout: Map<Int, ItemStack>) {
        val normalized = normalizeLayout(layout)
        repository.replaceAll(player.uniqueId, normalized)
    }

    fun normalizeLayout(layout: Map<Int, ItemStack>): Map<Int, ItemStack> {
        return buildMap {
            layout.forEach { (slot, item) ->
                val normalized = normalizer.normalizeForStorage(item) ?: return@forEach
                put(slot, normalized)
            }
        }
    }

    fun maxOccupiedSlotIndex(layout: Map<Int, ItemStack>): Int = layout.keys.maxOrNull() ?: -1

    fun currentCount(layout: Map<Int, ItemStack>): Int = layout.size

    fun limitFor(player: Player): Int = configResolver.resolveLimit(player)

    fun config(): BannerStorageConfig = configResolver.readConfig()

    fun totalPages(player: Player, layout: Map<Int, ItemStack>): Int {
        val limit = limitFor(player)
        return configResolver.computeTotalPages(limit, maxOccupiedSlotIndex(layout), config())
    }

    fun pageSlice(layout: Map<Int, ItemStack>, page: Int, pageSize: Int): Map<Int, ItemStack> {
        val base = (page - 1).coerceAtLeast(0) * pageSize
        return buildMap {
            for (slot in 0 until pageSize) {
                val absolute = base + slot
                val item = layout[absolute] ?: continue
                put(slot, item)
            }
        }
    }

    fun setPageSlot(layout: MutableMap<Int, ItemStack>, page: Int, pageSize: Int, slotInPage: Int, item: ItemStack?) {
        val absolute = (page - 1).coerceAtLeast(0) * pageSize + slotInPage
        if (item == null || item.type.isAir || item.amount <= 0) {
            layout.remove(absolute)
            return
        }
        layout[absolute] = item
    }

    fun limitSnapshot(player: Player, layout: Map<Int, ItemStack>): LimitSnapshot {
        val current = currentCount(layout)
        val limit = limitFor(player)
        val text = if (limit < 0) "∞" else limit.toString()
        return LimitSnapshot(current, limit, text)
    }

    fun plainTitle(item: ItemStack): String? = normalizer.plainTitle(item)
}
