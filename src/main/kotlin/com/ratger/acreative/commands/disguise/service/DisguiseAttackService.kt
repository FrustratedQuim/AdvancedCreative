package com.ratger.acreative.commands.disguise.service

import com.ratger.acreative.commands.disguise.DisguiseAttackContext
import com.ratger.acreative.commands.disguise.DisguiseAttackResetMode
import com.ratger.acreative.commands.disguise.DisguiseAttackState
import com.ratger.acreative.commands.disguise.DisguiseAttackStatePresentation
import com.ratger.acreative.commands.disguise.DisguisePacketDispatcher
import com.ratger.acreative.commands.disguise.model.DisguiseData
import com.ratger.acreative.commands.disguise.model.DisguiseRuntimeState
import com.ratger.acreative.core.FunctionHooker
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class DisguiseAttackService(
    private val hooker: FunctionHooker,
    private val state: DisguiseRuntimeState,
    private val packetDispatcher: DisguisePacketDispatcher,
    private val syncService: DisguiseSyncService
) {
    companion object {
        private const val ATTACK_ACTION_COOLDOWN_TICKS = 2L
        private const val SLIME_JUMP_LAND_TICKS = 2L
    }

    fun stopTracking(player: Player) {
        state.lastAttackAnimationTick.remove(player)
        state.attackStateResetTasks.remove(player)?.let(hooker.tickScheduler::cancel)
    }

    fun sendAttackAnimation(player: Player) {
        val data = state.disguisedPlayers[player] ?: return
        val currentTick = Bukkit.getCurrentTick().toLong()
        val lastTick = state.lastAttackAnimationTick[player]
        if (lastTick != null && currentTick - lastTick < ATTACK_ACTION_COOLDOWN_TICKS) return
        state.lastAttackAnimationTick[player] = currentTick

        if (data.type == EntityType.EVOKER_FANGS) {
            syncService.respawnEntityForViewers(
                player,
                data,
                syncService.resolveActiveViewers(player),
                scheduleMotionStabilization = false
            )
        }

        if (data.type == EntityType.SLIME || data.type == EntityType.MAGMA_CUBE) {
            triggerSlimeJumpAnimation(player, data)
            return
        }

        packetDispatcher.playAttackAnimation(data.entity, data.capabilities.attackAnimation)
        applyAttackState(player, data)
    }

    fun syncContinuousAttackState(player: Player, data: DisguiseData) {
        val attackState = data.capabilities.attackState
        if (!attackState.reappliesContinuouslyWhenActive) return
        if (!attackState.isActive(data.entity)) return

        val presentation = attackState.presentation(data.entity, active = true) ?: return
        sendAttackStatePresentation(syncService.resolveActiveViewers(player), data.entity, presentation)
    }

    private fun applyAttackState(player: Player, data: DisguiseData) {
        val attackState = data.capabilities.attackState
        if (attackState == DisguiseAttackState.None) return

        state.attackStateResetTasks.remove(player)?.let(hooker.tickScheduler::cancel)

        val context = DisguiseAttackContext(targetEntityId = null)
        if (attackState.isToggle) {
            val activate = !attackState.isActive(data.entity)
            val changed = if (activate) {
                attackState.apply(data.entity, context)
            } else {
                attackState.clear(data.entity)
            }
            if (!changed) return

            val presentation = attackState.presentation(data.entity, active = activate) ?: return
            sendAttackStatePresentation(syncService.resolveActiveViewers(player), data.entity, presentation)
            return
        }

        val wasAlreadyActive = attackState.isActive(data.entity)
        val changed = attackState.apply(data.entity, context)
        if (!changed && !wasAlreadyActive) {
            return
        }

        val activePresentation = attackState.presentation(data.entity, active = true)
        if (activePresentation != null) {
            sendAttackStatePresentation(syncService.resolveActiveViewers(player), data.entity, activePresentation)
        }

        if (attackState.durationTicks <= 0L || attackState.resetMode == DisguiseAttackResetMode.NONE) {
            return
        }

        val ownerId = player.uniqueId
        val entityId = data.entity.entityId
        val resetTaskId = hooker.tickScheduler.runLater(attackState.durationTicks) {
            val currentOwner = Bukkit.getPlayer(ownerId) ?: return@runLater
            val currentData = state.disguisedPlayers[currentOwner] ?: return@runLater
            if (currentData.entity.entityId != entityId) return@runLater

            state.attackStateResetTasks.remove(currentOwner)
            val wasCleared = attackState.clear(currentData.entity)
            if (!wasCleared || attackState.resetMode != DisguiseAttackResetMode.SERVER_AND_CLIENT) {
                return@runLater
            }

            val clearedPresentation = attackState.presentation(currentData.entity, active = false) ?: return@runLater
            sendAttackStatePresentation(syncService.resolveActiveViewers(currentOwner), currentData.entity, clearedPresentation)
        }
        state.attackStateResetTasks[player] = resetTaskId
    }

    private fun sendAttackStatePresentation(
        viewers: Collection<Player>,
        entity: WrapperEntity,
        presentation: DisguiseAttackStatePresentation
    ) {
        when (presentation) {
            is DisguiseAttackStatePresentation.EntityStatus -> {
                packetDispatcher.sendEntityStatus(viewers, entity.entityId, presentation.status)
            }

            is DisguiseAttackStatePresentation.Metadata -> {
                packetDispatcher.sendMetadata(viewers, entity.entityId, presentation.entries)
            }
        }
    }

    private fun triggerSlimeJumpAnimation(player: Player, data: DisguiseData) {
        val viewers = syncService.resolveActiveViewers(player)
        if (viewers.isEmpty()) return

        val airborneSnapshot = syncService.currentLocationSnapshot(player, data).copy(onGround = false)
        packetDispatcher.sendTeleport(viewers, data.entity.entityId, airborneSnapshot)

        state.attackStateResetTasks.remove(player)?.let(hooker.tickScheduler::cancel)
        val ownerId = player.uniqueId
        val entityId = data.entity.entityId
        val resetTaskId = hooker.tickScheduler.runLater(SLIME_JUMP_LAND_TICKS) {
            val currentOwner = Bukkit.getPlayer(ownerId) ?: return@runLater
            val currentData = state.disguisedPlayers[currentOwner] ?: return@runLater
            if (currentData.entity.entityId != entityId) return@runLater

            state.attackStateResetTasks.remove(currentOwner)
            val currentViewers = syncService.resolveActiveViewers(currentOwner)
            if (currentViewers.isEmpty()) return@runLater

            packetDispatcher.sendTeleport(
                currentViewers,
                currentData.entity.entityId,
                syncService.currentLocationSnapshot(currentOwner, currentData)
            )
        }
        state.attackStateResetTasks[player] = resetTaskId
    }
}
