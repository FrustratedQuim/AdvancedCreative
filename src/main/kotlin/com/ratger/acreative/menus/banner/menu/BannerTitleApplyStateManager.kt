package com.ratger.acreative.menus.banner.menu

import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import com.ratger.acreative.menus.banner.model.BannerPostDraft
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class BannerTitleApplyStateManager(
    private val plugin: org.bukkit.plugin.Plugin,
    private val messageManager: MessageManager,
    private val promptService: ApplyPromptService,
    private val onApply: (Player, String?) -> Unit,
    private val onReopen: (Player) -> Unit
) : ApplyCommandTarget {
    private companion object {
        const val TIMEOUT_SECONDS: Int = 30
        const val TIMEOUT_TICKS: Long = 20L * TIMEOUT_SECONDS
    }

    private data class Request(val timeoutTaskId: Int)

    private val requests: MutableMap<UUID, Request> = mutableMapOf()

    fun begin(player: Player) {
        cancel(player)
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val onlinePlayer = Bukkit.getPlayer(player.uniqueId) ?: return@Runnable
            val request = requests.remove(onlinePlayer.uniqueId) ?: return@Runnable
            Bukkit.getScheduler().cancelTask(request.timeoutTaskId)
            promptService.clearPrompt(onlinePlayer)
            onReopen(onlinePlayer)
        }, TIMEOUT_TICKS)

        requests[player.uniqueId] = Request(task.taskId)
        promptService.showPrompt(player, EditorApplyKind.NAME_TEXT, TIMEOUT_SECONDS)
    }

    override fun isWaiting(player: Player): Boolean = requests.containsKey(player.uniqueId)

    override fun handle(player: Player, args: Array<out String>): Boolean {
        val request = requests[player.uniqueId] ?: return false
        if (args.isEmpty()) {
            messageManager.sendChat(player, MessageKey.EDIT_APPLY_USAGE_TEXT)
            return true
        }

        if (args[0].equals("cancel", ignoreCase = true)) {
            clear(player, request)
            onReopen(player)
            return true
        }

        val title = BannerPostDraft.normalizeTitle(args.joinToString(" "))
        clear(player, request)
        onApply(player, title)
        return true
    }

    override fun tabComplete(player: Player, args: Array<out String>): List<String> {
        if (!isWaiting(player)) return emptyList()
        return if (args.size == 1 && "cancel".startsWith(args[0], ignoreCase = true)) listOf("cancel") else emptyList()
    }

    override fun cancel(player: Player) {
        val request = requests.remove(player.uniqueId) ?: return
        Bukkit.getScheduler().cancelTask(request.timeoutTaskId)
        promptService.clearPrompt(player)
    }

    private fun clear(player: Player, request: Request) {
        requests.remove(player.uniqueId)
        Bukkit.getScheduler().cancelTask(request.timeoutTaskId)
        promptService.clearPrompt(player)
    }
}
