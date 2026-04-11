package com.ratger.acreative.menus.itemEdit.apply

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.itemedit.api.ItemAction
import com.ratger.acreative.itemedit.api.ItemContext
import com.ratger.acreative.itemedit.effects.EdibleMenuSupport
import com.ratger.acreative.itemedit.effects.FoodComponentSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.itemEdit.ItemEditSession
import org.bukkit.entity.Player

class FoodNutritionApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.FOOD_NUTRITION

    private val presets = listOf("2", "4", "8", "20")

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (args.size != 1) return ApplyExecutionResult.InvalidValue
        val value = args[0].toIntOrNull() ?: return ApplyExecutionResult.InvalidValue
        if (value < 0) return ApplyExecutionResult.InvalidValue

        val action = ItemAction.FoodNutrition(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (validationService.validate(action, context, player) != null) return ApplyExecutionResult.InvalidValue

        EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
        FoodComponentSupport.setNutrition(session.editableItem, value)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return presets.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}
