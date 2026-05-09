package com.ratger.acreative.commands.spit

import com.ratger.acreative.commands.common.MouthEmissionCalculator
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Sound
import org.bukkit.entity.LlamaSpit
import org.bukkit.entity.Player

class SpitManager(private val hooker: FunctionHooker) {

    private val emissionCalculator = MouthEmissionCalculator(hooker)
    private val plugin = hooker.plugin
    private val utils = hooker.utils

    fun spitPlayer(player: Player) {
        val emission = emissionCalculator.calculate(player)
        hooker.actionLogger.info(
            "Spit requested for ${hooker.actionLogger.playerRef(player)} origin=${hooker.actionLogger.locationRef(emission.origin)} direction=${emission.direction}"
        )

        val spit = player.world.spawn(emission.origin, LlamaSpit::class.java) { entity ->
            entity.velocity = emission.direction.multiply(1.5)
            entity.isPersistent = false
            if (utils.isGlowing(player)) entity.isGlowing = true
        }

        var hiddenViewers = 0
        var audibleViewers = 0
        for (viewer in player.world.players) {
            if (viewer != player && utils.isHiddenFromPlayer(viewer, player)) {
                viewer.hideEntity(plugin, spit)
                hiddenViewers++
                continue
            }
            viewer.playSound(emission.origin, Sound.ENTITY_LLAMA_SPIT, 2.0f, 1.0f)
            audibleViewers++
        }
        hooker.actionLogger.info(
            "Spit entity spawned for ${hooker.actionLogger.playerRef(player)} spit=${spit.uniqueId} hiddenViewers=$hiddenViewers audibleViewers=$audibleViewers"
        )
    }
}
