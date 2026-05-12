package com.ratger.acreative.menus.edit.apply.effects

import com.ratger.acreative.commands.edit.EditTargetResolver
import com.ratger.acreative.menus.edit.ItemEditSession
import com.ratger.acreative.menus.edit.api.ItemAction
import com.ratger.acreative.menus.edit.api.ItemContext
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.edit.apply.core.OneArgumentEditorApplyHandler
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.effects.EdibleMenuSupport
import com.ratger.acreative.menus.edit.effects.FoodComponentSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import org.bukkit.entity.Player

class FoodNutritionApplyHandler(
    private val validationService: ValidationService,
    private val targetResolver: EditTargetResolver
) : OneArgumentEditorApplyHandler<Int>() {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.FOOD_NUTRITION
    override val inputSpec = ApplyInputSpecs.AMOUNT
    override val presets: List<String> = listOf("2", "4", "8", "20")

    override fun parseValue(rawValue: String, session: ItemEditSession): Int? {
        return rawValue.toIntOrNull()?.takeIf { it >= 0 }
    }

    override fun applyValue(player: Player, session: ItemEditSession, value: Int): ApplyExecutionResult {
        val action = ItemAction.FoodNutrition(value)
        val context = ItemContext(session.editableItem, targetResolver.snapshot(session.editableItem))
        if (!validationService.validate(action, context, player)) return ApplyExecutionResult.InvalidValue

        EdibleMenuSupport.ensureEnabledWithDefaults(session.editableItem)
        FoodComponentSupport.setNutrition(session.editableItem, value)
        return ApplyExecutionResult.Success
    }
}
