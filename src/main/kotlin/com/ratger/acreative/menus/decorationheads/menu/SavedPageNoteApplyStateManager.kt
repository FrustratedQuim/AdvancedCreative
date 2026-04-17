package com.ratger.acreative.menus.decorationheads.menu

import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.core.MessageManager
import com.ratger.acreative.menus.apply.ApplyCommandTarget
import com.ratger.acreative.menus.edit.apply.core.ApplyPromptService
import com.ratger.acreative.menus.edit.apply.core.EditorApplyKind
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class SavedPageNoteApplyStateManager(
    private val plugin: org.bukkit.plugin.Plugin,
    private val messageManager: MessageManager,
    private val promptService: ApplyPromptService,
    private val onApply: (player: Player, pageId: Long, note: String?) -> Unit,
    private val onReopen: (player: Player, pageId: Long) -> Unit
) : ApplyCommandTarget {
    private companion object {
        const val TIMEOUT_SECONDS: Int = 30
        const val TIMEOUT_TICKS: Long = 20L * TIMEOUT_SECONDS
    }

    private data class Request(val pageId: Long, val timeoutTaskId: Int)

    private val requests: MutableMap<UUID, Request> = mutableMapOf()

    fun begin(player: Player, pageId: Long) {
        cancel(player)
        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val online = Bukkit.getPlayer(player.uniqueId) ?: return@Runnable
            val request = requests.remove(online.uniqueId) ?: return@Runnable
            Bukkit.getScheduler().cancelTask(request.timeoutTaskId)
            promptService.clearPrompt(online)
            onReopen(online, request.pageId)
        }, TIMEOUT_TICKS)
        requests[player.uniqueId] = Request(pageId, task.taskId)
        promptService.showPrompt(player, EditorApplyKind.LORE_TEXT, TIMEOUT_SECONDS)
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
            onReopen(player, request.pageId)
            return true
        }
        val note = args.joinToString(" ").trim().takeIf { it.isNotBlank() }
        clear(player, request)
        onApply(player, request.pageId, note)
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
