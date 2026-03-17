package com.ratger.acreative.commands.glide

import com.ratger.acreative.commands.ExecutableCommand
import com.ratger.acreative.commands.PluginCommandType
import com.ratger.acreative.core.FunctionHooker
import com.ratger.acreative.core.MessageChannel
import com.ratger.acreative.core.MessageKey
import org.bukkit.entity.Player
import com.ratger.acreative.utils.PlayerStateManager.PlayerStateType
import org.bukkit.command.CommandSender

class GlideManager(private val hooker: FunctionHooker) {

    companion object {
        private val BOOST_OPTIONS = listOf("0", "0.1", "0.3", "0.5", "0.7", "1.0")
    }

    private data class PlayerFlightState(
        val allowFlight: Boolean,
        val isFlying: Boolean
    )

    val glidingPlayers = mutableSetOf<Player>()
    private val flightStates = mutableMapOf<Player, PlayerFlightState>()
    private val glideBoostByPlayer = mutableMapOf<Player, Double>()
    private var boostTaskId: Int? = null

    fun parseBoost(arg: String?): Double {
        return arg?.toDoubleOrNull()?.takeIf { it in 0.0..1.0 } ?: 0.0
    }

    fun tabCompletions(args: Array<out String>): List<String> {
        return if (args.size == 1) {
            BOOST_OPTIONS.filter { it.startsWith(args[0], ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    fun canGlide(player: Player): Boolean {
        return player.gameMode != org.bukkit.GameMode.SPECTATOR
    }

    fun glidePlayer(player: Player, boost: Double = 0.0) {
        val currentBoost = glideBoostByPlayer[player] ?: 0.0
        if (glidingPlayers.contains(player)) {
            if (currentBoost == boost) {
                unglidePlayer(player)
            } else {
                glideBoostByPlayer[player] = boost
            }
            return
        }

        hooker.playerStateManager.activateState(player, PlayerStateType.GLIDING)
        if (!canGlide(player)) {
            return
        }

        glidingPlayers.add(player)
        glideBoostByPlayer[player] = boost
        ensureBoostTaskRunning()
        flightStates[player] = PlayerFlightState(player.allowFlight, player.isFlying)
        player.isGliding = true
        player.allowFlight = false
        player.isFlying = false
        hooker.messageManager.sendChat(player, MessageKey.INFO_GLIDE_ON)
        hooker.messageManager.startRepeatingActionBar(player, MessageKey.ACTION_POSE_UNSET)
    }

    fun unglidePlayer(player: Player) {
        if (!glidingPlayers.contains(player)) {
            hooker.playerStateManager.deactivateState(player, PlayerStateType.GLIDING)
            return
        }
        glidingPlayers.remove(player)
        glideBoostByPlayer.remove(player)
        cleanupBoostTaskIfNeeded()
        flightStates.remove(player)?.let { state ->
            player.allowFlight = state.allowFlight
            player.isFlying = state.isFlying
        }
        player.isGliding = false
        hooker.messageManager.sendChat(player, MessageKey.INFO_GLIDE_OFF)
        hooker.messageManager.stopRepeating(player, MessageChannel.ACTION_BAR)
        hooker.playerStateManager.deactivateState(player, PlayerStateType.GLIDING)
        hooker.playerStateManager.refreshPlayerPose(player)
    }

    private fun ensureBoostTaskRunning() {
        if (boostTaskId != null) return
        boostTaskId = hooker.tickScheduler.runRepeating(0L, 2L) {
            val iterator = glidingPlayers.iterator()
            while (iterator.hasNext()) {
                val player = iterator.next()
                if (!player.isOnline) {
                    iterator.remove()
                    glideBoostByPlayer.remove(player)
                    flightStates.remove(player)
                    continue
                }

                val boost = glideBoostByPlayer[player] ?: 0.0
                if (boost <= 0.0) continue

                val lookDirection = player.location.direction
                player.velocity = player.velocity.add(lookDirection.multiply(boost))
            }
            cleanupBoostTaskIfNeeded()
        }
    }

    private fun cleanupBoostTaskIfNeeded() {
        if (glidingPlayers.isNotEmpty()) return
        boostTaskId?.let { hooker.tickScheduler.cancel(it) }
        boostTaskId = null
    }
}

class GlideCommand(hooker: FunctionHooker) : ExecutableCommand(hooker, PluginCommandType.GLIDE) {
    override fun handle(player: Player, args: Array<out String>) {
        val boost = hooker.glideManager.parseBoost(args.firstOrNull())
        hooker.glideManager.glidePlayer(player, boost)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return hooker.glideManager.tabCompletions(args)
    }
}
