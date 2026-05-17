package com.ratger.acreative.commands.paint.moderation

import com.ratger.acreative.commands.paint.persistence.PaintUserStateRepository
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.menus.banner.service.BannerPlayerLookupService
import com.ratger.acreative.menus.common.MenuSoundSupport
import com.ratger.acreative.menus.edit.head.LicensedProfileLookupService
import com.ratger.acreative.menus.edit.meta.MiniMessageParser
import com.ratger.acreative.moderation.userban.UserBanEntry
import com.ratger.acreative.moderation.userban.UserBanMenuRenderer
import com.ratger.acreative.moderation.userban.UserBanService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.violence.coreapi.bukkit.api.menu.Menu
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PaintBanController(
    private val hooker: FunctionHooker,
    parser: MiniMessageParser,
    pageSize: Int
) {
    val repository = PaintUserStateRepository(hooker.database, pageSize)

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "acreative-paint-moderation").apply { isDaemon = true }
    }
    private val playerLookupService = BannerPlayerLookupService(LicensedProfileLookupService())
    private val userBanService = UserBanService(repository, playerLookupService)
    private val userBanMenuRenderer by lazy {
        UserBanMenuRenderer(
            plugin = hooker.plugin,
            parser = parser,
            sharedButtonFactory = hooker.menuService.buttonFactory(),
            title = "? ?????????? ??????"
        )
    }

    fun isBanned(playerId: UUID): Boolean = userBanService.isBanned(playerId)

    fun toggleUserBan(player: Player, targetName: String, reason: String?) {
        val targetUser = playerLookupService.findUser(targetName)
        if (targetUser == null) {
            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
            return
        }

        hooker.actionLogger.auditInfo {
            "Paint user-ban requested by ${hooker.actionLogger.playerRef(player)} target=${targetUser.name} reason=${reason?.takeIf { it.isNotBlank() } ?: "none"}"
        }

        executor.submit {
            userBanService.toggle(targetUser, reason)
                .whenComplete { result, error ->
                    runSync {
                        if (error != null || result == null) {
                            hooker.actionLogger.auditWarning {
                                "Paint user-ban failed by ${hooker.actionLogger.playerRef(player)} target=${targetUser.name} error=${error?.javaClass?.simpleName ?: "null"}"
                            }
                            if (!player.isOnline) return@runSync
                            hooker.messageManager.sendChat(player, MessageKey.ERROR_UNKNOWN_PLAYER)
                            return@runSync
                        }

                        when (result) {
                            UserBanService.ToggleResult.Unbanned -> {
                                hooker.actionLogger.auditInfo {
                                    "Paint user-ban completed by ${hooker.actionLogger.playerRef(player)} target=${targetUser.name} result=unbanned"
                                }
                                if (!player.isOnline) return@runSync
                                hooker.messageManager.sendChat(
                                    player,
                                    MessageKey.USER_UNBANNED,
                                    mapOf("player" to targetUser.name)
                                )
                            }

                            is UserBanService.ToggleResult.Banned -> {
                                hooker.actionLogger.auditInfo {
                                    "Paint user-ban completed by ${hooker.actionLogger.playerRef(player)} target=${result.entry.playerName} result=banned"
                                }
                                if (!player.isOnline) return@runSync
                                hooker.messageManager.sendChat(
                                    player,
                                    MessageKey.USER_BANNED,
                                    mapOf("player" to result.entry.playerName)
                                )
                            }
                        }
                    }
                }
        }
    }

    fun openBannedUsers(player: Player, requestedPage: Int = 1, currentMenu: Menu? = null) {
        executor.submit {
            val pageResult = userBanService.page(requestedPage)
            runSync {
                if (!player.isOnline) return@runSync
                userBanMenuRenderer.render(
                    player = player,
                    pageResult = pageResult,
                    onEntry = { entry -> unbanUserFromMenu(player, entry, pageResult.page, currentMenu) },
                    onBack = if (pageResult.page > 1) {
                        { openBannedUsers(player, pageResult.page - 1) }
                    } else {
                        null
                    },
                    onForward = if (pageResult.page < pageResult.totalPages) {
                        { openBannedUsers(player, pageResult.page + 1) }
                    } else {
                        null
                    },
                    currentMenu = currentMenu
                )
            }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private fun unbanUserFromMenu(player: Player, entry: UserBanEntry, currentPage: Int, currentMenu: Menu? = null) {
        executor.submit {
            val removed = userBanService.unban(entry.playerUuid)
            runSync {
                if (!player.isOnline) return@runSync
                if (removed) {
                    MenuSoundSupport.success(player)
                    hooker.messageManager.sendChat(
                        player,
                        MessageKey.USER_UNBANNED,
                        mapOf("player" to entry.playerName)
                    )
                }
                openBannedUsers(player, currentPage, currentMenu)
            }
        }
    }

    private fun runSync(action: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            action()
        } else {
            Bukkit.getScheduler().runTask(hooker.plugin, Runnable { action() })
        }
    }
}
