package com.ratger.acreative.menus.edit.apply.map

import com.ratger.acreative.menus.edit.apply.preset.ApplyPresetCatalog
import com.ratger.acreative.menus.edit.apply.core.ApplyExecutionResult
import com.ratger.acreative.menus.edit.apply.core.EditorApplyHandler
import com.ratger.acreative.menus.edit.apply.core.EditorApplyActionKind
import com.ratger.acreative.menus.apply.ApplyInputSpecs
import com.ratger.acreative.menus.edit.map.MapItemSupport
import com.ratger.acreative.menus.edit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class MapIdApplyHandler(
    private val validationService: ValidationService
) : EditorApplyHandler {
    override val kind: EditorApplyActionKind = EditorApplyActionKind.MAP_ID
    override val inputSpec = ApplyInputSpecs.AMOUNT

    override fun apply(player: Player, session: ItemEditSession, args: Array<out String>): ApplyExecutionResult {
        if (!validationService.isMapEditable(session.editableItem)) return ApplyExecutionResult.InvalidValue
        if (args.size != 1) return ApplyExecutionResult.InvalidValue

        val mapId = args[0].toIntOrNull() ?: return ApplyExecutionResult.InvalidValue
        if (!validationService.isValidMapId(mapId)) return ApplyExecutionResult.InvalidValue

        val mapView = MapItemSupport.resolveMapView(mapId) ?: return ApplyExecutionResult.UnknownValue
        MapItemSupport.setMapView(session.editableItem, mapView)
        return ApplyExecutionResult.Success
    }

    override fun suggestions(args: Array<out String>): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0]
        return ApplyPresetCatalog.getPresets(kind).filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
