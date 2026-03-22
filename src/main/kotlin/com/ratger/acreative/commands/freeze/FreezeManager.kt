package com.ratger.acreative.commands.freeze

import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageKey
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FreezeManager(private val hooker: FunctionHooker) {

    private val targetResolver = FreezeTargetResolver(hooker)
    private val blockFactory = FreezeBlockFactory(hooker)
    private val sessions = FreezeSessionRegistry()

    val frozenPlayers: Map<Player, MutableList<WrapperEntity>>
        get() = sessions.frozenPlayersView()

    fun prepareToFreezePlayer(initiator: Player, targetName: String?) {
        val resolved = targetResolver.resolve(initiator, targetName) ?: return
        hooker.playerStateManager.activateState(resolved.target, PlayerStateType.FROZEN)
        freezePlayer(resolved.target, resolved.initiator)
    }

    fun freezePlayer(player: Player, initiator: Player? = null) {
        if (player.gameMode == GameMode.SPECTATOR) {
            player.gameMode = GameMode.CREATIVE
        }

        if (sessions.hasSession(player)) {
            unfreezePlayer(player)
            return
        }

        val blocks = blockFactory.createFor(player)
        if (hooker.utils.isGlowing(player)) blocks.forEach { it.entityMeta.isGlowing = true }

        hideBlocksFromHiddenViewers(player, blocks)
        val taskId = scheduleFreezeTicks(player)
        sessions.saveSession(FreezeSession(player, blocks, taskId))
        notifyFreezeResult(player, initiator)
    }

    fun unfreezePlayer(player: Player) {
        val session = sessions.removeSession(player)
        session?.blocks?.forEach {
            it.entityMeta.isGlowing = false
            it.remove()
        }
        player.freezeTicks = 0

        for (viewer in Bukkit.getOnlinePlayers()) {
            val hiddenForViewer = sessions.getHiddenTargets(viewer.uniqueId) ?: continue
            if (!hiddenForViewer.containsKey(player.uniqueId)) continue

            if (!hooker.utils.isHiddenFromPlayer(viewer, player)) {
                hiddenForViewer[player.uniqueId]?.forEach { block ->
                    if (block.isSpawned) block.addViewer(viewer.uniqueId)
                }
            }
            sessions.removeHiddenTarget(viewer.uniqueId, player.uniqueId)
        }

        session?.taskId?.let(hooker.tickScheduler::cancel)
        hooker.playerStateManager.deactivateState(player, PlayerStateType.FROZEN)
    }

    fun hideFreezeBlocksForViewer(viewer: Player, target: Player) {
        val blocks = sessions.getSession(target)?.blocks ?: return
        blocks.forEach { it.removeViewer(viewer.uniqueId) }
        sessions.trackHiddenBlocks(viewer.uniqueId, target.uniqueId, blocks)
    }

    fun showFreezeBlocksForViewer(viewer: Player, target: Player) {
        val blocks = sessions.getSession(target)?.blocks ?: return
        blocks.forEach { block ->
            if (block.isSpawned) block.addViewer(viewer.uniqueId)
        }
        sessions.removeHiddenTarget(viewer.uniqueId, target.uniqueId)
    }

    fun updateIceGlowing(player: Player, isGlowing: Boolean) {
        sessions.getSession(player)?.blocks?.forEach { block ->
            block.entityMeta.isGlowing = isGlowing && !sessions.hasAnyHiddenBlocks(player.uniqueId)
        }
    }

    private fun hideBlocksFromHiddenViewers(player: Player, blocks: MutableList<WrapperEntity>) {
        for (viewer in Bukkit.getOnlinePlayers()) {
            if (viewer == player || !hooker.utils.isHiddenFromPlayer(viewer, player)) continue
            sessions.trackHiddenBlocks(viewer.uniqueId, player.uniqueId, blocks)
            blocks.forEach { it.removeViewer(viewer.uniqueId) }
        }
    }

    private fun scheduleFreezeTicks(player: Player): Int = hooker.tickScheduler.runRepeating(0L, 20L) {
        if (!sessions.hasSession(player) || !player.isOnline) {
            unfreezePlayer(player)
            return@runRepeating
        }
        player.freezeTicks = player.maxFreezeTicks * 2
    }

    private fun notifyFreezeResult(player: Player, initiator: Player?) {
        if (initiator == null || initiator == player) {
            hooker.messageManager.sendChat(player, MessageKey.SUCCESS_FREEZE_SELF)
            return
        }

        hooker.messageManager.sendChat(
            initiator,
            MessageKey.SUCCESS_FREEZE,
            variables = mapOf("target" to player.name)
        )
    }
}
