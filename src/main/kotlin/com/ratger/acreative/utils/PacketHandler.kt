package com.ratger.acreative.utils

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PacketHandler(private val hooker: FunctionHooker) {

    fun register() {
        val listener = object : PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            override fun onPacketReceive(event: PacketReceiveEvent) {
                val type = event.packetType
                if (type != PacketType.Play.Client.ANIMATION && type != PacketType.Play.Client.INTERACT_ENTITY) return

                val player = event.getPlayer() as? Player ?: return

                when (type) {
                    PacketType.Play.Client.ANIMATION -> {
                        if (!hooker.disguiseManager.disguisedPlayers.containsKey(player)) return
                        Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                            hooker.disguiseManager.sendSwingAnimation(player)
                        })
                    }
                    PacketType.Play.Client.INTERACT_ENTITY -> {
                        if (!hooker.utils.isSlapping(player)) return
                        val packet = WrapperPlayClientInteractEntity(event)

                        if (packet.action != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return
                        val target = Bukkit.getOnlinePlayers().firstOrNull { it.entityId == packet.entityId } ?: return

                        Bukkit.getScheduler().runTask(hooker.plugin, Runnable {
                            hooker.slapManager.applySlap(player, target)
                        })
                    }
                }
            }
        }
        PacketEvents.getAPI().eventManager.registerListener(listener)
    }

    fun unregister() {
        PacketEvents.getAPI().eventManager.unregisterAllListeners()
    }
}