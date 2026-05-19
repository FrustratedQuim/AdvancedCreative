package com.ratger.acreative.commands.admin.npc

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Base64

class NpcEquipmentCodec {
    fun captureFrom(player: Player): NpcEquipment = NpcEquipment(
        helmet = normalize(player.inventory.helmet),
        chestplate = normalize(player.inventory.chestplate),
        leggings = normalize(player.inventory.leggings),
        boots = normalize(player.inventory.boots),
        mainHand = normalize(player.inventory.itemInMainHand),
        offHand = normalize(player.inventory.itemInOffHand)
    )

    fun normalize(item: ItemStack?): ItemStack? {
        val clone = item?.clone() ?: return null
        if (clone.type.isAir || clone.amount <= 0) {
            return null
        }
        clone.amount = 1
        return clone
    }

    fun serialize(item: ItemStack?): String? {
        val normalized = normalize(item) ?: return null
        return Base64.getEncoder().encodeToString(normalized.serializeAsBytes())
    }

    fun deserialize(value: String?): ItemStack? {
        val encoded = value?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        return runCatching {
            val bytes = Base64.getDecoder().decode(encoded)
            ItemStack.deserializeBytes(bytes)
        }.getOrNull()?.let(::normalize)
    }
}
