package com.ratger.acreative.commands.spit

import com.ratger.acreative.core.FunctionHooker
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LlamaSpit
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class SpitManager(private val hooker: FunctionHooker) {

    private fun scaleValue(base: Double, player: Player): Double {
        val scale = player.getAttribute(Attribute.GENERIC_SCALE)?.value ?: 1.0
        return base * scale
    }

    fun spitPlayer(player: Player): Boolean {
        val isLaying = hooker.utils.isLaying(player)
        val pitch = if (isLaying) -90f else player.location.pitch.coerceIn(-90f, 90f)

        val yOffsetBase = if (isLaying) 1.2f else 1.9f - ((pitch + 90f) / 180f) * (1.9f - 1.2f)
        val yOffset = scaleValue(yOffsetBase.toDouble(), player).toFloat()

        val forwardOffsetBase = if (isLaying) 0f else if (pitch <= 0) {
            -0.1f + ((pitch + 90f) / 90f) * (0.6f + 0.1f)
        } else {
            0.6f - (pitch / 90f) * (0.6f - 0.2f)
        }
        val forwardOffset = scaleValue(forwardOffsetBase.toDouble(), player).toFloat()

        val direction = if (isLaying) Vector(0.0, 1.0, 0.0) else player.location.direction.clone()
        val horizontalDirection = if (isLaying) Vector(0.0, 0.0, 0.0) else Vector(direction.x, 0.0, direction.z).normalize().multiply(forwardOffset)

        val spitLocation = player.location.clone()
            .add(horizontalDirection)
            .add(0.0, yOffset.toDouble(), 0.0)

        val spit = player.world.spawn(spitLocation, LlamaSpit::class.java) { entity ->
            entity.velocity = direction.multiply(1.5)
            if (hooker.utils.isGlowing(player)) entity.isGlowing = true
        }

        val nearbyPlayers = player.world.players

        for (viewer in nearbyPlayers) {
            if (viewer != player && hooker.utils.isHiddenFromPlayer(viewer, player)) {
                viewer.hideEntity(hooker.plugin, spit)
                continue
            }
            viewer.playSound(spitLocation, Sound.ENTITY_LLAMA_SPIT, 2.0f, 1.0f)
        }

        return true
    }
}