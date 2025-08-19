package com.ratger.acreative.commands.glide

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.entity.Player

class GlideManager(private val hooker: FunctionHooker) {

    private data class PlayerFlightState(
        val allowFlight: Boolean,
        val isFlying: Boolean
    )

    val glidingPlayers = mutableSetOf<Player>()
    private val flightStates = mutableMapOf<Player, PlayerFlightState>()

    fun canGlide(player: Player): Boolean {
        return player.gameMode != org.bukkit.GameMode.SPECTATOR
    }

    fun glidePlayer(player: Player) {
        if (!hooker.utils.checkAndRemovePose(player)) {
            return
        }
        if (!canGlide(player)) {
            return
        }
        if (glidingPlayers.contains(player)) return
        glidingPlayers.add(player)
        flightStates[player] = PlayerFlightState(player.allowFlight, player.isFlying)
        player.isGliding = true
        player.allowFlight = false
        player.isFlying = false
        hooker.messageManager.sendMiniMessage(player, key = "info-glide-on")
        hooker.messageManager.sendMiniMessage(player, "ACTION", "action-pose-unset", repeatable = true)
    }

    fun unglidePlayer(player: Player) {
        if (!glidingPlayers.contains(player)) return
        glidingPlayers.remove(player)
        flightStates.remove(player)?.let { state ->
            player.allowFlight = state.allowFlight
            player.isFlying = state.isFlying
        }
        player.isGliding = false
        hooker.messageManager.sendMiniMessage(player, key = "info-glide-off")
        hooker.messageManager.sendMiniMessage(player, "ACTION_STOP")
        hooker.playerStateManager.refreshPlayerPose(player)
    }
}