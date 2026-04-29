package com.ratger.acreative.utils

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class PacketHandler(private val hooker: FunctionHooker) {
    private var listener: PacketListenerAbstract? = null
    private val pendingPaintAnimationTasks = mutableMapOf<UUID, Int>()
    private val suppressedNextPaintAnimations = mutableMapOf<UUID, Long>()

    fun register() {
        listener = object : PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            override fun onPacketReceive(event: PacketReceiveEvent) {
                val type = event.packetType
                if (
                    type != PacketType.Play.Client.ANIMATION &&
                    type != PacketType.Play.Client.INTERACT_ENTITY &&
                    type != PacketType.Play.Client.PLAYER_DIGGING
                ) {
                    return
                }

                val player = event.getPlayer() as? Player ?: return

                when (type) {
                    PacketType.Play.Client.ANIMATION -> handleAnimationPacket(player)
                    PacketType.Play.Client.INTERACT_ENTITY -> handleInteractPacket(event, player)
                    PacketType.Play.Client.PLAYER_DIGGING -> handleDiggingPacket(event, player)
                }
            }
        }
        PacketEvents.getAPI().eventManager.registerListener(listener!!)
    }

    private fun handleAnimationPacket(player: Player) {
        val paintManager = hooker.paintManagerOrNull()
        if (paintManager?.isPainting(player) == true) {
            if (consumeSuppressedPaintAnimation(player)) {
                return
            }
            pendingPaintAnimationTasks.remove(player.uniqueId)?.let { taskId ->
                hooker.tickScheduler.cancel(taskId)
            }
            var taskId = -1
            taskId = hooker.tickScheduler.runNow {
                pendingPaintAnimationTasks.remove(player.uniqueId)
                val handledByPaint = paintManager.handleSwing(player)
                if (handledByPaint) return@runNow
                val disguised = hooker.disguiseManager.disguisedPlayers.containsKey(player)
                if (!disguised) return@runNow
                hooker.disguiseManager.sendSwingAnimation(player)
            }
            pendingPaintAnimationTasks[player.uniqueId] = taskId
            return
        }
        if (paintManager?.handleSwing(player) == true) return
        if (!hooker.disguiseManager.disguisedPlayers.containsKey(player)) return
        hooker.tickScheduler.runNow {
            hooker.disguiseManager.sendSwingAnimation(player)
        }
    }

    private fun handleInteractPacket(event: PacketReceiveEvent, player: Player) {
        val packet = WrapperPlayClientInteractEntity(event)
        val paintManager = hooker.paintManagerOrNull()
        if (paintManager?.handleFrameInteraction(packet.entityId) == true) {
            event.isCancelled = true
            hooker.tickScheduler.runNow {
                paintManager.handleFrameUse(player, packet.entityId)
            }
            return
        }
        if (packet.action == WrapperPlayClientInteractEntity.InteractAction.ATTACK && paintManager?.isPainting(player) == true) {
            event.isCancelled = true
            hooker.tickScheduler.runNow {
                paintManager.handleLeftClick(player)
            }
            return
        }
        if (packet.action != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return

        val target = Bukkit.getOnlinePlayers().firstOrNull { it.entityId == packet.entityId } ?: return

        hooker.tickScheduler.runNow {
            // Stage 1 (dependent): grab attack has priority and can "consume" the chain.
            val handledByGrab = hooker.grabManagerOrNull()?.handleHolderAttack(player, target) == true
            if (handledByGrab) return@runNow

            // Stage 2 (dependent): jar attack follows grab and can "consume" the chain.
            val handledByJar = hooker.jarManagerOrNull()?.handleJarredAttack(player, target) == true
            if (handledByJar) return@runNow

            // Stage 3 (dependent): slap executes only if previous stages did not consume processing.
            if (hooker.utils.isSlapping(player)) {
                hooker.slapManager.applySlap(player, target)
            }
        }
    }

    private fun handleDiggingPacket(event: PacketReceiveEvent, player: Player) {
        val paintManager = hooker.paintManagerOrNull() ?: return
        if (!paintManager.isPainting(player)) {
            return
        }

        val packet = WrapperPlayClientPlayerDigging(event)
        when (packet.action) {
            DiggingAction.DROP_ITEM -> {
                event.isCancelled = true
                paintManager.suppressDirectUseAfterDrop(player)
                pendingPaintAnimationTasks.remove(player.uniqueId)?.let { taskId ->
                    hooker.tickScheduler.cancel(taskId)
                }
                suppressNextPaintAnimation(player)
                hooker.tickScheduler.runNow {
                    paintManager.handleDropAction(player, false)
                }
            }
            DiggingAction.DROP_ITEM_STACK -> {
                event.isCancelled = true
                paintManager.suppressDirectUseAfterDrop(player)
                pendingPaintAnimationTasks.remove(player.uniqueId)?.let { taskId ->
                    hooker.tickScheduler.cancel(taskId)
                }
                suppressNextPaintAnimation(player)
                hooker.tickScheduler.runNow {
                    paintManager.handleDropAction(player, true)
                }
                hooker.tickScheduler.runLater(1L) {
                    if (paintManager.isPainting(player) && player.isOnline) {
                        paintManager.resyncHeldToolSlot(player)
                    }
                }
            }
            else -> {}
        }
    }

    private fun suppressNextPaintAnimation(player: Player) {
        val untilMillis = System.currentTimeMillis() + SUPPRESSED_ANIMATION_WINDOW_MILLIS
        suppressedNextPaintAnimations[player.uniqueId] = untilMillis
    }

    private fun consumeSuppressedPaintAnimation(player: Player): Boolean {
        val untilMillis = suppressedNextPaintAnimations[player.uniqueId] ?: return false
        val now = System.currentTimeMillis()
        if (now > untilMillis) {
            suppressedNextPaintAnimations.remove(player.uniqueId)
            return false
        }
        suppressedNextPaintAnimations.remove(player.uniqueId)
        return true
    }

    fun unregister() {
        pendingPaintAnimationTasks.values.forEach(hooker.tickScheduler::cancel)
        pendingPaintAnimationTasks.clear()
        suppressedNextPaintAnimations.clear()
        listener?.let {
            PacketEvents.getAPI().eventManager.unregisterListener(it)
            listener = null
        }
    }

    companion object {
        private const val SUPPRESSED_ANIMATION_WINDOW_MILLIS = 250L
    }
}
