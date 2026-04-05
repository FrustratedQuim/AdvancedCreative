package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

class EquipSoundApplyHandler : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.EQUIP_SOUND

    private val presets = listOf(
        "minecraft:item.totem.use",
        "minecraft:entity.player.levelup",
        "minecraft:entity.goat.screaming.ambient",
        "minecraft:ui.toast.challenge_complete",
        "minecraft:ambient.cave"
    )

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val key = NamespacedKey.fromString(args[0].trim().lowercase()) ?: return ApplyExecutionResult.InvalidValue
        val sound = Registry.SOUNDS.get(key) ?: return ApplyExecutionResult.InvalidValue

        val updated = EquippableSupport.mutateOrCreateForMenu(
            item = session.editableItem,
            preferredFallbackSlot = EquipmentSlot.HAND
        ) {
            setEquipSound(sound)
        }
        if (!updated) return ApplyExecutionResult.InvalidValue
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0]
        return presets.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
