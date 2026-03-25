package com.ratger.acreative.commands.edit

import com.ratger.acreative.core.FunctionHooker
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class EditTargetResolver(private val hooker: FunctionHooker) {
    private val mini = MiniMessage.miniMessage()
    private val stateKey = NamespacedKey(hooker.plugin, "edit_state_version")

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

    fun markPluginState(item: ItemStack) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(stateKey, PersistentDataType.INTEGER, 1)
        item.itemMeta = meta
    }

    fun clearPluginState(item: ItemStack) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.remove(stateKey)
        item.itemMeta = meta
    }

    fun snapshot(item: ItemStack): EditStateSnapshot {
        val meta = item.itemMeta
        val pluginState = meta?.persistentDataContainer?.has(stateKey, PersistentDataType.INTEGER) == true
        return EditStateSnapshot(
            type = item.type.key.key,
            amount = item.amount,
            hasName = meta?.hasDisplayName() == true,
            loreSize = meta?.lore()?.size ?: 0,
            isPotion = item.type.name.endsWith("POTION") || item.type == Material.TIPPED_ARROW,
            isArmor = item.type.name.endsWith("_HELMET") || item.type.name.endsWith("_CHESTPLATE") || item.type.name.endsWith("_LEGGINGS") || item.type.name.endsWith("_BOOTS"),
            isHead = item.type == Material.PLAYER_HEAD,
            isShulker = item.type.name.endsWith("SHULKER_BOX"),
            hasPluginState = pluginState
        )
    }
}
