@file:Suppress("UnstableApiUsage") // Experimental Sound

package com.ratger.acreative.itemedit.apply.head

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.equippable.EquippableSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

class EquipSoundApplyHandler(
    private val parser: EditParsers
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.EQUIP_SOUND

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val key = parser.parseSoundNamespacedKey(args[0]) ?: return ApplyExecutionResult.InvalidValue
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
        return parser.soundSuggestions(args[0])
    }
}
