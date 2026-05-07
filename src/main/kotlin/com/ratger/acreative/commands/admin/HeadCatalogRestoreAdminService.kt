package com.ratger.acreative.commands.admin

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.decorationheads.service.HeadCatalogRestoreService
import com.ratger.acreative.menus.decorationheads.service.HeadFallbackCatalog
import org.bukkit.entity.Player

class HeadCatalogRestoreAdminService(
    private val hooker: FunctionHooker
) {
    fun restoreFromDat(player: Player) {
        when (val result = hooker.subsystem.restoreCatalogFromDat()) {
            HeadCatalogRestoreService.RestoreResult.AlreadyPopulated -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_ALREADY_POPULATED)
            }
            HeadCatalogRestoreService.RestoreResult.DatFileMissing -> {
                hooker.messageManager.sendChat(
                    player,
                    MessageKey.HEAD_RESTORE_DAT_FILE_MISSING,
                    mapOf("name" to HeadFallbackCatalog.BASE_NAME)
                )
            }
            HeadCatalogRestoreService.RestoreResult.LoadError -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_LOAD_ERROR)
            }
            is HeadCatalogRestoreService.RestoreResult.Success -> {
                hooker.messageManager.sendChat(
                    player,
                    MessageKey.HEAD_RESTORE_DAT_SUCCESS,
                    mapOf("amount" to result.amount.toString())
                )
            }
            else -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_LOAD_ERROR)
            }
        }
    }

    fun restoreFromApi(player: Player) {
        when (val result = hooker.subsystem.restoreCatalogFromApi()) {
            HeadCatalogRestoreService.RestoreResult.AlreadyPopulated -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_ALREADY_POPULATED)
            }
            HeadCatalogRestoreService.RestoreResult.ApiKeyMissing -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_API_KEY_MISSING)
            }
            HeadCatalogRestoreService.RestoreResult.LoadError -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_LOAD_ERROR)
            }
            is HeadCatalogRestoreService.RestoreResult.Success -> {
                hooker.messageManager.sendChat(
                    player,
                    MessageKey.HEAD_RESTORE_API_SUCCESS,
                    mapOf("amount" to result.amount.toString())
                )
            }
            else -> {
                hooker.messageManager.sendChat(player, MessageKey.HEAD_RESTORE_LOAD_ERROR)
            }
        }
    }
}
