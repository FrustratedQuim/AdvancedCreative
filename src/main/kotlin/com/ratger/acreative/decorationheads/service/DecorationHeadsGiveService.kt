package com.ratger.acreative.decorationheads.service

import com.ratger.acreative.decorationheads.model.DecorationHeadEntry
import com.ratger.acreative.itemedit.head.HeadTextureMutationSupport
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DecorationHeadsGiveService(
    private val headTextureMutationSupport: HeadTextureMutationSupport,
    private val recentService: DecorationHeadsRecentService
) {
    fun give(player: Player, entry: DecorationHeadEntry) {
        val item = ItemStack(Material.PLAYER_HEAD)
        val result = headTextureMutationSupport.applyFromTextureValue(item, entry.textureValue)
        if (result !is HeadTextureMutationSupport.MutationResult.Success) return

        val empty = player.inventory.firstEmpty()
        if (empty != -1) {
            player.inventory.setItem(empty, item)
        } else {
            player.world.dropItemNaturally(player.location.clone().add(0.0, 1.0, 0.0), item)
        }
        recentService.push(player.uniqueId, entry)
    }
}
