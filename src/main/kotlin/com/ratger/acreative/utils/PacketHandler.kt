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

class PacketHandler(private val hooker: FunctionHooker) {
    private var listener: PacketListenerAbstract? = null

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
        if (hooker.paintManagerOrNull()?.handleSwing(player) == true) return
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
                hooker.tickScheduler.runNow {
                    paintManager.handleDropAction(player, false)
                }
            }
            DiggingAction.DROP_ITEM_STACK -> {
                event.isCancelled = true
                hooker.tickScheduler.runNow {
                    paintManager.handleDropAction(player, true)
                }
            }
            else -> {}
        }
    }

    fun unregister() {
        listener?.let {
            PacketEvents.getAPI().eventManager.unregisterListener(it)
            listener = null
        }
    }
}
