package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditParsers
import com.ratger.acreative.itemedit.effects.ConsumableComponentSupport
import com.ratger.acreative.itemedit.effects.EdibleMenuSupport
import com.ratger.acreative.itemedit.potion.PotionItemSupport
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class ConsumableRemoveEffectAddApplyHandler(
    private val parser: EditParsers
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.CONSUMABLE_REMOVE_EFFECT_ADD

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val effectType = parser.effectFromToken(args[0]) ?: return ApplyExecutionResult.UnknownValue

        EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
        ConsumableComponentSupport.addRemovedEffect(session.editableItem, effectType)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return PotionItemSupport.effectSuggestions(args[0])
    }
}
