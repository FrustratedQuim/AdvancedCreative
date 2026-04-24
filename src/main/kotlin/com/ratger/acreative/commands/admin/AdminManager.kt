package com.ratger.acreative.commands.admin

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import com.ratger.acreative.menus.decorationheads.service.HeadCatalogRestoreService
import com.ratger.acreative.menus.decorationheads.service.HeadFallbackCatalog
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import java.util.Locale
import kotlin.math.max

class AdminManager(
    private val hooker: FunctionHooker
) {
    private val mini = MiniMessage.miniMessage()

    private data class MemoryBlock(
        val title: String,
        val bytes: Long,
        val units: Int
    )

    fun showMemoryUsage(player: Player) {
        val report = buildReport()
        val totalText = formatBytes(report.totalBytes)

        player.sendMessage(mini.deserialize("<#FFD700><st>                          </st><<#FFE68A><b> Memory Usage </b><#FFD700>><st>                         </st>"))
        player.sendMessage(mini.deserialize("<#FFE68A><b>Всего</b> <#EDC800>- <#FFF3E0>$totalText"))

        report.mainBlocks.forEach { block ->
            val line = "<#C7A300> ● <#FFE68A>${block.title}<#EDC800>- <#FFF3E0>${formatBytes(block.bytes)} <gray>(${formatUnits(block.units)} ед.)"
            player.sendMessage(mini.deserialize(line))
        }

        player.sendMessage(mini.deserialize("<#C7A300> ● <#FFE68A>Остальное<#EDC800>- <#FFF3E0>${formatBytes(report.remainingBytes)}"))
        player.sendMessage(mini.deserialize("<#FFD700><st>                                                                             </st>"))
    }

    fun restoreHeadsFromDat(player: Player) {
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

    fun restoreHeadsFromApi(player: Player) {
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

    private fun buildReport(): MemoryReport {
        val bannerSnapshot = hooker.bannerSubsystem.memorySnapshot()
        val headsSnapshot = hooker.subsystem.memorySnapshot()
        val itemEditSessions = hooker.menuService.itemEditSessionsSnapshot()
        val hideSnapshot = hooker.hideManager.cacheSnapshot()
        val effectSnapshot = hooker.effectsManager.cacheSnapshot()
        val freezeSnapshot = hooker.freezeManager.cacheSnapshot()
        val disguiseSnapshot = hooker.disguiseManager.cacheSnapshot()
        val jarSnapshot = hooker.jarManager.cacheSnapshot()
        val commandCooldownEntries = hooker.commandManager.cooldownService.cachedEntriesCount()
        val commandCooldownPlayers = hooker.commandManager.cooldownService.cachedPlayersCount()
        val licensedProfiles = LicensedProfileLookupService.cacheEntriesSnapshot()

        val flagBlock = MemoryBlock(
            title = "Флаги",
            bytes = bannerSnapshot.authorNames.sumOf(::estimateStringBytes) +
                bannerSnapshot.publicationHistoryKeys.sumOf(::estimateStringBytes) +
                bannerSnapshot.editorSessions.sumOf { estimateSerializedItemBytes(it.editableBanner) + 256L } +
                bannerSnapshot.bannerSessionEntries * 224L,
            units = bannerSnapshot.authorNames.size +
                bannerSnapshot.publicationHistoryKeys.size +
                bannerSnapshot.editorSessions.size +
                bannerSnapshot.bannerSessionEntries
        )

        val headsBlock = MemoryBlock(
            title = "Головы",
            bytes = headsSnapshot.dynamicEntries.sumOf { entry ->
            estimateStringBytes(entry.stableKey) +
                estimateStringBytes(entry.name) +
                estimateStringBytes(entry.russianAlias) +
                estimateStringBytes(entry.textureValue) +
                96L
            } + headsSnapshot.searchEntries.sumOf { (key, entries) ->
                estimateStringBytes(key) + entries.size * 40L
            } + headsSnapshot.cachedRecentEntries * 120L +
                headsSnapshot.cachedRecentPlayers * 48L +
                headsSnapshot.sessionEntries * 84L,
            units = headsSnapshot.dynamicCount
        )

        val namedBlocks = listOf(
            flagBlock,
            headsBlock,
            MemoryBlock(
                title = "Эффекты",
                bytes = effectSnapshot.activeEffects * 72L + effectSnapshot.scheduledTasks * 36L + effectSnapshot.internalOwners * 48L,
                units = effectSnapshot.activeEffects + effectSnapshot.scheduledTasks + effectSnapshot.internalOwners
            ),
            MemoryBlock(
                title = "Скрытия",
                bytes = hideSnapshot.hiddenRelations * 40L + hideSnapshot.hiders * 32L + hideSnapshot.notificationCooldowns * 24L,
                units = hideSnapshot.hiddenRelations + hideSnapshot.hiders + hideSnapshot.notificationCooldowns
            ),
            MemoryBlock(
                title = "Маскировки",
                bytes = disguiseSnapshot.disguisedPlayers * 320L +
                    disguiseSnapshot.viewerRelations * 24L +
                    disguiseSnapshot.queuedViewerRelations * 24L +
                    disguiseSnapshot.pendingViewers * 24L +
                    disguiseSnapshot.rememberedNames * 96L +
                    disguiseSnapshot.rememberedGlowStates * 24L,
                units = disguiseSnapshot.disguisedPlayers +
                    disguiseSnapshot.viewerRelations +
                    disguiseSnapshot.queuedViewerRelations +
                    disguiseSnapshot.pendingViewers +
                    disguiseSnapshot.rememberedNames +
                    disguiseSnapshot.rememberedGlowStates
            ),
            MemoryBlock(
                title = "Заморозка",
                bytes = freezeSnapshot.sessions * 320L + freezeSnapshot.hiddenViewers * 48L + freezeSnapshot.hiddenRelations * 64L,
                units = freezeSnapshot.sessions + freezeSnapshot.hiddenViewers + freezeSnapshot.hiddenRelations
            ),
            MemoryBlock(
                title = "Банки",
                bytes = jarSnapshot.activeSessions * 480L +
                    jarSnapshot.releaseInProgress * 24L +
                    jarSnapshot.pendingReleaseCallbacks * 48L +
                    jarSnapshot.movementBypassTargets * 24L,
                units = jarSnapshot.activeSessions +
                    jarSnapshot.releaseInProgress +
                    jarSnapshot.pendingReleaseCallbacks +
                    jarSnapshot.movementBypassTargets
            ),
            MemoryBlock(
                title = "Кулдауны",
                bytes = commandCooldownEntries * 40L + commandCooldownPlayers * 24L,
                units = commandCooldownEntries
            ),
            MemoryBlock(
                title = "Лиц. профили",
                bytes = licensedProfiles.sumOf { payload ->
                    estimateStringBytes(payload.canonicalName) +
                        estimateStringBytes(payload.textureValue) +
                        estimateStringBytes(payload.textureSignature) + 96L
                },
                units = licensedProfiles.size
            ),
            MemoryBlock(
                title = "Редактор",
                bytes = itemEditSessions.sumOf { estimateSerializedItemBytes(it.editableItem) + 512L },
                units = itemEditSessions.size
            )
        ).filter { it.units > 0 || it.bytes > 0L }
            .sortedByDescending { it.bytes }

        val total = namedBlocks.sumOf { it.bytes }
        return MemoryReport(
            totalBytes = total,
            mainBlocks = namedBlocks,
            remainingBytes = max(0L, total - namedBlocks.sumOf { it.bytes })
        )
    }

    private data class MemoryReport(
        val totalBytes: Long,
        val mainBlocks: List<MemoryBlock>,
        val remainingBytes: Long
    )

    private fun estimateSerializedItemBytes(item: org.bukkit.inventory.ItemStack?): Long {
        if (item == null) return 0L
        return runCatching { item.serializeAsBytes().size.toLong() + 96L }.getOrDefault(96L)
    }

    private fun estimateStringBytes(value: String?): Long {
        if (value.isNullOrEmpty()) return 0L
        return 40L + value.length * 2L
    }

    private fun formatUnits(value: Int): String = String.format(Locale.US, "%,d", value)

    private fun formatBytes(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        return when {
            bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
