package com.ratger.acreative.commands.spit

import com.ratger.acreative.commands.common.MouthEmissionCalculator
import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Sound
import org.bukkit.entity.LlamaSpit
import org.bukkit.entity.Player

class SpitManager(hooker: FunctionHooker) {

    private val emissionCalculator = MouthEmissionCalculator(hooker)
    private val plugin = hooker.plugin
    private val utils = hooker.utils

    fun spitPlayer(player: Player) {
        val emission = emissionCalculator.calculate(player)

        val spit = player.world.spawn(emission.origin, LlamaSpit::class.java) { entity ->
            entity.velocity = emission.direction.multiply(1.5)
            entity.isPersistent = false
            if (utils.isGlowing(player)) entity.isGlowing = true
        }

        for (viewer in player.world.players) {
            if (viewer != player && utils.isHiddenFromPlayer(viewer, player)) {
                viewer.hideEntity(plugin, spit)
                continue
            }
            viewer.playSound(emission.origin, Sound.ENTITY_LLAMA_SPIT, 2.0f, 1.0f)
        }
    }
}
