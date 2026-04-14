package com.ratger.acreative.itemedit.apply.map

import com.ratger.acreative.itemedit.apply.core.ApplyExecutionResult
import com.ratger.acreative.itemedit.apply.core.EditorApplyHandler
import com.ratger.acreative.itemedit.apply.core.EditorApplyKind
import com.ratger.acreative.itemedit.map.MapItemSupport
import com.ratger.acreative.itemedit.validation.ValidationService
import com.ratger.acreative.menus.edit.ItemEditSession
import org.bukkit.entity.Player

class MapIdApplyHandler(
    private val validationService: ValidationService
) : EditorApplyHandler {
    override val kind: EditorApplyKind = EditorApplyKind.MAP_ID

    private val presets = listOf("1", "5", "10")

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
        return presets.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
