package com.ratger.acreative.commands.admin

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class AdminManager(
    hooker: FunctionHooker
) {
    private val memoryUsageReporter = MemoryUsageReporter(hooker)
    private val toggleStatusReporter = ToggleStatusReporter(hooker)
    private val headCatalogRestoreService = HeadCatalogRestoreAdminService(hooker)

    fun showMemoryUsage(player: Player) = memoryUsageReporter.show(player)

    fun showToggleStatus(player: Player) = toggleStatusReporter.show(player)

    fun restoreHeadsFromDat(player: Player) = headCatalogRestoreService.restoreFromDat(player)

    fun restoreHeadsFromApi(player: Player) = headCatalogRestoreService.restoreFromApi(player)
}
