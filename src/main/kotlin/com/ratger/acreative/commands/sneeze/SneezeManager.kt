package com.ratger.acreative.commands.sneeze

import com.ratger.acreative.commands.common.MouthEmissionCalculator
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player

class SneezeManager(hooker: FunctionHooker) {

    private val emissionCalculator = MouthEmissionCalculator(hooker)
    private val utils = hooker.utils

    fun sneezePlayer(player: Player) {
        val emission = emissionCalculator.calculate(player)
        val nearbyPlayers = player.world.players.filter { it.isOnline }

        val particleCount = (5 * emission.scale).toInt().coerceAtLeast(1)
        val particleSpread = 0.1 * emission.scale
        val particleSpeed = 0.05 * emission.scale

        for (viewer in nearbyPlayers) {
            if (viewer != player && utils.isHiddenFromPlayer(viewer, player)) {
                continue
            }

            viewer.playSound(player.location, Sound.ENTITY_PANDA_SNEEZE, 1f, 1f)
            viewer.spawnParticle(
                Particle.SNEEZE,
                emission.origin,
                particleCount,
                particleSpread,
                particleSpread,
                particleSpread,
                particleSpeed
            )
        }
    }
}
