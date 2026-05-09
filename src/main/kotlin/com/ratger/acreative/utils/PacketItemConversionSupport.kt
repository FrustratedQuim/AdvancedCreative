package com.ratger.acreative.utils

import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import com.ratger.acreative.menus.edit.container.LockItemSupport
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object PacketItemConversionSupport {

    fun toPacket(item: ItemStack): PacketItemStack {
        return runCatching {
            SpigotConversionUtil.fromBukkitItemStack(item)
        }.getOrElse {
            convertSanitized(item)
        }
    }

    private fun convertSanitized(item: ItemStack): PacketItemStack {
        val sanitized = item.clone()
        if (LockItemSupport.supports(sanitized)) {
            LockItemSupport.clear(sanitized)
        }

        return runCatching {
            SpigotConversionUtil.fromBukkitItemStack(sanitized)
        }.getOrElse {
            fallback(sanitized)
        }
    }

    private fun fallback(item: ItemStack): PacketItemStack {
        val itemType = ItemTypes.getByName(item.type.key.toString()) ?: ItemTypes.AIR
        val amount = if (item.type == Material.AIR) 0 else item.amount.coerceAtLeast(1)
        return PacketItemStack.builder()
            .type(itemType)
            .amount(amount)
            .build()
    }
}
