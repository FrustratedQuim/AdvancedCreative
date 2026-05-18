package com.ratger.acreative.commands.disguise.model

import com.github.retrooper.packetevents.util.Vector3d
import com.ratger.acreative.commands.disguise.DisguisePacketDispatcher
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.github.retrooper.packetevents.protocol.item.ItemStack as PacketItemStack

class DisguiseRuntimeState {
    val disguisedPlayers = ConcurrentHashMap<Player, DisguiseData>()
    val tasks = ConcurrentHashMap<Player, Int>()
    val activeViewers = ConcurrentHashMap<Player, MutableSet<UUID>>()
    val queuedInitViewers = ConcurrentHashMap<Player, MutableSet<UUID>>()
    val viewerPendingUntilTick = ConcurrentHashMap<UUID, Long>()
    val lastCustomName = ConcurrentHashMap<Player, Component>()
    val lastSharedFlags = ConcurrentHashMap<Player, DisguisePacketDispatcher.SharedFlagsState>()
    val lastVelocityState = ConcurrentHashMap<Player, Vector3d>()
    val lastLocationState = ConcurrentHashMap<Player, DisguisePacketDispatcher.LocationSnapshot>()
    val lastPrimaryItemState = ConcurrentHashMap<Player, PacketItemStack>()
    val lastMirroredBlockStateId = ConcurrentHashMap<Player, Int>()
    val lastAttackAnimationTick = ConcurrentHashMap<Player, Long>()
    val attackStateResetTasks = ConcurrentHashMap<Player, Int>()
}
