package com.ratger.acreative.commands.freeze

import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal data class FreezeSession(
    val player: Player,
    val blocks: MutableList<WrapperEntity>,
    var taskId: Int? = null
)

internal class FreezeSessionRegistry {
    private val sessionsByPlayer = ConcurrentHashMap<Player, FreezeSession>()
    private val hiddenBlocksByViewer = ConcurrentHashMap<UUID, MutableMap<UUID, MutableList<WrapperEntity>>>()

    fun hasSession(player: Player): Boolean = sessionsByPlayer.containsKey(player)

    fun getSession(player: Player): FreezeSession? = sessionsByPlayer[player]

    fun saveSession(session: FreezeSession) {
        sessionsByPlayer[session.player] = session
    }

    fun removeSession(player: Player): FreezeSession? = sessionsByPlayer.remove(player)

    fun trackHiddenBlocks(viewerId: UUID, targetId: UUID, blocks: MutableList<WrapperEntity>) {
        val hiddenByTarget = hiddenBlocksByViewer.computeIfAbsent(viewerId) { ConcurrentHashMap() }
        hiddenByTarget[targetId] = blocks
    }

    fun getHiddenTargets(viewerId: UUID): MutableMap<UUID, MutableList<WrapperEntity>>? = hiddenBlocksByViewer[viewerId]

    fun removeHiddenTarget(viewerId: UUID, targetId: UUID) {
        val hiddenByTarget = hiddenBlocksByViewer[viewerId] ?: return
        hiddenByTarget.remove(targetId)
        if (hiddenByTarget.isEmpty()) {
            hiddenBlocksByViewer.remove(viewerId)
        }
    }

    fun hasAnyHiddenBlocks(targetId: UUID): Boolean = hiddenBlocksByViewer.any { it.value.containsKey(targetId) }

    fun frozenPlayersView(): Map<Player, MutableList<WrapperEntity>> = sessionsByPlayer.mapValues { it.value.blocks }

    fun sessionCount(): Int = sessionsByPlayer.size

    fun hiddenViewerCount(): Int = hiddenBlocksByViewer.size

    fun hiddenRelationsCount(): Int = hiddenBlocksByViewer.values.sumOf { it.size }
}
