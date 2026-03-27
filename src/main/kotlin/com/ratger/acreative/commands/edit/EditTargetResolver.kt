package com.ratger.acreative.commands.edit

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EditTargetResolver {
    private val mini = MiniMessage.miniMessage()

    fun resolve(player: Player): EditContext? {
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR || item.amount <= 0) {
            player.sendMessage(mini.deserialize("<red>Держите предмет в основной руке."))
            return null
        }
        return EditContext(item, snapshot(item))
    }

    fun save(player: Player, item: ItemStack) {
        player.inventory.setItemInMainHand(item)
    }

    fun snapshot(item: ItemStack): EditStateSnapshot {
        val meta = item.itemMeta
        return EditStateSnapshot(
            type = item.type.key.key,
            amount = item.amount,
            hasName = meta?.hasDisplayName() == true,
            loreSize = meta?.lore()?.size ?: 0,
            isPotion = item.type.name.endsWith("POTION") || item.type == Material.TIPPED_ARROW,
            isArmor = item.type.name.endsWith("_HELMET") || item.type.name.endsWith("_CHESTPLATE") || item.type.name.endsWith("_LEGGINGS") || item.type.name.endsWith("_BOOTS"),
            isHead = item.type == Material.PLAYER_HEAD,
            isShulker = item.type.name.endsWith("SHULKER_BOX")
        )
    }
}
