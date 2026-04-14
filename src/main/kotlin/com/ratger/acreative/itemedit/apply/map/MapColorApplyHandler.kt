package com.ratger.acreative.itemedit.apply.map

import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.color.ColorInputSupport
import com.ratger.acreative.itemedit.map.MapItemSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class MapColorApplyHandler(
    private val validationService: ValidationService
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.MAP_COLOR

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (!validationService.isMapEditable(session.editableItem)) return ApplyExecutionResult.InvalidValue
        if (args.size != 1) return ApplyExecutionResult.InvalidValue

        val color = ColorInputSupport.parseColor(args[0]) ?: return ApplyExecutionResult.InvalidValue
        MapItemSupport.setColor(session.editableItem, color)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        return ColorInputSupport.suggestions(args[0])
    }
}
